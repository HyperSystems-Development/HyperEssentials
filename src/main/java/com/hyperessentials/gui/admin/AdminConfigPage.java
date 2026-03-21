package com.hyperessentials.gui.admin;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminConfigData;
import com.hyperessentials.util.Logger;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Admin Config Editor page with 13 tabbed sections and two-column layout.
 *
 * <p>
 * Allows admins to view and edit all HyperEssentials configuration settings
 * directly from an in-game GUI. Changes are tracked as pending until saved.
 * Uses targeted updates (no full page rebuild) for toggle/increment/text changes.
 */
public class AdminConfigPage extends InteractiveCustomUIPage<AdminConfigData> {

  // ================================================================
  // Edit Session Cache
  // ================================================================

  /** Cached edit sessions per player — survives page close/reopen. */
  private static final ConcurrentHashMap<UUID, EditSession> editSessions = new ConcurrentHashMap<>();

  /** Snapshot of pending edits that survives page close/reopen. */
  private record EditSession(
      String tab,
      Map<String, Object> pendingChanges,
      Map<String, Object> originalValues
  ) {}

  /** Removes the cached edit session for a player. */
  public static void clearSession(UUID playerId) {
    editSessions.remove(playerId);
  }

  private static final String[] TABS = {
      "core", "homes", "warps", "spawns", "teleport", "warmup", "kits",
      "moderation", "vanish", "utility", "announcements", "debug", "backup"
  };

  /** Setting type for targeted updates. */
  private enum SettingKind { BOOL, INT, DOUBLE, STRING, COLOR, ENUM }

  /** Layout size for template selection. */
  private enum LayoutSize { NARROW, STANDARD }

  private static LayoutSize getLayoutSize(String tab) {
    return switch (tab) {
      case "warps", "spawns", "warmup", "kits", "backup" -> LayoutSize.NARROW;
      default -> LayoutSize.STANDARD;
    };
  }

  // ================================================================
  // Instance Fields
  // ================================================================

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;

  private String currentTab = "core";
  private final ConcurrentHashMap<String, Object> pendingChanges = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Object> originalValues = new ConcurrentHashMap<>();
  private boolean saveConfirmActive = false;
  private boolean resetConfirmActive = false;

  /** Fields with invalid input that must be fixed before saving. */
  private final java.util.Set<String> invalidFields = ConcurrentHashMap.newKeySet();

  /** Maps settingKey -> container selector (e.g. "#LeftContainer[3]") for targeted updates. */
  private final Map<String, String> settingSelectors = new HashMap<>();

  /** Maps settingKey -> its SettingKind for targeted updates. */
  private final Map<String, SettingKind> settingKinds = new HashMap<>();

  /** Debounce timestamp for text input — only the latest scheduled refresh runs. */
  private static final long DEBOUNCE_MS = 600;
  private volatile long lastTextInputNanos = 0;

  /** Row counter for indexed selectors. Reset each build cycle. */
  private int leftRowIdx;
  private int rightRowIdx;
  private boolean appendingToLeft = true;

  // ================================================================
  // Constructor
  // ================================================================

