package com.hyperessentials.gui.admin;

import com.hyperessentials.HyperEssentials;
import com.hyperessentials.data.PlayerData;
import com.hyperessentials.gui.GuiColors;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.module.moderation.ModerationManager;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.module.moderation.data.PunishmentType;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.DurationParser;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.HEMessages;
import com.hyperessentials.util.PlayerResolver;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin player moderation page with search, player detail, and punishment action forms.
 * Three view modes: SEARCH (player list + search), DETAIL (selected player info),
 * ACTION (punishment form).
 */
public class AdminPlayerModerationPage extends InteractiveCustomUIPage<AdminPageData> {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneId.systemDefault());

  private enum ViewMode { SEARCH, DETAIL, ACTION }

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;

  private ViewMode viewMode = ViewMode.SEARCH;

  /** Selected player UUID in DETAIL/ACTION modes. */
  @Nullable private UUID selectedUuid;
  @Nullable private String selectedName;

  /** Punishment type being applied in ACTION mode. */
  @Nullable private PunishmentType actionType;

  /** Last search results. */
  @Nullable private List<PlayerResolver.ResolvedPlayer> searchResults;

  /** Stat row counter for detail view. */
  private int statRowCount;

  public AdminPlayerModerationPage(
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
    cmd.append(UIPaths.ADMIN_PLAYER_MODERATION);
    NavBarHelper.setupAdminBar(playerRef, "moderation", guiManager.getAdminRegistry(), cmd, events);
    buildContent(cmd, events);
  }

  // =====================================================================
  // Content Builder (dispatches by mode)
  // =====================================================================

  private void buildContent(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    // Back button always navigates up one level
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#BackBtn",
        EventData.of("Button", "BackNav"), false
    );

    switch (viewMode) {
      case SEARCH -> buildSearchView(cmd, events);
      case DETAIL -> buildDetailView(cmd, events);
      case ACTION -> buildActionView(cmd, events);
    }
  }

  // =====================================================================
  // SEARCH View
  // =====================================================================

  private void buildSearchView(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    // Search button
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#SearchBtn",
        EventData.of("Button", "Search").append("@SearchInput", "#SearchInput.Value"), false
    );

    cmd.clear("#ContentList");
    cmd.appendInline("#ContentList", "Group #IndexCards { LayoutMode: Top; }");

    // If we have search results, show them
    if (searchResults != null && !searchResults.isEmpty()) {
      cmd.set("#StatusLabel.Text", searchResults.size() + " result" + (searchResults.size() != 1 ? "s" : ""));
      int i = 0;
      for (PlayerResolver.ResolvedPlayer rp : searchResults) {
        appendPlayerEntry(cmd, events, i, rp.uuid(), rp.username(),
            rp.source() == PlayerResolver.Source.ONLINE);
        i++;
      }
      return;
    }

    // Default: show online players
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    Map<UUID, PlayerRef> tracked = plugin != null ? plugin.getTrackedPlayers() : Map.of();

    cmd.set("#StatusLabel.Text", tracked.size() + " online");

    if (tracked.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", HEMessages.get(playerRef, AdminKeys.PlayerMod.SEARCH_EMPTY));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, AdminKeys.PlayerMod.SEARCH_HINT));
      return;
    }

    int i = 0;
    for (PlayerRef p : tracked.values()) {
      appendPlayerEntry(cmd, events, i, p.getUuid(), p.getUsername(), true);
      i++;
    }
  }

  private void appendPlayerEntry(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events,
                                  int index, @NotNull UUID uuid, @NotNull String name, boolean online) {
    cmd.append("#IndexCards", UIPaths.ADMIN_PLAYER_MOD_ENTRY);
    String idx = "#IndexCards[" + index + "]";

    cmd.set(idx + " #EntryName.Text", name);

    String info = uuid.toString().substring(0, 8) + "...";
    if (online) {
      info += " - " + HEMessages.get(playerRef, AdminKeys.PlayerMod.ONLINE_BADGE);
    }
    cmd.set(idx + " #EntryInfo.Text", info);

    // Online indicator bar color
    cmd.set(idx + " #OnlineBar.Background.Color",
        online ? GuiColors.STATUS_ONLINE : GuiColors.STATUS_OFFLINE);

    events.addEventBinding(
        CustomUIEventBindingType.Activating, idx + " #SelectBtn",
        EventData.of("Button", "SelectPlayer").append("Target", uuid.toString())
            .append("Value", name),
        false
    );
  }

  // =====================================================================
  // DETAIL View
  // =====================================================================

  private void buildDetailView(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    if (selectedUuid == null) {
      viewMode = ViewMode.SEARCH;
      buildSearchView(cmd, events);
      return;
    }

    statRowCount = 0;

    // Load player data
    PlayerData data = loadPlayerData(selectedUuid);
    boolean isOnline = isPlayerOnline(selectedUuid);

    cmd.set("#StatusLabel.Text", selectedName != null ? selectedName : "");

    cmd.clear("#ContentList");
    cmd.appendInline("#ContentList", "Group #IndexCards { LayoutMode: Top; }");

    // Player detail header
    cmd.append("#IndexCards", UIPaths.ADMIN_PLAYER_DETAIL);
    String detail = "#IndexCards[0]";

    cmd.set(detail + " #DetailName.Text", selectedName != null ? selectedName : selectedUuid.toString().substring(0, 8));

    // Stats
    addStatRow(cmd, detail + " #DetailStats",
        HEMessages.get(playerRef, AdminKeys.PlayerMod.DETAIL_UUID), selectedUuid.toString());

    // Status
    String statusText = isOnline ? "Online" : "Offline";
    String statusColor = isOnline ? GuiColors.STATUS_ONLINE : GuiColors.STATUS_OFFLINE;
    if (data != null && data.getActiveBan() != null) {
      statusText = "Banned";
      statusColor = GuiColors.DANGER;
    } else if (data != null && data.getActiveMute() != null) {
      statusText = "Muted";
      statusColor = GuiColors.WARNING;
    }
    addStatRow(cmd, detail + " #DetailStats",
        HEMessages.get(playerRef, AdminKeys.PlayerMod.DETAIL_STATUS), statusText, statusColor);

    if (data != null) {
      addStatRow(cmd, detail + " #DetailStats",
          HEMessages.get(playerRef, AdminKeys.PlayerMod.DETAIL_FIRST_JOIN),
          DATE_FORMAT.format(data.getFirstJoin()));
      addStatRow(cmd, detail + " #DetailStats",
          HEMessages.get(playerRef, AdminKeys.PlayerMod.DETAIL_LAST_SEEN),
          DATE_FORMAT.format(data.getLastJoin()));
      addStatRow(cmd, detail + " #DetailStats",
          HEMessages.get(playerRef, AdminKeys.PlayerMod.DETAIL_PLAYTIME),
          UIHelper.formatPlaytime(data.getTotalPlaytimeMs()));
    }

    // Wire action buttons
    wireActionButton(events, detail + " #KickBtn", "ActionKick");
    wireActionButton(events, detail + " #MuteBtn", "ActionMute");
    wireActionButton(events, detail + " #BanBtn", "ActionBan");

    // Punishment history
    ModerationManager modManager = getModerationManager();
    List<Punishment> history = modManager != null
        ? modManager.getHistory(selectedUuid) : List.of();

    // Add extra action buttons after the detail card
    cmd.appendInline("#IndexCards",
        "Group { Anchor: (Height: 32, Bottom: 6); LayoutMode: Left; "
        + "TextButton #WarnBtn { Text: \"Warn\"; Anchor: (Width: 65, Height: 26); } "
        + "Group { Anchor: (Width: 8); } "
        + "TextButton #IpBanBtn { Text: \"IP Ban\"; Anchor: (Width: 70, Height: 26); } "
        + "Group { Anchor: (Width: 8); } "
        + "TextButton #UnbanBtn { Text: \"Unban\"; Anchor: (Width: 65, Height: 26); } "
        + "Group { Anchor: (Width: 8); } "
        + "TextButton #UnmuteBtn { Text: \"Unmute\"; Anchor: (Width: 70, Height: 26); } }");

    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#IndexCards[1] #WarnBtn",
        EventData.of("Button", "ActionWarn"), false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#IndexCards[1] #IpBanBtn",
        EventData.of("Button", "ActionIpBan"), false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#IndexCards[1] #UnbanBtn",
        EventData.of("Button", "ActionUnban"), false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#IndexCards[1] #UnmuteBtn",
        EventData.of("Button", "ActionUnmute"), false
    );

    // History divider and list
    cmd.appendInline("#IndexCards",
        "Group { Anchor: (Height: 1, Bottom: 6); Background: (Color: #2a3a4a); }");

    cmd.appendInline("#IndexCards",
        "Label { Text: \"" + HEMessages.get(playerRef, AdminKeys.PlayerMod.DETAIL_HISTORY)
        + " (" + history.size() + ")\"; Style: (FontSize: 11, TextColor: #7c8b99); Anchor: (Height: 18, Bottom: 4); }");

    if (history.isEmpty()) {
      cmd.appendInline("#IndexCards",
          "Label { Text: \"" + HEMessages.get(playerRef, AdminKeys.PlayerMod.DETAIL_NO_HISTORY)
          + "\"; Style: (FontSize: 10, TextColor: #555555); Anchor: (Height: 16); }");
    } else {
      // Show most recent first, max 20
      List<Punishment> sorted = history.stream()
          .sorted(Comparator.comparing(Punishment::issuedAt).reversed())
          .limit(20)
          .toList();

      int hi = 0;
      for (Punishment p : sorted) {
        cmd.append("#IndexCards", UIPaths.ADMIN_PUNISHMENT_ENTRY);
        int cardIdx = 4 + hi; // 0=detail, 1=actions, 2=divider, 3=history label
        String hIdx = "#IndexCards[" + cardIdx + "]";

        String typeLabel = p.type().name();
        String typeColor = getTypeColor(p.type());
        cmd.set(hIdx + " #TypeBadge.Text", typeLabel);
        cmd.set(hIdx + " #TypeBadge.Style.TextColor", typeColor);
        cmd.set(hIdx + " #TypeBar.Background.Color", typeColor);

        cmd.set(hIdx + " #PlayerName.Text", p.playerName());
        cmd.set(hIdx + " #IssuerName.Text", "by " + p.issuerName());
        cmd.set(hIdx + " #Reason.Text", p.reason() != null ? p.reason() : HEMessages.get(playerRef, AdminKeys.Moderation.NO_REASON));

        if (p.isEffective()) {
          if (p.isPermanent()) {
            cmd.set(hIdx + " #Expires.Text", HEMessages.get(playerRef, AdminKeys.Moderation.PERMANENT));
          } else {
            cmd.set(hIdx + " #Expires.Text", UIHelper.formatDuration((int) (p.getRemainingMillis() / 1000)));
          }
        } else {
          cmd.set(hIdx + " #Expires.Text", p.active()
              ? HEMessages.get(playerRef, AdminKeys.Moderation.EXPIRED)
              : HEMessages.get(playerRef, AdminKeys.Moderation.REVOKED));
        }

        hi++;
      }
    }
  }

  private void wireActionButton(@NotNull UIEventBuilder events, @NotNull String selector, @NotNull String action) {
    events.addEventBinding(
        CustomUIEventBindingType.Activating, selector,
        EventData.of("Button", action), false
    );
  }

  private void addStatRow(@NotNull UICommandBuilder cmd, @NotNull String container,
                           @NotNull String label, @NotNull String value) {
    addStatRow(cmd, container, label, value, null);
  }

  private void addStatRow(@NotNull UICommandBuilder cmd, @NotNull String container,
                           @NotNull String label, @NotNull String value, @Nullable String valueColor) {
    cmd.append(container, UIPaths.STAT_ROW);
    int idx = statRowCount++;
    String row = container + "[" + idx + "]";
    cmd.set(row + " #StatLabel.Text", label);
    cmd.set(row + " #StatValue.Text", value);
    if (valueColor != null) {
      cmd.set(row + " #StatValue.Style.TextColor", valueColor);
    }
  }

  // =====================================================================
  // ACTION View
  // =====================================================================

  private void buildActionView(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    if (selectedUuid == null || actionType == null) {
      viewMode = ViewMode.DETAIL;
      buildDetailView(cmd, events);
      return;
    }

    String playerName = selectedName != null ? selectedName : selectedUuid.toString().substring(0, 8);
    cmd.set("#StatusLabel.Text", actionType.name() + " - " + playerName);

    cmd.clear("#ContentList");
    cmd.appendInline("#ContentList", "Group #IndexCards { LayoutMode: Top; }");

    cmd.append("#IndexCards", UIPaths.ADMIN_PUNISHMENT_ACTION);
    String form = "#IndexCards[0]";

    cmd.set(form + " #ActionTitle.Text", actionType.name() + ": " + playerName);

    // Hide duration for types that don't use it
    boolean showDuration = (actionType == PunishmentType.BAN || actionType == PunishmentType.IPBAN
        || actionType == PunishmentType.MUTE);
    if (!showDuration) {
      cmd.set(form + " #DurationLabel.Text", "");
      cmd.set(form + " #DurationHint.Text", "");
    }

    // Wire confirm/cancel
    EventData confirmData = EventData.of("Button", "ConfirmAction");
    if (showDuration) {
      confirmData.append("@DurationInput", "#DurationInput.Value");
    }
    confirmData.append("@ReasonInput", "#ReasonInput.Value");

    events.addEventBinding(
        CustomUIEventBindingType.Activating, form + " #ConfirmBtn",
        confirmData, false
    );

    events.addEventBinding(
        CustomUIEventBindingType.Activating, form + " #CancelBtn",
        EventData.of("Button", "CancelAction"), false
    );
  }

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
      case "BackNav" -> handleBackNav();
      case "Search" -> handleSearch(data.inputSearch);
      case "SelectPlayer" -> handleSelectPlayer(data.target, data.value);
      case "ActionBan" -> enterAction(PunishmentType.BAN);
      case "ActionIpBan" -> enterAction(PunishmentType.IPBAN);
      case "ActionMute" -> enterAction(PunishmentType.MUTE);
      case "ActionKick" -> handleDirectAction(PunishmentType.KICK, null, null);
      case "ActionWarn" -> enterAction(PunishmentType.WARN);
      case "ActionUnban" -> handleUnban();
      case "ActionUnmute" -> handleUnmute();
      case "ConfirmAction" -> handleConfirmAction(data.inputDuration, data.inputReason);
      case "CancelAction" -> { viewMode = ViewMode.DETAIL; rebuildContent(); }
      default -> sendUpdate();
    }
  }

  private void handleBackNav() {
    switch (viewMode) {
      case ACTION -> {
        viewMode = ViewMode.DETAIL;
        rebuildContent();
      }
      case DETAIL -> {
        viewMode = ViewMode.SEARCH;
        selectedUuid = null;
        selectedName = null;
        rebuildContent();
      }
      case SEARCH -> {
        // At top level — just send update (user can press Escape to dismiss)
        sendUpdate();
      }
    }
  }

  private void handleSearch(@Nullable String query) {
    if (query == null || query.trim().isEmpty()) {
      searchResults = null;
    } else {
      searchResults = PlayerResolver.search(query.trim(), playerRef.getUuid());
    }
    viewMode = ViewMode.SEARCH;
    rebuildContent();
  }

  private void handleSelectPlayer(@Nullable String uuidStr, @Nullable String name) {
    if (uuidStr == null) return;
    try {
      selectedUuid = UUID.fromString(uuidStr);
      selectedName = name;
      viewMode = ViewMode.DETAIL;
    } catch (IllegalArgumentException ignored) {
      return;
    }
    rebuildContent();
  }

  private void enterAction(@NotNull PunishmentType type) {
    actionType = type;
    viewMode = ViewMode.ACTION;
    rebuildContent();
  }

  private void handleDirectAction(@NotNull PunishmentType type,
                                   @Nullable String durationStr, @Nullable String reason) {
    if (selectedUuid == null) return;
    ModerationManager mod = getModerationManager();
    if (mod == null) return;

    String name = selectedName != null ? selectedName : selectedUuid.toString().substring(0, 8);

    try {
      switch (type) {
        case KICK -> mod.kick(selectedUuid, name, playerRef.getUuid(), playerRef.getUsername(), reason);
        case WARN -> mod.warn(selectedUuid, name, playerRef.getUuid(), playerRef.getUsername(), reason);
        default -> {}
      }
      playerRef.sendMessage(com.hyperessentials.util.HEMessageUtil.adminSuccess(
          playerRef, AdminKeys.PlayerMod.ACTION_SUCCESS));
    } catch (Exception e) {
      ErrorHandler.report("[AdminPlayerMod] Action failed", e);
      playerRef.sendMessage(com.hyperessentials.util.HEMessageUtil.adminError(
          playerRef, AdminKeys.PlayerMod.ACTION_FAILED));
    }

    rebuildContent();
  }

  private void handleConfirmAction(@Nullable String durationStr, @Nullable String reason) {
    if (selectedUuid == null || actionType == null) return;
    ModerationManager mod = getModerationManager();
    if (mod == null) return;

    String name = selectedName != null ? selectedName : selectedUuid.toString().substring(0, 8);
    Long durationMs = null;

    if (durationStr != null && !durationStr.trim().isEmpty()) {
      long parsed = DurationParser.parse(durationStr.trim());
      if (parsed > 0) {
        durationMs = parsed;
      }
    }

    String effectiveReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : null;

    try {
      switch (actionType) {
        case BAN -> mod.ban(selectedUuid, name, playerRef.getUuid(), playerRef.getUsername(), effectiveReason, durationMs);
        case IPBAN -> handleIpBanAction(mod, name, effectiveReason, durationMs);
        case MUTE -> mod.mute(selectedUuid, name, playerRef.getUuid(), playerRef.getUsername(), effectiveReason, durationMs);
        case KICK -> mod.kick(selectedUuid, name, playerRef.getUuid(), playerRef.getUsername(), effectiveReason);
        case WARN -> mod.warn(selectedUuid, name, playerRef.getUuid(), playerRef.getUsername(), effectiveReason);
      }

      playerRef.sendMessage(com.hyperessentials.util.HEMessageUtil.adminSuccess(
          playerRef, AdminKeys.PlayerMod.ACTION_SUCCESS));
    } catch (Exception e) {
      ErrorHandler.report("[AdminPlayerMod] Action failed", e);
      playerRef.sendMessage(com.hyperessentials.util.HEMessageUtil.adminError(
          playerRef, AdminKeys.PlayerMod.ACTION_FAILED));
    }

    viewMode = ViewMode.DETAIL;
    actionType = null;
    rebuildContent();
  }

  private void handleIpBanAction(@NotNull ModerationManager mod, @NotNull String name,
                                  @Nullable String reason, @Nullable Long durationMs) {
    // Need to get the player's IP
    String ip = mod.getPlayerIp(selectedUuid);
    if (ip != null) {
      mod.ipBan(ip, playerRef.getUuid(), playerRef.getUsername(), reason, durationMs,
          selectedUuid, name);
      mod.kickPlayersWithIp(ip, "Your IP has been banned.");
    } else {
      // Fall back to regular ban if IP not available
      mod.ban(selectedUuid, name, playerRef.getUuid(), playerRef.getUsername(),
          reason != null ? reason : "IP ban (IP unavailable, regular ban applied)", durationMs);
    }
  }

  private void handleUnban() {
    if (selectedUuid == null) return;
    ModerationManager mod = getModerationManager();
    if (mod == null) return;

    try {
      mod.unban(selectedUuid, playerRef.getUuid(), playerRef.getUsername());
      playerRef.sendMessage(com.hyperessentials.util.HEMessageUtil.adminSuccess(
          playerRef, AdminKeys.PlayerMod.ACTION_SUCCESS));
    } catch (Exception e) {
      ErrorHandler.report("[AdminPlayerMod] Unban failed", e);
    }
    rebuildContent();
  }

  private void handleUnmute() {
    if (selectedUuid == null) return;
    ModerationManager mod = getModerationManager();
    if (mod == null) return;

    try {
      mod.unmute(selectedUuid, playerRef.getUuid(), playerRef.getUsername());
      playerRef.sendMessage(com.hyperessentials.util.HEMessageUtil.adminSuccess(
          playerRef, AdminKeys.PlayerMod.ACTION_SUCCESS));
    } catch (Exception e) {
      ErrorHandler.report("[AdminPlayerMod] Unmute failed", e);
    }
    rebuildContent();
  }

  // =====================================================================
  // Helpers
  // =====================================================================

  @NotNull
  private static String getTypeColor(@NotNull PunishmentType type) {
    return switch (type) {
      case BAN -> GuiColors.DANGER;
      case IPBAN -> "#AA0000";
      case MUTE -> GuiColors.WARNING;
      case KICK -> GuiColors.INFO;
      case WARN -> "#FFFF55";
    };
  }

  private boolean isPlayerOnline(@NotNull UUID uuid) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    return plugin != null && plugin.getTrackedPlayer(uuid) != null;
  }

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
      ErrorHandler.report("[AdminPlayerMod] Failed to load player data for " + uuid, e);
      return null;
    }
  }
}
