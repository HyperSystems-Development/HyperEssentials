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
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
 */
public class AdminPlayersPage extends InteractiveCustomUIPage<AdminPageData> {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneId.systemDefault());

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;

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

    // Sort players by username
    List<PlayerRef> sorted = new ArrayList<>(tracked.values());
    sorted.sort(Comparator.comparing(PlayerRef::getUsername, String.CASE_INSENSITIVE_ORDER));

    cmd.clear("#PlayerList");
    cmd.appendInline("#PlayerList", "Group #IndexCards { LayoutMode: Top; }");

    if (sorted.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", HEMessages.get(playerRef, AdminKeys.Players.EMPTY_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, AdminKeys.Players.EMPTY_MESSAGE));
      return;
    }

    int i = 0;
    for (PlayerRef p : sorted) {
      cmd.append("#IndexCards", UIPaths.ADMIN_PLAYER_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #PlayerName.Text", p.getUsername());
      cmd.set(idx + " #PlayerInfo.Text", HEMessages.get(playerRef, AdminKeys.Players.PLAYER_INFO, p.getUuid().toString()));

      // Wire View button
      events.addEventBinding(
          CustomUIEventBindingType.Activating, idx + " #ViewBtn",
          EventData.of("Button", "View").append("Target", p.getUuid().toString()),
          false
      );

      i++;
    }
  }

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
    cmd.appendInline("#IndexCards",
        "Group { Anchor: (Height: 28, Bottom: 6); LayoutMode: Left; "
        + "TextButton #BackBtn { Text: \"Back\"; Anchor: (Width: 70, Height: 24); } }");

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