  public AdminConfigPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminConfigData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    restoreSession();
  }

  /** Restores cached edit session if one exists for this player. */
  private void restoreSession() {
    EditSession session = editSessions.remove(playerRef.getUuid());
    if (session != null) {
      this.currentTab = session.tab();
      this.pendingChanges.putAll(session.pendingChanges());
      this.originalValues.putAll(session.originalValues());
    }
  }

  /** Saves current edit session to cache if there are pending changes. */
  private void saveSession() {
    if (!pendingChanges.isEmpty()) {
      editSessions.put(playerRef.getUuid(), new EditSession(
          currentTab,
          new HashMap<>(pendingChanges),
          new HashMap<>(originalValues)
      ));
    } else {
      editSessions.remove(playerRef.getUuid());
    }
  }

  // ================================================================
  // Build
  // ================================================================

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    String template = switch (getLayoutSize(currentTab)) {
      case NARROW -> UIPaths.ADMIN_CONFIG_NARROW;
      case STANDARD -> UIPaths.ADMIN_CONFIG_STANDARD;
    };
    cmd.append(template);
    NavBarHelper.setupAdminBar(playerRef, "config", guiManager.getAdminRegistry(), cmd, events);
    buildDynamicContent(cmd, events);
  }

  /** Full rebuild of all dynamic content: tabs, settings, status, action bindings. */
  private void buildDynamicContent(UICommandBuilder cmd, UIEventBuilder events) {
    // Bind tab buttons
    for (int i = 0; i < TABS.length; i++) {
      String tab = TABS[i];
      String tabId = "#Tab" + capitalize(tab);
      events.addEventBinding(CustomUIEventBindingType.Activating, tabId,
          EventData.of("Button", "TabSwitch").append("Tab", tab), false);
    }

    updatePageTitle(cmd);
    updateStatusLabel(cmd);

    // Clear only the containers that exist in the current template
    switch (getLayoutSize(currentTab)) {
      case NARROW -> cmd.clear("#LeftContainer");
      case STANDARD -> { cmd.clear("#LeftContainer"); cmd.clear("#RightContainer"); }
    }
    leftRowIdx = 0;
    rightRowIdx = 0;
    appendingToLeft = true;
    settingSelectors.clear();
    settingKinds.clear();
    buildTabContent(cmd, events);

    // Action bar buttons
    events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveBtn",
        EventData.of("Button", "Save"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#RevertBtn",
        EventData.of("Button", "Revert"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ResetBtn",
        EventData.of("Button", "ResetDefaults"), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#ReloadBtn",
        EventData.of("Button", "Reload"), false);

    // Save button: confirm state + disable when invalid
    if (!invalidFields.isEmpty()) {
      cmd.set("#SaveBtn.Disabled", true);
    }
    if (saveConfirmActive) {
      cmd.set("#SaveBtn.Text", "Confirm Save");
    }

    if (resetConfirmActive) {
      cmd.set("#ResetBtn.Text", "Confirm Reset");
    }
  }

  private void updatePageTitle(UICommandBuilder cmd) {
    String tabName = switch (currentTab) {
      case "core" -> "Core";
      case "homes" -> "Homes";
      case "warps" -> "Warps";
      case "spawns" -> "Spawns";
      case "teleport" -> "Teleport";
      case "warmup" -> "Warmup";
      case "kits" -> "Kits";
      case "moderation" -> "Moderation";
      case "vanish" -> "Vanish";
      case "utility" -> "Utility";
      case "announcements" -> "Announcements";
      case "debug" -> "Debug";
      case "backup" -> "Backup";
      default -> currentTab;
    };
    cmd.set("#PageTitle.Text", "Config: " + tabName);
  }

  private void updateStatusLabel(UICommandBuilder cmd) {
    int errorCount = invalidFields.size();
    int changeCount = pendingChanges.size();
    if (errorCount > 0) {
      cmd.set("#StatusLabel.Text", errorCount + " invalid");
      cmd.set("#StatusLabel.Style.TextColor", "#FF4444");
    } else if (changeCount > 0) {
      cmd.set("#StatusLabel.Text", changeCount + " changes pending");
      cmd.set("#StatusLabel.Style.TextColor", "#FFAA00");
    } else {
      cmd.set("#StatusLabel.Text", "No changes");
      cmd.set("#StatusLabel.Style.TextColor", "#888888");
    }
  }

  /**
   * Targeted update: just update the label color for a single setting + status bar.
   * No full page rebuild, no clearing containers, no re-binding events.
   */
  private void updateSettingAndStatus(Ref<EntityStore> ref, Store<EntityStore> store, String key) {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    String selector = settingSelectors.get(key);
    if (selector != null) {
      boolean pending = pendingChanges.containsKey(key);
      String color = pending ? "#FFAA00" : "#CCCCCC";
      cmd.set(selector + " #SettingLabel.Style.TextColor", color);

      SettingKind kind = settingKinds.get(key);

      // Update the value display for types where the UI doesn't self-update
      if (kind == SettingKind.INT) {
        Object orig = originalValues.get(key);
        int val = pending ? ((Number) pendingChanges.get(key)).intValue()
            : (orig instanceof Number n ? n.intValue() : 0);
        cmd.set(selector + " #NumInput.Value", String.valueOf(val));
        cmd.set(selector + " #NumInput.Style.TextColor", pending ? "#FFAA00" : "#FFFFFF");
      } else if (kind == SettingKind.DOUBLE) {
        Object orig = originalValues.get(key);
        double val = pending ? ((Number) pendingChanges.get(key)).doubleValue()
            : (orig instanceof Number n ? n.doubleValue() : 0.0);
        cmd.set(selector + " #NumInput.Value", String.format("%.2f", val));
        cmd.set(selector + " #NumInput.Style.TextColor", pending ? "#FFAA00" : "#FFFFFF");
      }
      // BOOL: checkbox handles its own visual toggle
      // STRING: text field has user input already, don't overwrite
      if (kind == SettingKind.COLOR) {
        Object orig = originalValues.get(key);
        String val = pending ? String.valueOf(pendingChanges.get(key))
            : (orig instanceof String s ? s : "#FFFFFF");
        cmd.set(selector + " #ColorInput.Value", val);
        cmd.set(selector + " #ColorInput.Style.TextColor", pending ? "#FFAA00" : "#FFFFFF");
        cmd.set(selector + " #ColorPicker.Color", val);
      }
    }

    updateStatusLabel(cmd);
    sendUpdate(cmd, events, false);
  }

  // ================================================================
  // Tab Content Routing
  // ================================================================

  private void buildTabContent(UICommandBuilder cmd, UIEventBuilder events) {
    switch (currentTab) {
      case "core" -> buildCoreTab(cmd, events);
      case "homes" -> buildHomesTab(cmd, events);
      case "warps" -> buildWarpsTab(cmd, events);
      case "spawns" -> buildSpawnsTab(cmd, events);
      case "teleport" -> buildTeleportTab(cmd, events);
      case "warmup" -> buildWarmupTab(cmd, events);
      case "kits" -> buildKitsTab(cmd, events);
      case "moderation" -> buildModerationTab(cmd, events);
      case "vanish" -> buildVanishTab(cmd, events);
      case "utility" -> buildUtilityTab(cmd, events);
      case "announcements" -> buildAnnouncementsTab(cmd, events);
      case "debug" -> buildDebugTab(cmd, events);
      case "backup" -> buildBackupTab(cmd, events);
    }
  }

  // ================================================================
  // Column Management
  // ================================================================

  private void setColumn(boolean left) {
    this.appendingToLeft = left;
  }

  private String getContainerId() {
    return appendingToLeft ? "#LeftContainer" : "#RightContainer";
  }

  private int getRowIdx() {
    return appendingToLeft ? leftRowIdx : rightRowIdx;
  }

  private void incrementRowIdx() {
    if (appendingToLeft) {
      leftRowIdx++;
    } else {
      rightRowIdx++;
    }
  }

  // ================================================================
  // Value Helpers
  // ================================================================

  private boolean boolVal(String key) {
    Object v = ConfigSnapshot.getValue(key);
    return v instanceof Boolean b && b;
  }

  private int intVal(String key) {
    Object v = ConfigSnapshot.getValue(key);
    return v instanceof Number n ? n.intValue() : 0;
  }

  private double doubleVal(String key) {
    Object v = ConfigSnapshot.getValue(key);
    return v instanceof Number n ? n.doubleValue() : 0.0;
  }

  private String strVal(String key) {
    Object v = ConfigSnapshot.getValue(key);
    return v instanceof String s ? s : "";
  }

  private String colorVal(String key) {
    Object v = ConfigSnapshot.getValue(key);
    return v instanceof String s ? s : "#FFFFFF";
  }

  // ================================================================
  // Core Tab
  // ================================================================

  private void buildCoreTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "Messages");
    addStringSetting(cmd, events, "core.prefixText", "Prefix Text", strVal("core.prefixText"));
    addColorSetting(cmd, events, "core.prefixColor", "Prefix Color", colorVal("core.prefixColor"));
    addColorSetting(cmd, events, "core.prefixBracketColor", "Bracket Color", colorVal("core.prefixBracketColor"));
    addColorSetting(cmd, events, "core.primaryColor", "Primary Color", colorVal("core.primaryColor"));
    addColorSetting(cmd, events, "core.secondaryColor", "Secondary Color", colorVal("core.secondaryColor"));
    addColorSetting(cmd, events, "core.errorColor", "Error Color", colorVal("core.errorColor"));

    setColumn(false);
    addSectionHeader(cmd, "Permissions");
    addBooleanSetting(cmd, events, "core.adminRequiresOp", "Admin Requires OP", boolVal("core.adminRequiresOp"));
    addBooleanSetting(cmd, events, "core.allowWithoutPermissionMod", "Allow Without Perm Mod", boolVal("core.allowWithoutPermissionMod"));

    addSectionHeader(cmd, "Language");
    addLocaleSetting(cmd, events, "core.defaultLanguage", "Default Language", strVal("core.defaultLanguage"));
    addBooleanSetting(cmd, events, "core.usePlayerLanguage", "Use Player Language", boolVal("core.usePlayerLanguage"));

    addSectionHeader(cmd, "Updates");
    addBooleanSetting(cmd, events, "core.updateCheck", "Update Check", boolVal("core.updateCheck"));
  }

  // ================================================================
  // Homes Tab
  // ================================================================

  private void buildHomesTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "General");
    addBooleanSetting(cmd, events, "homes.enabled", "Enabled", boolVal("homes.enabled"));
    addIntSetting(cmd, events, "homes.defaultHomeLimit", "Default Home Limit", intVal("homes.defaultHomeLimit"));

    addSectionHeader(cmd, "Factions Integration");
    addBooleanSetting(cmd, events, "homes.factionsEnabled", "Factions Enabled", boolVal("homes.factionsEnabled"));
    addBooleanSetting(cmd, events, "homes.allowInOwnTerritory", "Allow Own Territory", boolVal("homes.allowInOwnTerritory"));
    addBooleanSetting(cmd, events, "homes.allowInAllyTerritory", "Allow Ally Territory", boolVal("homes.allowInAllyTerritory"));
    addBooleanSetting(cmd, events, "homes.allowInNeutralTerritory", "Allow Neutral Territory", boolVal("homes.allowInNeutralTerritory"));
    addBooleanSetting(cmd, events, "homes.allowInEnemyTerritory", "Allow Enemy Territory", boolVal("homes.allowInEnemyTerritory"));
    addBooleanSetting(cmd, events, "homes.allowInWilderness", "Allow Wilderness", boolVal("homes.allowInWilderness"));

    setColumn(false);
    addSectionHeader(cmd, "Bed Sync");
    addBooleanSetting(cmd, events, "homes.bedSyncEnabled", "Bed Sync Enabled", boolVal("homes.bedSyncEnabled"));
    addStringSetting(cmd, events, "homes.bedHomeName", "Bed Home Name", strVal("homes.bedHomeName"));

    addSectionHeader(cmd, "Sharing");
    addBooleanSetting(cmd, events, "homes.shareEnabled", "Share Enabled", boolVal("homes.shareEnabled"));
    addIntSetting(cmd, events, "homes.maxSharesPerHome", "Max Shares Per Home", intVal("homes.maxSharesPerHome"));
  }

  // ================================================================
  // Warps Tab (narrow — left only)
  // ================================================================

  private void buildWarpsTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "General");
    addBooleanSetting(cmd, events, "warps.enabled", "Enabled", boolVal("warps.enabled"));
    addStringSetting(cmd, events, "warps.defaultCategory", "Default Category", strVal("warps.defaultCategory"));
  }

  // ================================================================
  // Spawns Tab (narrow)
  // ================================================================

  private void buildSpawnsTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "General");
    addBooleanSetting(cmd, events, "spawns.enabled", "Enabled", boolVal("spawns.enabled"));
    addStringSetting(cmd, events, "spawns.defaultSpawnName", "Default Spawn Name", strVal("spawns.defaultSpawnName"));
    addBooleanSetting(cmd, events, "spawns.teleportOnJoin", "Teleport on Join", boolVal("spawns.teleportOnJoin"));
    addBooleanSetting(cmd, events, "spawns.teleportOnRespawn", "Teleport on Respawn", boolVal("spawns.teleportOnRespawn"));
    addBooleanSetting(cmd, events, "spawns.perWorldSpawns", "Per-World Spawns", boolVal("spawns.perWorldSpawns"));
  }

  // ================================================================
  // Teleport Tab
  // ================================================================

  private void buildTeleportTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "TPA");
    addBooleanSetting(cmd, events, "teleport.enabled", "Enabled", boolVal("teleport.enabled"));
    addIntSetting(cmd, events, "teleport.tpaTimeout", "TPA Timeout", intVal("teleport.tpaTimeout"));
    addIntSetting(cmd, events, "teleport.tpaCooldown", "TPA Cooldown", intVal("teleport.tpaCooldown"));
    addIntSetting(cmd, events, "teleport.maxPendingTpa", "Max Pending TPA", intVal("teleport.maxPendingTpa"));

    addSectionHeader(cmd, "Back");
    addIntSetting(cmd, events, "teleport.backHistorySize", "Back History Size", intVal("teleport.backHistorySize"));
    addBooleanSetting(cmd, events, "teleport.saveBackOnDeath", "Save Back on Death", boolVal("teleport.saveBackOnDeath"));
    addBooleanSetting(cmd, events, "teleport.saveBackOnTeleport", "Save Back on Teleport", boolVal("teleport.saveBackOnTeleport"));
    addBooleanSetting(cmd, events, "teleport.backAllowSelectAny", "Allow Select Any", boolVal("teleport.backAllowSelectAny"));

    addSectionHeader(cmd, "Back Factions");
    addBooleanSetting(cmd, events, "teleport.backFactionsEnabled", "Back Factions Enabled", boolVal("teleport.backFactionsEnabled"));
    addBooleanSetting(cmd, events, "teleport.backAllowInOwnTerritory", "Allow Own Territory", boolVal("teleport.backAllowInOwnTerritory"));
    addBooleanSetting(cmd, events, "teleport.backAllowInAllyTerritory", "Allow Ally Territory", boolVal("teleport.backAllowInAllyTerritory"));
    addBooleanSetting(cmd, events, "teleport.backAllowInNeutralTerritory", "Allow Neutral Territory", boolVal("teleport.backAllowInNeutralTerritory"));
    addBooleanSetting(cmd, events, "teleport.backAllowInEnemyTerritory", "Allow Enemy Territory", boolVal("teleport.backAllowInEnemyTerritory"));
    addBooleanSetting(cmd, events, "teleport.backAllowInWilderness", "Allow Wilderness", boolVal("teleport.backAllowInWilderness"));

    setColumn(false);
    addSectionHeader(cmd, "RTP");
    addIntSetting(cmd, events, "teleport.rtpCenterX", "RTP Center X", intVal("teleport.rtpCenterX"));
    addIntSetting(cmd, events, "teleport.rtpCenterZ", "RTP Center Z", intVal("teleport.rtpCenterZ"));
    addIntSetting(cmd, events, "teleport.rtpMinRadius", "RTP Min Radius", intVal("teleport.rtpMinRadius"));
    addIntSetting(cmd, events, "teleport.rtpMaxRadius", "RTP Max Radius", intVal("teleport.rtpMaxRadius"));
    addIntSetting(cmd, events, "teleport.rtpMaxAttempts", "RTP Max Attempts", intVal("teleport.rtpMaxAttempts"));
    addBooleanSetting(cmd, events, "teleport.rtpPlayerRelative", "RTP Player Relative", boolVal("teleport.rtpPlayerRelative"));

    addSectionHeader(cmd, "RTP Factions");
    addBooleanSetting(cmd, events, "teleport.rtpFactionAvoidanceEnabled", "Faction Avoidance", boolVal("teleport.rtpFactionAvoidanceEnabled"));
    addIntSetting(cmd, events, "teleport.rtpFactionAvoidanceBufferRadius", "Avoidance Buffer", intVal("teleport.rtpFactionAvoidanceBufferRadius"));

    addSectionHeader(cmd, "RTP Safety");
    addBooleanSetting(cmd, events, "teleport.rtpSafetyAvoidWater", "Avoid Water", boolVal("teleport.rtpSafetyAvoidWater"));
    addBooleanSetting(cmd, events, "teleport.rtpSafetyAvoidDangerousFluids", "Avoid Dangerous Fluids", boolVal("teleport.rtpSafetyAvoidDangerousFluids"));
    addIntSetting(cmd, events, "teleport.rtpSafetyMinY", "Safety Min Y", intVal("teleport.rtpSafetyMinY"));
    addIntSetting(cmd, events, "teleport.rtpSafetyMaxY", "Safety Max Y", intVal("teleport.rtpSafetyMaxY"));
    addIntSetting(cmd, events, "teleport.rtpSafetyAirAboveHead", "Air Above Head", intVal("teleport.rtpSafetyAirAboveHead"));
  }

  // ================================================================
  // Warmup Tab (narrow)
  // ================================================================

  private void buildWarmupTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "General");
    addBooleanSetting(cmd, events, "warmup.enabled", "Enabled", boolVal("warmup.enabled"));
    addBooleanSetting(cmd, events, "warmup.cancelOnMove", "Cancel on Move", boolVal("warmup.cancelOnMove"));
    addBooleanSetting(cmd, events, "warmup.cancelOnDamage", "Cancel on Damage", boolVal("warmup.cancelOnDamage"));
    addBooleanSetting(cmd, events, "warmup.safeTeleport", "Safe Teleport", boolVal("warmup.safeTeleport"));
    addIntSetting(cmd, events, "warmup.safeRadius", "Safe Radius", intVal("warmup.safeRadius"));
  }

  // ================================================================
  // Kits Tab (narrow)
  // ================================================================

  private void buildKitsTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "General");
    addBooleanSetting(cmd, events, "kits.enabled", "Enabled", boolVal("kits.enabled"));
    addIntSetting(cmd, events, "kits.defaultCooldownSeconds", "Default Cooldown (sec)", intVal("kits.defaultCooldownSeconds"));
    addBooleanSetting(cmd, events, "kits.oneTimeDefault", "One-Time Default", boolVal("kits.oneTimeDefault"));
  }

  // ================================================================
  // Moderation Tab
  // ================================================================

  private void buildModerationTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "General");
    addBooleanSetting(cmd, events, "moderation.enabled", "Enabled", boolVal("moderation.enabled"));
    addIntSetting(cmd, events, "moderation.maxWarningsBeforeBan", "Max Warnings Before Ban", intVal("moderation.maxWarningsBeforeBan"));
    addIntSetting(cmd, events, "moderation.maxHistoryPerPlayer", "Max History Per Player", intVal("moderation.maxHistoryPerPlayer"));
    addIntSetting(cmd, events, "moderation.freezeCheckIntervalMs", "Freeze Check (ms)", intVal("moderation.freezeCheckIntervalMs"));

    addSectionHeader(cmd, "Broadcasts");
    addBooleanSetting(cmd, events, "moderation.broadcastBans", "Broadcast Bans", boolVal("moderation.broadcastBans"));
    addBooleanSetting(cmd, events, "moderation.broadcastKicks", "Broadcast Kicks", boolVal("moderation.broadcastKicks"));
    addBooleanSetting(cmd, events, "moderation.broadcastMutes", "Broadcast Mutes", boolVal("moderation.broadcastMutes"));
    addBooleanSetting(cmd, events, "moderation.broadcastWarnings", "Broadcast Warnings", boolVal("moderation.broadcastWarnings"));

    setColumn(false);
    addSectionHeader(cmd, "Default Reasons");
    addStringSetting(cmd, events, "moderation.defaultBanReason", "Ban Reason", strVal("moderation.defaultBanReason"));
    addStringSetting(cmd, events, "moderation.defaultMuteReason", "Mute Reason", strVal("moderation.defaultMuteReason"));
    addStringSetting(cmd, events, "moderation.defaultKickReason", "Kick Reason", strVal("moderation.defaultKickReason"));
    addStringSetting(cmd, events, "moderation.defaultWarnReason", "Warn Reason", strVal("moderation.defaultWarnReason"));

    addSectionHeader(cmd, "Messages");
    addStringSetting(cmd, events, "moderation.mutedChatMessage", "Muted Chat Message", strVal("moderation.mutedChatMessage"));
    addStringSetting(cmd, events, "moderation.freezeMessage", "Freeze Message", strVal("moderation.freezeMessage"));
  }

  // ================================================================
  // Vanish Tab
  // ================================================================

  private void buildVanishTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "General");
    addBooleanSetting(cmd, events, "vanish.enabled", "Enabled", boolVal("vanish.enabled"));
    addBooleanSetting(cmd, events, "vanish.silentJoin", "Silent Join", boolVal("vanish.silentJoin"));
    addBooleanSetting(cmd, events, "vanish.fakeLeaveMessage", "Fake Leave Message", boolVal("vanish.fakeLeaveMessage"));
    addBooleanSetting(cmd, events, "vanish.fakeJoinMessage", "Fake Join Message", boolVal("vanish.fakeJoinMessage"));

    setColumn(false);
    addSectionHeader(cmd, "Messages");
    addStringSetting(cmd, events, "vanish.vanishEnableMessage", "Vanish Enable Message", strVal("vanish.vanishEnableMessage"));
    addStringSetting(cmd, events, "vanish.vanishDisableMessage", "Vanish Disable Message", strVal("vanish.vanishDisableMessage"));
  }

  // ================================================================
  // Utility Tab
  // ================================================================

  private void buildUtilityTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "General");
    addBooleanSetting(cmd, events, "utility.enabled", "Enabled", boolVal("utility.enabled"));

    addSectionHeader(cmd, "Values");
    addIntSetting(cmd, events, "utility.defaultNearRadius", "Default Near Radius", intVal("utility.defaultNearRadius"));
    addIntSetting(cmd, events, "utility.maxNearRadius", "Max Near Radius", intVal("utility.maxNearRadius"));
    addIntSetting(cmd, events, "utility.clearChatLines", "Clear Chat Lines", intVal("utility.clearChatLines"));
    addIntSetting(cmd, events, "utility.afkTimeoutSeconds", "AFK Timeout (sec)", intVal("utility.afkTimeoutSeconds"));
    addIntSetting(cmd, events, "utility.sleepPercentage", "Sleep Percentage", intVal("utility.sleepPercentage"));
    addStringSetting(cmd, events, "utility.discordUrl", "Discord URL", strVal("utility.discordUrl"));

    setColumn(false);
    addSectionHeader(cmd, "Commands");
    addBooleanSetting(cmd, events, "utility.clearChatEnabled", "Clear Chat", boolVal("utility.clearChatEnabled"));
    addBooleanSetting(cmd, events, "utility.clearInventoryEnabled", "Clear Inventory", boolVal("utility.clearInventoryEnabled"));
    addBooleanSetting(cmd, events, "utility.repairEnabled", "Repair", boolVal("utility.repairEnabled"));
    addBooleanSetting(cmd, events, "utility.nearEnabled", "Near", boolVal("utility.nearEnabled"));
    addBooleanSetting(cmd, events, "utility.healEnabled", "Heal", boolVal("utility.healEnabled"));
    addBooleanSetting(cmd, events, "utility.flyEnabled", "Fly", boolVal("utility.flyEnabled"));
    addBooleanSetting(cmd, events, "utility.godEnabled", "God", boolVal("utility.godEnabled"));
    addBooleanSetting(cmd, events, "utility.durabilityEnabled", "Durability", boolVal("utility.durabilityEnabled"));
    addBooleanSetting(cmd, events, "utility.motdEnabled", "MOTD", boolVal("utility.motdEnabled"));
    addBooleanSetting(cmd, events, "utility.rulesEnabled", "Rules", boolVal("utility.rulesEnabled"));
    addBooleanSetting(cmd, events, "utility.discordEnabled", "Discord", boolVal("utility.discordEnabled"));
    addBooleanSetting(cmd, events, "utility.listEnabled", "List", boolVal("utility.listEnabled"));
    addBooleanSetting(cmd, events, "utility.playtimeEnabled", "Playtime", boolVal("utility.playtimeEnabled"));
    addBooleanSetting(cmd, events, "utility.joindateEnabled", "Join Date", boolVal("utility.joindateEnabled"));
    addBooleanSetting(cmd, events, "utility.afkEnabled", "AFK", boolVal("utility.afkEnabled"));
    addBooleanSetting(cmd, events, "utility.invseeEnabled", "Invsee", boolVal("utility.invseeEnabled"));
    addBooleanSetting(cmd, events, "utility.staminaEnabled", "Stamina", boolVal("utility.staminaEnabled"));
    addBooleanSetting(cmd, events, "utility.trashEnabled", "Trash", boolVal("utility.trashEnabled"));
    addBooleanSetting(cmd, events, "utility.maxstackEnabled", "Max Stack", boolVal("utility.maxstackEnabled"));
    addBooleanSetting(cmd, events, "utility.sleepPercentageEnabled", "Sleep Percentage", boolVal("utility.sleepPercentageEnabled"));
  }

  // ================================================================
  // Announcements Tab
  // ================================================================

  private void buildAnnouncementsTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "General");
    addBooleanSetting(cmd, events, "announcements.enabled", "Enabled", boolVal("announcements.enabled"));
    addIntSetting(cmd, events, "announcements.intervalSeconds", "Interval (sec)", intVal("announcements.intervalSeconds"));
    addBooleanSetting(cmd, events, "announcements.randomize", "Randomize", boolVal("announcements.randomize"));

    setColumn(false);
    addSectionHeader(cmd, "Formatting");
    addStringSetting(cmd, events, "announcements.prefixText", "Prefix Text", strVal("announcements.prefixText"));
    addColorSetting(cmd, events, "announcements.prefixColor", "Prefix Color", colorVal("announcements.prefixColor"));
    addColorSetting(cmd, events, "announcements.messageColor", "Message Color", colorVal("announcements.messageColor"));

    addSectionHeader(cmd, "Events");
    addBooleanSetting(cmd, events, "announcements.joinMessagesEnabled", "Join Messages", boolVal("announcements.joinMessagesEnabled"));
    addBooleanSetting(cmd, events, "announcements.leaveMessagesEnabled", "Leave Messages", boolVal("announcements.leaveMessagesEnabled"));
    addBooleanSetting(cmd, events, "announcements.welcomeMessagesEnabled", "Welcome Messages", boolVal("announcements.welcomeMessagesEnabled"));
  }

  // ================================================================
  // Debug Tab
  // ================================================================

  private void buildDebugTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "General");
    addBooleanSetting(cmd, events, "debug.enabled", "Enabled", boolVal("debug.enabled"));
    addBooleanSetting(cmd, events, "debug.enabledByDefault", "Enabled by Default", boolVal("debug.enabledByDefault"));
    addBooleanSetting(cmd, events, "debug.logToConsole", "Log to Console", boolVal("debug.logToConsole"));

    addSectionHeader(cmd, "Sentry");
    addBooleanSetting(cmd, events, "debug.sentryEnabled", "Sentry Enabled", boolVal("debug.sentryEnabled"));
    addBooleanSetting(cmd, events, "debug.sentryDebug", "Sentry Debug", boolVal("debug.sentryDebug"));
    addDoubleSetting(cmd, events, "debug.sentryTracesSampleRate", "Traces Sample Rate", doubleVal("debug.sentryTracesSampleRate"));

    setColumn(false);
    addSectionHeader(cmd, "Categories");
    addBooleanSetting(cmd, events, "debug.homes", "Homes", boolVal("debug.homes"));
    addBooleanSetting(cmd, events, "debug.warps", "Warps", boolVal("debug.warps"));
    addBooleanSetting(cmd, events, "debug.spawns", "Spawns", boolVal("debug.spawns"));
    addBooleanSetting(cmd, events, "debug.teleport", "Teleport", boolVal("debug.teleport"));
    addBooleanSetting(cmd, events, "debug.kits", "Kits", boolVal("debug.kits"));
    addBooleanSetting(cmd, events, "debug.moderation", "Moderation", boolVal("debug.moderation"));
    addBooleanSetting(cmd, events, "debug.utility", "Utility", boolVal("debug.utility"));
    addBooleanSetting(cmd, events, "debug.rtp", "RTP", boolVal("debug.rtp"));
    addBooleanSetting(cmd, events, "debug.announcements", "Announcements", boolVal("debug.announcements"));
    addBooleanSetting(cmd, events, "debug.integration", "Integration", boolVal("debug.integration"));
    addBooleanSetting(cmd, events, "debug.economy", "Economy", boolVal("debug.economy"));
    addBooleanSetting(cmd, events, "debug.storage", "Storage", boolVal("debug.storage"));
  }

  // ================================================================
  // Backup Tab (narrow)
  // ================================================================

  private void buildBackupTab(UICommandBuilder cmd, UIEventBuilder events) {
    setColumn(true);
    addSectionHeader(cmd, "General");
    addBooleanSetting(cmd, events, "backup.enabled", "Backup Enabled", boolVal("backup.enabled"));
    addBooleanSetting(cmd, events, "backup.onShutdown", "Backup on Shutdown", boolVal("backup.onShutdown"));

    addSectionHeader(cmd, "Retention");
    addIntSetting(cmd, events, "backup.hourlyRetention", "Hourly Retention", intVal("backup.hourlyRetention"));
    addIntSetting(cmd, events, "backup.dailyRetention", "Daily Retention", intVal("backup.dailyRetention"));
    addIntSetting(cmd, events, "backup.weeklyRetention", "Weekly Retention", intVal("backup.weeklyRetention"));
    addIntSetting(cmd, events, "backup.manualRetention", "Manual Retention", intVal("backup.manualRetention"));
    addIntSetting(cmd, events, "backup.shutdownRetention", "Shutdown Retention", intVal("backup.shutdownRetention"));
  }

  // ================================================================
  // Setting Row Builders
  // ================================================================

  private void addSectionHeader(UICommandBuilder cmd, String label) {
    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_SECTION);
    cmd.set(containerId + "[" + getRowIdx() + "] #SectionTitle.Text", label);
    incrementRowIdx();
  }

  private void addBooleanSetting(UICommandBuilder cmd, UIEventBuilder events,
                                  String key, String label, boolean value) {
    boolean pending = pendingChanges.containsKey(key);
    boolean effectiveValue = pending ? (Boolean) pendingChanges.get(key) : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_BOOL_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #BoolToggle #CheckBox.Value", effectiveValue);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #BoolToggle #CheckBox",
        EventData.of("Button", "ToggleSetting").append("SettingKey", key), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.BOOL);
    incrementRowIdx();
  }

  private void addIntSetting(UICommandBuilder cmd, UIEventBuilder events,
                              String key, String label, int value) {
    boolean pending = pendingChanges.containsKey(key);
    int effectiveValue = pending ? ((Number) pendingChanges.get(key)).intValue() : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_NUM_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #NumInput.Value", String.valueOf(effectiveValue));
    if (pending) cmd.set(idx + " #NumInput.Style.TextColor", "#FFAA00");
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #DecBtn",
        EventData.of("Button", "DecrementSetting").append("SettingKey", key), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #IncBtn",
        EventData.of("Button", "IncrementSetting").append("SettingKey", key), false);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #NumInput",
        EventData.of("Button", "SetNumericValue").append("SettingKey", key)
            .append("@numInput", idx + " #NumInput.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.INT);
    incrementRowIdx();
  }

  private void addDoubleSetting(UICommandBuilder cmd, UIEventBuilder events,
                                 String key, String label, double value) {
    boolean pending = pendingChanges.containsKey(key);
    double effectiveValue = pending ? ((Number) pendingChanges.get(key)).doubleValue() : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_NUM_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #NumInput.Value", String.format("%.2f", effectiveValue));
    if (pending) cmd.set(idx + " #NumInput.Style.TextColor", "#FFAA00");
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #DecBtn",
        EventData.of("Button", "DecrementSetting").append("SettingKey", key), false);
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #IncBtn",
        EventData.of("Button", "IncrementSetting").append("SettingKey", key), false);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #NumInput",
        EventData.of("Button", "SetNumericValue").append("SettingKey", key)
            .append("@numInput", idx + " #NumInput.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.DOUBLE);
    incrementRowIdx();
  }

  private void addStringSetting(UICommandBuilder cmd, UIEventBuilder events,
                                 String key, String label, String value) {
    boolean pending = pendingChanges.containsKey(key);
    String effectiveValue = pending ? String.valueOf(pendingChanges.get(key)) : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_STR_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #StrInput.Value", effectiveValue);
    if (pending) cmd.set(idx + " #StrInput.Style.TextColor", "#FFAA00");
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #StrInput",
        EventData.of("Button", "SetStringValue").append("SettingKey", key)
            .append("@strInput", idx + " #StrInput.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.STRING);
    incrementRowIdx();
  }

  private void addColorSetting(UICommandBuilder cmd, UIEventBuilder events,
                                 String key, String label, String value) {
    boolean pending = pendingChanges.containsKey(key);
    String effectiveValue = pending ? String.valueOf(pendingChanges.get(key)) : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_COLOR_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #ColorPicker.Color", effectiveValue);
    cmd.set(idx + " #ColorInput.Value", effectiveValue);
    if (pending) cmd.set(idx + " #ColorInput.Style.TextColor", "#FFAA00");
    // Set button: reads the picker's current Color
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #ApplyColorBtn",
        EventData.of("Button", "SetColorValue").append("SettingKey", key)
            .append("@colorValue", idx + " #ColorPicker.Color"), false);
    // Text field: type a hex color manually (debounced)
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #ColorInput",
        EventData.of("Button", "TypeColorValue").append("SettingKey", key)
            .append("@strInput", idx + " #ColorInput.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.COLOR);
    incrementRowIdx();
  }

  private void addEnumSetting(UICommandBuilder cmd, UIEventBuilder events,
                               String key, String label, String value, String... options) {
    boolean pending = pendingChanges.containsKey(key);
    String effectiveValue = pending ? String.valueOf(pendingChanges.get(key)) : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_ENUM_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #EnumSelect.Entries",
        java.util.Arrays.stream(options)
            .map(o -> new DropdownEntryInfo(LocalizableString.fromString(o), o))
            .toList());
    cmd.set(idx + " #EnumSelect.Value", effectiveValue);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #EnumSelect",
        EventData.of("Button", "EnumChanged").append("SettingKey", key)
            .append("@enumValue", idx + " #EnumSelect.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.ENUM);
    incrementRowIdx();
  }

  /** Available locale codes — delegates to HEMessages single source of truth. */
  private static java.util.List<String> availableLocales() {
    return com.hyperessentials.util.HEMessages.getSupportedLocalesList();
  }

  private static String nativeDisplayName(String localeCode) {
    java.util.Locale locale = java.util.Locale.forLanguageTag(localeCode);
    String lang = locale.getDisplayLanguage(locale);
    if (!lang.isEmpty()) {
      lang = Character.toUpperCase(lang.charAt(0)) + lang.substring(1);
    }
    String country = locale.getCountry();
    return country.isEmpty() ? lang : lang + " (" + country + ")";
  }

  private void addLocaleSetting(UICommandBuilder cmd, UIEventBuilder events,
                                 String key, String label, String value) {
    boolean pending = pendingChanges.containsKey(key);
    String effectiveValue = pending ? String.valueOf(pendingChanges.get(key)) : value;
    if (!pending) originalValues.putIfAbsent(key, value);
    String color = pending ? "#FFAA00" : "#CCCCCC";

    String containerId = getContainerId();
    cmd.append(containerId, UIPaths.ADMIN_CONFIG_ENUM_ROW);
    String idx = containerId + "[" + getRowIdx() + "]";
    cmd.set(idx + " #SettingLabel.Text", label);
    cmd.set(idx + " #SettingLabel.Style.TextColor", color);
    cmd.set(idx + " #EnumSelect.Entries",
        availableLocales().stream()
            .map(code -> new DropdownEntryInfo(LocalizableString.fromString(nativeDisplayName(code)), code))
            .toList());
    cmd.set(idx + " #EnumSelect.Value", effectiveValue);
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, idx + " #EnumSelect",
        EventData.of("Button", "EnumChanged").append("SettingKey", key)
            .append("@enumValue", idx + " #EnumSelect.Value"), false);
    settingSelectors.put(key, idx);
    settingKinds.put(key, SettingKind.ENUM);
    incrementRowIdx();
  }

  // ================================================================
  // Event Handling
  // ================================================================

  @Override
  public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                              @NotNull AdminConfigData data) {
    super.handleDataEvent(ref, store, data);

    if ("Nav".equals(data.button)) {
      NavBarHelper.handleNavEvent(
          data.navTarget != null ? data.navTarget : "",
          player, ref, store, playerRef, guiManager, GuiType.ADMIN
      );
      return;
    }

    if (data.button == null) return;

    switch (data.button) {
      case "TabSwitch" -> {
        if (data.tab != null) {
          LayoutSize oldSize = getLayoutSize(currentTab);
          currentTab = data.tab;
          saveConfirmActive = false;
          resetConfirmActive = false;
          if (getLayoutSize(currentTab) != oldSize) {
            rebuild();
          } else {
            refresh(ref, store);
          }
        }
      }

      case "ToggleSetting" -> {
        if (data.settingKey != null) {
          handleToggle(data.settingKey);
          updateSettingAndStatus(ref, store, data.settingKey);
        }
      }

      case "IncrementSetting" -> {
        if (data.settingKey != null) {
          handleIncrement(data.settingKey, true);
          updateSettingAndStatus(ref, store, data.settingKey);
        }
      }

      case "DecrementSetting" -> {
        if (data.settingKey != null) {
          handleIncrement(data.settingKey, false);
          updateSettingAndStatus(ref, store, data.settingKey);
        }
      }

      case "SetNumericValue" -> {
        if (data.settingKey != null && data.numInput != null) {
          handleNumericInput(data.settingKey, data.numInput);
          debouncedStatusUpdate(ref, store, data.settingKey);
        }
      }

      case "SetStringValue" -> {
        if (data.settingKey != null && data.strInput != null) {
          handleStringInput(data.settingKey, data.strInput);
          debouncedStatusUpdate(ref, store, data.settingKey);
        }
      }

      case "SetColorValue" -> {
        if (data.settingKey != null && data.colorValue != null) {
          handleColorInput(data.settingKey, data.colorValue);
          updateSettingAndStatus(ref, store, data.settingKey);
        }
      }

      case "TypeColorValue" -> {
        if (data.settingKey != null && data.strInput != null) {
          handleColorInput(data.settingKey, data.strInput);
          debouncedStatusUpdate(ref, store, data.settingKey);
        }
      }

      case "EnumChanged" -> {
        if (data.settingKey != null && data.enumValue != null) {
          handleEnumInput(data.settingKey, data.enumValue);
          updateSettingAndStatus(ref, store, data.settingKey);
        }
      }

      case "Save" -> {
        if (!invalidFields.isEmpty()) {
          // Can't save with invalid fields
          break;
        }
        if (!saveConfirmActive) {
          saveConfirmActive = true;
          resetConfirmActive = false;
          refresh(ref, store);
        } else {
          applyAndSave();
          saveConfirmActive = false;
          resetConfirmActive = false;
          editSessions.remove(playerRef.getUuid());
          refresh(ref, store);
        }
      }

      case "Revert" -> {
        pendingChanges.clear();
        invalidFields.clear();
        saveConfirmActive = false;
        resetConfirmActive = false;
        editSessions.remove(playerRef.getUuid());
        refresh(ref, store);
      }

      case "ResetDefaults" -> {
        if (!resetConfirmActive) {
          resetConfirmActive = true;
          refresh(ref, store);
        } else {
          ConfigManager.get().resetAllDefaults();
          pendingChanges.clear();
          originalValues.clear();
          resetConfirmActive = false;
          editSessions.remove(playerRef.getUuid());
          refresh(ref, store);
        }
      }

      case "Reload" -> {
        ConfigManager.get().reloadAll();
        pendingChanges.clear();
        originalValues.clear();
        invalidFields.clear();
        saveConfirmActive = false;
        resetConfirmActive = false;
        editSessions.remove(playerRef.getUuid());
        refresh(ref, store);
      }

      default -> { }
    }
  }

  // ================================================================
  // Input Handlers
  // ================================================================

  private void handleToggle(String key) {
    Object orig = originalValues.get(key);
    boolean current;
    if (pendingChanges.containsKey(key)) {
      current = (Boolean) pendingChanges.get(key);
    } else if (orig instanceof Boolean b) {
      current = b;
    } else {
      return;
    }
    boolean newVal = !current;
    if (orig instanceof Boolean b && newVal == b) {
      pendingChanges.remove(key);
    } else {
      pendingChanges.put(key, newVal);
    }
  }

  private void handleIncrement(String key, boolean increment) {
    Object orig = originalValues.get(key);
    if (orig instanceof Integer origInt) {
      int step = ConfigSnapshot.getIntStep(key);
      int current = pendingChanges.containsKey(key) ? ((Number) pendingChanges.get(key)).intValue() : origInt;
      int newVal = increment ? current + step : current - step;
      newVal = Math.max(0, newVal);
      if (newVal == origInt) {
        pendingChanges.remove(key);
      } else {
        pendingChanges.put(key, newVal);
      }
    } else if (orig instanceof Double origDbl) {
      double step = ConfigSnapshot.getDoubleStep(key);
      double current = pendingChanges.containsKey(key) ? ((Number) pendingChanges.get(key)).doubleValue() : origDbl;
      double newVal = increment ? current + step : current - step;
      newVal = Math.max(0.0, newVal);
      newVal = Math.round(newVal * 100.0) / 100.0;
      if (Math.abs(newVal - origDbl) < 0.001) {
        pendingChanges.remove(key);
      } else {
        pendingChanges.put(key, newVal);
      }
    }
  }

  private void handleNumericInput(String key, String input) {
    Object orig = originalValues.get(key);
    if (input == null || input.isBlank()) {
      invalidFields.remove(key);
      return;
    }
    if (orig instanceof Integer origInt) {
      if (!isValidInt(input)) {
        invalidFields.add(key);
        return;
      }
      invalidFields.remove(key);
      int newVal = Integer.parseInt(input.trim());
      newVal = Math.max(0, newVal);
      if (newVal == origInt) pendingChanges.remove(key);
      else pendingChanges.put(key, newVal);
    } else if (orig instanceof Double origDbl) {
      if (!isValidDouble(input)) {
        invalidFields.add(key);
        return;
      }
      invalidFields.remove(key);
      double newVal = Double.parseDouble(input.trim());
      newVal = Math.max(0.0, newVal);
      newVal = Math.round(newVal * 100.0) / 100.0;
      if (Math.abs(newVal - origDbl) < 0.001) pendingChanges.remove(key);
      else pendingChanges.put(key, newVal);
    }
  }

  private static boolean isValidInt(String input) {
    try { Integer.parseInt(input.trim()); return true; }
    catch (NumberFormatException e) { return false; }
  }

  private static boolean isValidDouble(String input) {
    try {
      double v = Double.parseDouble(input.trim());
      return !Double.isNaN(v) && !Double.isInfinite(v);
    } catch (NumberFormatException e) { return false; }
  }

  private void handleStringInput(String key, String input) {
    Object orig = originalValues.get(key);
    String validated = input != null ? input : "";
    if (validated.length() > 256) validated = validated.substring(0, 256);
    if (orig instanceof String origStr && validated.equals(origStr)) {
      pendingChanges.remove(key);
    } else {
      pendingChanges.put(key, validated);
    }
  }

  private void handleColorInput(String key, String rawColor) {
    Object orig = originalValues.get(key);
    String origStr = orig instanceof String s ? s : "#FFFFFF";
    // ColorPicker returns #RRGGBBAA — strip alpha to get #RRGGBB
    String hex = rawColor != null && rawColor.length() >= 7
        ? rawColor.substring(0, 7).toUpperCase() : rawColor;
    if (hex != null && !hex.isBlank() && !hex.matches("#[0-9A-Fa-f]{6}")) {
      invalidFields.add(key);
      return;
    }
    invalidFields.remove(key);
    String validated = (hex != null && !hex.isBlank()) ? hex : origStr;
    if (validated.equals(origStr)) {
      pendingChanges.remove(key);
    } else {
      pendingChanges.put(key, validated);
    }
  }

  private void handleEnumInput(String key, String value) {
    Object orig = originalValues.get(key);
    if (orig instanceof String origStr && value.equals(origStr)) {
      pendingChanges.remove(key);
    } else {
      pendingChanges.put(key, value);
    }
  }

  // ================================================================
  // Apply and Save
  // ================================================================

  private void applyAndSave() {
    for (var entry : pendingChanges.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      ConfigSnapshot.applyChange(key, value);
    }

    ConfigManager.get().saveAll();

    for (var entry : pendingChanges.entrySet()) {
      originalValues.put(entry.getKey(), entry.getValue());
    }
    pendingChanges.clear();
    Logger.info("[ConfigEditor] Config changes saved");
  }

  // ================================================================
  // Debounced Status Update
  // ================================================================

  /**
   * Debounced status update for text input fields (numeric, string, color).
   * Updates only the label color + status bar after the user stops typing.
   */
  private void debouncedStatusUpdate(Ref<EntityStore> ref, Store<EntityStore> store, String key) {
    long stamp = System.nanoTime();
    lastTextInputNanos = stamp;
    final var capturedRef = ref;
    final var capturedStore = store;
    final var capturedKey = key;
    CompletableFuture.delayedExecutor(DEBOUNCE_MS, TimeUnit.MILLISECONDS)
        .execute(() -> {
          if (lastTextInputNanos == stamp) {
            // Only update label color + status, not the input value
            UICommandBuilder cmd = new UICommandBuilder();
            UIEventBuilder events = new UIEventBuilder();
            String selector = settingSelectors.get(capturedKey);
            if (selector != null) {
              boolean invalid = invalidFields.contains(capturedKey);
              boolean pending = pendingChanges.containsKey(capturedKey);
              String labelColor = invalid ? "#FF4444" : (pending ? "#FFAA00" : "#CCCCCC");
              cmd.set(selector + " #SettingLabel.Style.TextColor", labelColor);

              SettingKind kind = settingKinds.get(capturedKey);
              // Show red on the input field itself if invalid
              if (invalid) {
                if (kind == SettingKind.INT || kind == SettingKind.DOUBLE) {
                  cmd.set(selector + " #NumInput.Style.TextColor", "#FF4444");
                } else if (kind == SettingKind.COLOR) {
                  cmd.set(selector + " #ColorInput.Style.TextColor", "#FF4444");
                }
              }
              // Update color picker preview when typing a valid hex color
              if (kind == SettingKind.COLOR && pending && !invalid) {
                String colorVal = String.valueOf(pendingChanges.get(capturedKey));
                cmd.set(selector + " #ColorPicker.Color", colorVal);
              }
            }
            updateStatusLabel(cmd);
            sendUpdate(cmd, events, false);
          }
        });
  }

  // ================================================================
  // Refresh / Dismiss
  // ================================================================

  /** Full page refresh — used for tab switches, save, revert, reset. */
  private void refresh(Ref<EntityStore> ref, Store<EntityStore> store) {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildDynamicContent(cmd, events);
    sendUpdate(cmd, events, false);
  }

  @Override
  public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
    super.onDismiss(ref, store);
    saveSession();
  }

  // ================================================================
  // Utilities
  // ================================================================

  private static String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }
}
