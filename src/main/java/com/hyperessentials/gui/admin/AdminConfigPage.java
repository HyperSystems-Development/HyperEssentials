package com.hyperessentials.gui.admin;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.HEMessages;
import com.hyperessentials.util.Logger;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin Config Editor page - edit all HyperEssentials config fields by section.
 */
public class AdminConfigPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;

  private String currentSection = "core";
  private String statusMessage = "";

  /** Ordered map of section -> list of config keys. */
  private static final Map<String, List<String>> SECTIONS = new LinkedHashMap<>();

  static {
    SECTIONS.put("core", List.of(
        "core.prefixText", "core.prefixColor", "core.prefixBracketColor",
        "core.primaryColor", "core.secondaryColor", "core.errorColor",
        "core.adminRequiresOp", "core.allowWithoutPermissionMod",
        "core.updateCheck", "core.defaultLanguage", "core.usePlayerLanguage"
    ));
    SECTIONS.put("homes", List.of(
        "homes.enabled", "homes.defaultHomeLimit",
        "homes.factionsEnabled", "homes.allowInOwnTerritory", "homes.allowInAllyTerritory",
        "homes.allowInNeutralTerritory", "homes.allowInEnemyTerritory", "homes.allowInWilderness",
        "homes.bedSyncEnabled", "homes.bedHomeName",
        "homes.shareEnabled", "homes.maxSharesPerHome"
    ));
    SECTIONS.put("warps", List.of(
        "warps.enabled", "warps.defaultCategory"
    ));
    SECTIONS.put("spawns", List.of(
        "spawns.enabled", "spawns.defaultSpawnName",
        "spawns.teleportOnJoin", "spawns.teleportOnRespawn", "spawns.perWorldSpawns"
    ));
    SECTIONS.put("teleport", List.of(
        "teleport.enabled", "teleport.tpaTimeout", "teleport.tpaCooldown",
        "teleport.maxPendingTpa", "teleport.backHistorySize",
        "teleport.saveBackOnDeath", "teleport.saveBackOnTeleport", "teleport.backAllowSelectAny",
        "teleport.backFactionsEnabled",
        "teleport.backAllowInOwnTerritory", "teleport.backAllowInAllyTerritory",
        "teleport.backAllowInNeutralTerritory", "teleport.backAllowInEnemyTerritory",
        "teleport.backAllowInWilderness",
        "teleport.rtpCenterX", "teleport.rtpCenterZ",
        "teleport.rtpMinRadius", "teleport.rtpMaxRadius", "teleport.rtpMaxAttempts",
        "teleport.rtpPlayerRelative",
        "teleport.rtpFactionAvoidanceEnabled", "teleport.rtpFactionAvoidanceBufferRadius",
        "teleport.rtpSafetyAvoidWater", "teleport.rtpSafetyAvoidDangerousFluids",
        "teleport.rtpSafetyMinY", "teleport.rtpSafetyMaxY", "teleport.rtpSafetyAirAboveHead"
    ));
    SECTIONS.put("warmup", List.of(
        "warmup.enabled", "warmup.cancelOnMove", "warmup.cancelOnDamage",
        "warmup.safeTeleport", "warmup.safeRadius"
    ));
    SECTIONS.put("kits", List.of(
        "kits.enabled", "kits.defaultCooldownSeconds", "kits.oneTimeDefault"
    ));
    SECTIONS.put("moderation", List.of(
        "moderation.enabled",
        "moderation.broadcastBans", "moderation.broadcastKicks",
        "moderation.broadcastMutes", "moderation.broadcastWarnings",
        "moderation.maxWarningsBeforeBan", "moderation.maxHistoryPerPlayer",
        "moderation.freezeCheckIntervalMs",
        "moderation.defaultBanReason", "moderation.defaultMuteReason",
        "moderation.defaultKickReason", "moderation.defaultWarnReason",
        "moderation.mutedChatMessage", "moderation.freezeMessage"
    ));
    SECTIONS.put("vanish", List.of(
        "vanish.enabled", "vanish.fakeLeaveMessage", "vanish.fakeJoinMessage",
        "vanish.silentJoin",
        "vanish.vanishEnableMessage", "vanish.vanishDisableMessage"
    ));
    SECTIONS.put("utility", List.of(
        "utility.enabled",
        "utility.clearChatEnabled", "utility.clearInventoryEnabled",
        "utility.repairEnabled", "utility.nearEnabled", "utility.healEnabled",
        "utility.flyEnabled", "utility.godEnabled", "utility.durabilityEnabled",
        "utility.motdEnabled", "utility.rulesEnabled", "utility.discordEnabled",
        "utility.listEnabled", "utility.playtimeEnabled", "utility.joindateEnabled",
        "utility.afkEnabled", "utility.invseeEnabled", "utility.staminaEnabled",
        "utility.trashEnabled", "utility.maxstackEnabled", "utility.sleepPercentageEnabled",
        "utility.defaultNearRadius", "utility.maxNearRadius", "utility.clearChatLines",
        "utility.afkTimeoutSeconds", "utility.sleepPercentage",
        "utility.discordUrl"
    ));
    SECTIONS.put("announcements", List.of(
        "announcements.enabled", "announcements.intervalSeconds",
        "announcements.randomize",
        "announcements.prefixText", "announcements.prefixColor", "announcements.messageColor",
        "announcements.joinMessagesEnabled", "announcements.leaveMessagesEnabled",
        "announcements.welcomeMessagesEnabled"
    ));
    SECTIONS.put("debug", List.of(
        "debug.enabled", "debug.enabledByDefault", "debug.logToConsole",
        "debug.sentryEnabled", "debug.sentryDebug", "debug.sentryTracesSampleRate",
        "debug.homes", "debug.warps", "debug.spawns", "debug.teleport",
        "debug.kits", "debug.moderation", "debug.utility", "debug.rtp",
        "debug.announcements", "debug.integration", "debug.economy", "debug.storage"
    ));
    SECTIONS.put("backup", List.of(
        "backup.enabled",
        "backup.hourlyRetention", "backup.dailyRetention", "backup.weeklyRetention",
        "backup.manualRetention", "backup.onShutdown", "backup.shutdownRetention"
    ));
  }

  public AdminConfigPage(
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
    cmd.append(UIPaths.ADMIN_CONFIG);
    NavBarHelper.setupAdminBar(playerRef, "config", guiManager.getAdminRegistry(), cmd, events);
    buildContent(cmd, events);
  }

  private void buildContent(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    // Section tabs
    cmd.clear("#SectionTabs");
    cmd.appendInline("#SectionTabs", "Group #TabCards { LayoutMode: Left; }");
    int tabIdx = 0;
    for (String section : SECTIONS.keySet()) {
      String label = capitalize(section);
      cmd.appendInline("#TabCards", "TextButton { Text: \"" + label + "\"; "
          + "Anchor: (Width: 70, Height: 24); }");
      String color = section.equals(currentSection) ? "#FFAA00" : "#888888";
      cmd.set("#TabCards[" + tabIdx + "].Style.Default.LabelStyle.TextColor", color);

      events.addEventBinding(CustomUIEventBindingType.Activating, "#TabCards[" + tabIdx + "]",
          EventData.of("Button", "Section").append("Target", section), false);
      tabIdx++;
    }

    // Section label
    cmd.set("#SectionLabel.Text", capitalize(currentSection) + " Settings");

    // Status
    cmd.set("#StatusLabel.Text", statusMessage);

    // Config fields
    List<String> keys = SECTIONS.getOrDefault(currentSection, List.of());
    cmd.clear("#ConfigList");
    cmd.appendInline("#ConfigList", "Group #FieldCards { LayoutMode: Top; }");

    for (int i = 0; i < keys.size(); i++) {
      String key = keys.get(i);
      Object currentValue = ConfigSnapshot.getValue(key);
      ConfigSnapshot.SettingType type = ConfigSnapshot.getSettingType(key);

      // Display name is the part after the dot
      String displayKey = key.contains(".") ? key.substring(key.indexOf('.') + 1) : key;

      cmd.append("#FieldCards", UIPaths.ADMIN_CONFIG_ENTRY);
      String idx = "#FieldCards[" + i + "]";

      cmd.set(idx + " #FieldLabel.Text", displayKey);

      if (type == ConfigSnapshot.SettingType.BOOLEAN) {
        // Boolean toggle button
        boolean boolVal = currentValue instanceof Boolean b && b;
        cmd.set(idx + " #ToggleBtn.Visible", true);
        cmd.set(idx + " #ToggleBtn.Text", HEMessages.get(playerRef, boolVal ? AdminKeys.Common.ON : AdminKeys.Common.OFF));
        cmd.set(idx + " #ValueInput.Visible", false);

        events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #ToggleBtn",
            EventData.of("Button", "ToggleConfig").append("Target", key)
                .append("Value", String.valueOf(!boolVal)), false);
      } else {
        // Text/number input
        cmd.set(idx + " #ToggleBtn.Visible", false);
        cmd.set(idx + " #ValueInput.Visible", true);

        String displayValue = currentValue != null ? String.valueOf(currentValue) : "";
        cmd.set(idx + " #ValueInput.Value", displayValue);

        events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #SaveFieldBtn",
            EventData.of("Button", "SaveField").append("Target", key)
                .append("@ConfigValue", idx + " #ValueInput.Value"), false);
      }
    }

    // Save All button
    events.addEventBinding(CustomUIEventBindingType.Activating, "#SaveAllBtn",
        EventData.of("Button", "SaveAll"), false);
  }

  private String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  private void rebuildContent() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildContent(cmd, events);
    sendUpdate(cmd, events, false);
  }

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
      return;
    }

    switch (data.button) {
      case "Section" -> {
        if (data.target != null && SECTIONS.containsKey(data.target)) {
          currentSection = data.target;
          statusMessage = "";
          rebuildContent();
        }
      }
      case "ToggleConfig" -> {
        if (data.target != null && data.value != null) {
          ConfigSnapshot.applyChange(data.target, data.value);
          statusMessage = "Changed: " + data.target;
          rebuildContent();
        }
      }
      case "SaveField" -> {
        if (data.target != null && data.configValue != null) {
          ConfigSnapshot.applyChange(data.target, data.configValue);
          statusMessage = "Changed: " + data.target;
          rebuildContent();
        }
      }
      case "SaveAll" -> {
        ConfigManager.get().saveAll();
        statusMessage = "Configuration saved to disk.";
        Logger.info("[Admin] Config saved by %s", playerRef.getUsername());
        rebuildContent();
      }
      default -> sendUpdate();
    }
  }
}
