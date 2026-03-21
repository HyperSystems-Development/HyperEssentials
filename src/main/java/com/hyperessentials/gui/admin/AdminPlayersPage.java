package com.hyperessentials.gui.admin;

import com.hyperessentials.data.PlayerData;
import com.hyperessentials.gui.GuiColors;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.HyperEssentials;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.moderation.ModerationManager;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.HEMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin players page — list online players with status info.
 * Supports a detail view mode for individual player inspection and quick actions.
 * Includes search, sort, filter, and pagination controls.
 */
public class AdminPlayersPage extends InteractiveCustomUIPage<AdminPageData> {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneId.systemDefault());

  private static final int ITEMS_PER_PAGE = 8;

  /** Sort modes for the player list. */
  private enum SortMode {
    NAME, JOIN_DATE, PLAYTIME;

    static SortMode fromString(@Nullable String s) {
      if (s == null) return NAME;
      return switch (s.toLowerCase()) {
        case "join_date" -> JOIN_DATE;
        case "playtime" -> PLAYTIME;
        default -> NAME;
      };
    }

    String toValue() {
      return switch (this) {
        case NAME -> "name";
        case JOIN_DATE -> "join_date";
        case PLAYTIME -> "playtime";
      };
    }
  }

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;

  // Search, sort, filter, pagination state
  private int currentPage = 0;
  private String searchQuery = "";
  private SortMode sortMode = SortMode.NAME;
  private boolean filterOnline = false;

  /** UUID of the player being viewed in detail mode, null for list mode. */
  @Nullable
  private UUID viewingPlayer;

  public AdminPlayersPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_PLAYERS);
    NavBarHelper.setupAdminBar(playerRef, "players", guiManager.getAdminRegistry(), cmd, events);
    buildContent(cmd, events);
  }

  private void buildContent(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    if (viewingPlayer != null) {
      buildDetailView(cmd, events);
    } else {
      buildPlayerList(cmd, events);
    }
  }

  // =====================================================================
  // List Mode
  // =====================================================================

  private void buildPlayerList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    Map<UUID, PlayerRef> tracked = plugin != null ? plugin.getTrackedPlayers() : Map.of();

    cmd.set("#PlayerCount.Text", tracked.size() + " player" + (tracked.size() != 1 ? "s" : "") + " online");

    // Show controls and pagination in list mode
    cmd.set("#ControlsRow.Visible", true);
    cmd.set("#PaginationRow.Visible", true);

    // --- Search controls ---
    cmd.set("#SearchInput.PlaceholderText", "Search players...");
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged, "#SearchInput",
        EventData.of("Button", "SearchChanged")
            .append("@SearchInput", "#SearchInput.Value"),
        false
    );

    // --- Sort dropdown ---
    cmd.set("#SortDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString("Name"), "name"),
        new DropdownEntryInfo(LocalizableString.fromString("Joined"), "join_date"),
        new DropdownEntryInfo(LocalizableString.fromString("Playtime"), "playtime")
    ));
    cmd.set("#SortDropdown.Value", sortMode.toValue());
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SortDropdown",
        EventData.of("Button", "SortChanged")
            .append("@SortMode", "#SortDropdown.Value")
            .append("@SearchInput", "#SearchInput.Value"),
        false);

    // --- Filter dropdown ---
    cmd.set("#FilterDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString("All"), "all"),
        new DropdownEntryInfo(LocalizableString.fromString("Online"), "online")
    ));
    cmd.set("#FilterDropdown.Value", filterOnline ? "online" : "all");
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#FilterDropdown",
        EventData.of("Button", "FilterChanged")
            .append("@FilterValue", "#FilterDropdown.Value")
            .append("@SearchInput", "#SearchInput.Value"),
        false);

    // --- Build filtered/sorted player list ---
    List<PlayerEntry> entries = new ArrayList<>();
    for (PlayerRef p : tracked.values()) {
      PlayerData data = loadPlayerData(p.getUuid());
      entries.add(new PlayerEntry(p.getUuid(), p.getUsername(), true, data));
    }

    // Apply search filter
    if (!searchQuery.isEmpty()) {
      String query = searchQuery.toLowerCase();
      entries.removeIf(e -> !e.username.toLowerCase().contains(query));
    }

    // Apply online filter (filterOnline=true means only online; all entries are online from tracked)
    // filterOnline=false shows all (which is currently just online players from tracked map)

    // Sort
    switch (sortMode) {
      case NAME -> entries.sort(Comparator.comparing(e -> e.username, String.CASE_INSENSITIVE_ORDER));
      case JOIN_DATE -> entries.sort(Comparator.comparing(
          (PlayerEntry e) -> e.data != null ? e.data.getFirstJoin() : Instant.EPOCH).reversed());
      case PLAYTIME -> entries.sort(Comparator.comparingLong(
          (PlayerEntry e) -> e.data != null ? e.data.getTotalPlaytimeMs() : 0L).reversed());
    }

    // Pagination
    int totalItems = entries.size();
    int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
    if (currentPage >= totalPages) currentPage = totalPages - 1;
    if (currentPage < 0) currentPage = 0;

    int start = currentPage * ITEMS_PER_PAGE;
    int end = Math.min(start + ITEMS_PER_PAGE, totalItems);
    List<PlayerEntry> pageEntries = entries.subList(start, end);

    // --- Render player list ---
    cmd.clear("#PlayerList");
    cmd.appendInline("#PlayerList", "Group #IndexCards { LayoutMode: Top; }");

    if (pageEntries.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", HEMessages.get(playerRef, AdminKeys.Players.EMPTY_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text",
          searchQuery.isEmpty()
              ? HEMessages.get(playerRef, AdminKeys.Players.EMPTY_MESSAGE)
              : "No players match your search.");
      return;
    }

    int i = 0;
    for (PlayerEntry entry : pageEntries) {
      cmd.append("#IndexCards", UIPaths.ADMIN_PLAYER_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #PlayerName.Text", entry.username);
      cmd.set(idx + " #PlayerInfo.Text", HEMessages.get(playerRef, AdminKeys.Players.PLAYER_INFO, entry.uuid.toString()));

      // Wire View button — preserve search state
      events.addEventBinding(
          CustomUIEventBindingType.Activating, idx + " #ViewBtn",
          EventData.of("Button", "View").append("Target", entry.uuid.toString())
              .append("@SearchInput", "#SearchInput.Value"),
          false
      );

      i++;
    }

    // --- Pagination controls ---
    cmd.set("#PageInfo.Text", "Page " + (currentPage + 1) + " / " + totalPages);

    cmd.set("#PrevBtn.Disabled", currentPage <= 0);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#PrevBtn",
        EventData.of("Button", "PrevPage").append("Page", String.valueOf(currentPage - 1))
            .append("@SearchInput", "#SearchInput.Value"),
        false);

    cmd.set("#NextBtn.Disabled", currentPage >= totalPages - 1);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#NextBtn",
        EventData.of("Button", "NextPage").append("Page", String.valueOf(currentPage + 1))
            .append("@SearchInput", "#SearchInput.Value"),
        false);
  }

  /** Lightweight record for sorting/filtering player list entries. */
  private record PlayerEntry(UUID uuid, String username, boolean online, @Nullable PlayerData data) {}

  // =====================================================================
  // Detail Mode
  // =====================================================================

  private void buildDetailView(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    if (viewingPlayer == null) return;

    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) {
      viewingPlayer = null;
      buildPlayerList(cmd, events);
      return;
    }

    // Hide controls and pagination in detail mode
    cmd.set("#ControlsRow.Visible", false);
    cmd.set("#PaginationRow.Visible", false);

    // Look up the target player
    PlayerRef targetRef = plugin.getTrackedPlayer(viewingPlayer);
    String username = targetRef != null ? targetRef.getUsername() : getStoredUsername(viewingPlayer);
    if (username == null) username = viewingPlayer.toString().substring(0, 8);
    boolean isOnline = targetRef != null;

    // Load PlayerData for stats
    PlayerData data = loadPlayerData(viewingPlayer);

    cmd.set("#PlayerCount.Text", username);

    cmd.clear("#PlayerList");
    cmd.appendInline("#PlayerList", "Group #IndexCards { LayoutMode: Top; }");

    // Back button row
    cmd.append("#IndexCards", UIPaths.ADMIN_PLAYER_DETAIL_HEADER);

    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#IndexCards[0] #BackBtn",
        EventData.of("Button", "Back"), false
    );

    // Detail content
    cmd.append("#IndexCards", UIPaths.ADMIN_PLAYER_DETAIL);
    String detail = "#IndexCards[1]";

    cmd.set(detail + " #DetailName.Text", username);

    // Stats rows
    addStatRow(cmd, detail + " #DetailStats", HEMessages.get(playerRef, AdminKeys.Players.DETAIL_UUID), viewingPlayer.toString());

    // Status
    String statusText = HEMessages.get(playerRef, AdminKeys.Players.DETAIL_STATUS_ONLINE);
    String statusColor = GuiColors.STATUS_ONLINE;
    if (data != null && data.getActiveBan() != null) {
      statusText = HEMessages.get(playerRef, AdminKeys.Players.DETAIL_STATUS_BANNED);
      statusColor = GuiColors.DANGER;
    } else if (data != null && data.getActiveMute() != null) {
      statusText = HEMessages.get(playerRef, AdminKeys.Players.DETAIL_STATUS_MUTED);
      statusColor = GuiColors.WARNING;
    } else if (!isOnline) {
      statusText = "Offline";
      statusColor = GuiColors.STATUS_OFFLINE;
    }
    addStatRow(cmd, detail + " #DetailStats", HEMessages.get(playerRef, AdminKeys.Players.DETAIL_STATUS), statusText, statusColor);

    // First join
    if (data != null) {
      addStatRow(cmd, detail + " #DetailStats",
          HEMessages.get(playerRef, AdminKeys.Players.DETAIL_FIRST_JOIN),
          DATE_FORMAT.format(data.getFirstJoin()));

      // Last seen
      addStatRow(cmd, detail + " #DetailStats",
          HEMessages.get(playerRef, AdminKeys.Players.DETAIL_LAST_SEEN),
          DATE_FORMAT.format(data.getLastJoin()));

      // Playtime
      addStatRow(cmd, detail + " #DetailStats",
          HEMessages.get(playerRef, AdminKeys.Players.DETAIL_PLAYTIME),
          UIHelper.formatPlaytime(data.getTotalPlaytimeMs()));

      // Punishment count
      long activeCount = data.getPunishments().stream().filter(Punishment::isEffective).count();
      addStatRow(cmd, detail + " #DetailStats",
          HEMessages.get(playerRef, AdminKeys.Players.DETAIL_PUNISHMENTS),
          activeCount + " active / " + data.getPunishments().size() + " total");
    }

    // Wire action buttons (only for online players)
    if (isOnline) {
      events.addEventBinding(
          CustomUIEventBindingType.Activating, detail + " #KickBtn",
          EventData.of("Button", "Kick").append("Target", viewingPlayer.toString()),
          false
      );
      events.addEventBinding(
          CustomUIEventBindingType.Activating, detail + " #MuteBtn",
          EventData.of("Button", "Mute").append("Target", viewingPlayer.toString()),
          false
      );
      events.addEventBinding(
          CustomUIEventBindingType.Activating, detail + " #BanBtn",
          EventData.of("Button", "Ban").append("Target", viewingPlayer.toString()),
          false
      );
    }
  }

  private void addStatRow(@NotNull UICommandBuilder cmd, @NotNull String container,
                           @NotNull String label, @NotNull String value) {
    addStatRow(cmd, container, label, value, null);
  }

  private void addStatRow(@NotNull UICommandBuilder cmd, @NotNull String container,
                           @NotNull String label, @NotNull String value, @Nullable String valueColor) {
    cmd.append(container, UIPaths.STAT_ROW);
    // Count existing stat rows to index properly
    int idx = statRowCount++;
    String row = container + "[" + idx + "]";
    cmd.set(row + " #StatLabel.Text", label);
    cmd.set(row + " #StatValue.Text", value);
    if (valueColor != null) {
      cmd.set(row + " #StatValue.Style.TextColor", valueColor);
    }
  }

  /** Counter reset before each detail build for stat row indexing. */
  private int statRowCount;

  // =====================================================================
  // Event Handling
  // =====================================================================

  @Override
  public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                              @NotNull AdminPageData data) {
    super.handleDataEvent(ref, store, data);

    if ("Nav".equals(data.button)) {
      NavBarHelper.handleNavEvent(
          data.navTarget != null ? data.navTarget : "",
          player, ref, store, playerRef, guiManager, GuiType.ADMIN
      );
      return;
    }

    // Capture search input from any event that includes it
    if (data.inputSearch != null) {
      searchQuery = data.inputSearch.isBlank() ? "" : data.inputSearch.trim();
    }

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "View" -> handleViewPlayer(data.target);
      case "Back" -> handleBack();
      case "Kick" -> handleKick(data.target);
      case "Mute" -> handleMute(data.target);
      case "Ban" -> handleBan(data.target);
      case "SearchChanged" -> {
        currentPage = 0;
        rebuildContent();
      }
      case "SortChanged" -> {
        sortMode = SortMode.fromString(data.sortMode);
        currentPage = 0;
        rebuildContent();
      }
      case "FilterChanged" -> {
        filterOnline = "online".equals(data.filterValue);
        currentPage = 0;
        rebuildContent();
      }
      case "PrevPage", "NextPage" -> {
        currentPage = data.page;
        rebuildContent();
      }
      default -> sendUpdate();
    }
  }

  private void handleViewPlayer(@Nullable String uuidStr) {
    if (uuidStr == null) return;

    try {
      viewingPlayer = UUID.fromString(uuidStr);
      statRowCount = 0;
    } catch (IllegalArgumentException ignored) {
      return;
    }

    rebuildContent();
  }

  private void handleBack() {
    viewingPlayer = null;
    rebuildContent();
  }

  private void handleKick(@Nullable String uuidStr) {
    if (uuidStr == null) return;

    ModerationManager modManager = getModerationManager();
    if (modManager == null) return;

    try {
      UUID targetUuid = UUID.fromString(uuidStr);
      HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
      PlayerRef target = plugin != null ? plugin.getTrackedPlayer(targetUuid) : null;
      String targetName = target != null ? target.getUsername() : getStoredUsername(targetUuid);
      if (targetName == null) targetName = uuidStr.substring(0, 8);

      modManager.kick(targetUuid, targetName, playerRef.getUuid(), playerRef.getUsername(), null);
      playerRef.sendMessage(
          CommandUtil.success(HEMessages.get(playerRef, AdminKeys.Players.KICKED))
      );
    } catch (Exception e) {
      ErrorHandler.report("[AdminPlayers] Kick failed", e);
    }

    // Return to list since the player is no longer online
    viewingPlayer = null;
    rebuildContent();
  }

  private void handleMute(@Nullable String uuidStr) {
    if (uuidStr == null) return;

    ModerationManager modManager = getModerationManager();
    if (modManager == null) return;

    try {
      UUID targetUuid = UUID.fromString(uuidStr);
      HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
      PlayerRef target = plugin != null ? plugin.getTrackedPlayer(targetUuid) : null;
      String targetName = target != null ? target.getUsername() : getStoredUsername(targetUuid);
      if (targetName == null) targetName = uuidStr.substring(0, 8);

      modManager.mute(targetUuid, targetName, playerRef.getUuid(), playerRef.getUsername(), null, null);
      playerRef.sendMessage(
          CommandUtil.success(HEMessages.get(playerRef, AdminKeys.Players.MUTED))
      );
    } catch (Exception e) {
      ErrorHandler.report("[AdminPlayers] Mute failed", e);
    }

    statRowCount = 0;
    rebuildContent();
  }

  private void handleBan(@Nullable String uuidStr) {
    if (uuidStr == null) return;

    ModerationManager modManager = getModerationManager();
    if (modManager == null) return;

    try {
      UUID targetUuid = UUID.fromString(uuidStr);
      HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
      PlayerRef target = plugin != null ? plugin.getTrackedPlayer(targetUuid) : null;
      String targetName = target != null ? target.getUsername() : getStoredUsername(targetUuid);
      if (targetName == null) targetName = uuidStr.substring(0, 8);

      modManager.ban(targetUuid, targetName, playerRef.getUuid(), playerRef.getUsername(), null, null);
      playerRef.sendMessage(
          CommandUtil.success(HEMessages.get(playerRef, AdminKeys.Players.BANNED))
      );
    } catch (Exception e) {
      ErrorHandler.report("[AdminPlayers] Ban failed", e);
    }

    // Return to list since the player was kicked
    viewingPlayer = null;
    rebuildContent();
  }

  // =====================================================================
  // Helpers
  // =====================================================================

  private void rebuildContent() {
    statRowCount = 0;
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildContent(cmd, events);
    sendUpdate(cmd, events, false);
  }

  @Nullable
  private ModerationManager getModerationManager() {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return null;
    HyperEssentials core = plugin.getHyperEssentials();
    if (core == null) return null;

    ModerationModule modModule = core.getModuleRegistry().getModule(ModerationModule.class);
    if (modModule != null && modModule.isEnabled()) {
      return modModule.getModerationManager();
    }
    return null;
  }

  @Nullable
  private PlayerData loadPlayerData(@NotNull UUID uuid) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return null;
    HyperEssentials core = plugin.getHyperEssentials();
    if (core == null) return null;

    // Try TpaManager cache first (online players)
    TeleportModule tm = core.getTeleportModule();
    if (tm != null && tm.isEnabled() && tm.getTpaManager() != null) {
      TpaManager tpa = tm.getTpaManager();
      PlayerData data = tpa.getPlayerData(uuid);
      if (data != null) return data;
    }

    // Fallback to storage for offline players
    try {
      return core.getStorageProvider().getPlayerDataStorage()
          .loadPlayerData(uuid).join().orElse(null);
    } catch (Exception e) {
      ErrorHandler.report("[AdminPlayers] Failed to load player data for " + uuid, e);
      return null;
    }
  }

  @Nullable
  private String getStoredUsername(@NotNull UUID uuid) {
    ModerationManager mod = getModerationManager();
    if (mod != null) {
      return mod.getStoredPlayerName(uuid);
    }
    // Fallback: load from storage
    PlayerData data = loadPlayerData(uuid);
    return data != null ? data.getUsername() : null;
  }
}
