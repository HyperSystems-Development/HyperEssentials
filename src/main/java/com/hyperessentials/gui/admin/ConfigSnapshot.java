package com.hyperessentials.gui.admin;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.Logger;

/**
 * Helper for the admin config editor GUI.
 *
 * <p>
 * Provides methods to apply setting changes by key to the appropriate
 * config class, using the setters on each module config.
 */
public final class ConfigSnapshot {

  private ConfigSnapshot() {}

  /**
   * The type of a config setting for UI rendering.
   */
  public enum SettingType {
    BOOLEAN,
    INT,
    DOUBLE,
    STRING,
    COLOR
  }

  /**
   * Applies a changed value to the appropriate config field.
   *
   * @param key   the dotted setting key (e.g. "core.prefixText")
   * @param value the new value
   */
  public static void applyChange(String key, Object value) {
    ConfigManager cfg = ConfigManager.get();
    try {
      switch (key) {
        // === CoreConfig ===
        case "core.prefixText" -> cfg.core().setPrefixText(toStr(value));
        case "core.prefixColor" -> cfg.core().setPrefixColor(toStr(value));
        case "core.prefixBracketColor" -> cfg.core().setPrefixBracketColor(toStr(value));
        case "core.primaryColor" -> cfg.core().setPrimaryColor(toStr(value));
        case "core.secondaryColor" -> cfg.core().setSecondaryColor(toStr(value));
        case "core.errorColor" -> cfg.core().setErrorColor(toStr(value));
        case "core.adminRequiresOp" -> cfg.core().setAdminRequiresOp(toBool(value));
        case "core.allowWithoutPermissionMod" -> cfg.core().setAllowWithoutPermissionMod(toBool(value));
        case "core.updateCheck" -> cfg.core().setUpdateCheck(toBool(value));
        case "core.defaultLanguage" -> cfg.core().setDefaultLanguage(toStr(value));
        case "core.usePlayerLanguage" -> cfg.core().setUsePlayerLanguage(toBool(value));

        // === HomesConfig ===
        case "homes.enabled" -> cfg.homes().setEnabled(toBool(value));
        case "homes.defaultHomeLimit" -> cfg.homes().setDefaultHomeLimit(toInt(value));
        case "homes.factionsEnabled" -> cfg.homes().setFactionsEnabled(toBool(value));
        case "homes.allowInOwnTerritory" -> cfg.homes().setAllowInOwnTerritory(toBool(value));
        case "homes.allowInAllyTerritory" -> cfg.homes().setAllowInAllyTerritory(toBool(value));
        case "homes.allowInNeutralTerritory" -> cfg.homes().setAllowInNeutralTerritory(toBool(value));
        case "homes.allowInEnemyTerritory" -> cfg.homes().setAllowInEnemyTerritory(toBool(value));
        case "homes.allowInWilderness" -> cfg.homes().setAllowInWilderness(toBool(value));
        case "homes.bedSyncEnabled" -> cfg.homes().setBedSyncEnabled(toBool(value));
        case "homes.bedHomeName" -> cfg.homes().setBedHomeName(toStr(value));
        case "homes.shareEnabled" -> cfg.homes().setShareEnabled(toBool(value));
        case "homes.maxSharesPerHome" -> cfg.homes().setMaxSharesPerHome(toInt(value));

        // === WarpsConfig ===
        case "warps.enabled" -> cfg.warps().setEnabled(toBool(value));
        case "warps.defaultCategory" -> cfg.warps().setDefaultCategory(toStr(value));

        // === SpawnsConfig ===
        case "spawns.enabled" -> cfg.spawns().setEnabled(toBool(value));
        case "spawns.defaultSpawnName" -> cfg.spawns().setDefaultSpawnName(toStr(value));
        case "spawns.teleportOnJoin" -> cfg.spawns().setTeleportOnJoin(toBool(value));
        case "spawns.teleportOnRespawn" -> cfg.spawns().setTeleportOnRespawn(toBool(value));
        case "spawns.perWorldSpawns" -> cfg.spawns().setPerWorldSpawns(toBool(value));

        // === TeleportConfig ===
        case "teleport.enabled" -> cfg.teleport().setEnabled(toBool(value));
        case "teleport.tpaTimeout" -> cfg.teleport().setTpaTimeout(toInt(value));
        case "teleport.tpaCooldown" -> cfg.teleport().setTpaCooldown(toInt(value));
        case "teleport.maxPendingTpa" -> cfg.teleport().setMaxPendingTpa(toInt(value));
        case "teleport.backHistorySize" -> cfg.teleport().setBackHistorySize(toInt(value));
        case "teleport.saveBackOnDeath" -> cfg.teleport().setSaveBackOnDeath(toBool(value));
        case "teleport.saveBackOnTeleport" -> cfg.teleport().setSaveBackOnTeleport(toBool(value));
        case "teleport.backAllowSelectAny" -> cfg.teleport().setBackAllowSelectAny(toBool(value));
        case "teleport.backFactionsEnabled" -> cfg.teleport().setBackFactionsEnabled(toBool(value));
        case "teleport.backAllowInOwnTerritory" -> cfg.teleport().setBackAllowInOwnTerritory(toBool(value));
        case "teleport.backAllowInAllyTerritory" -> cfg.teleport().setBackAllowInAllyTerritory(toBool(value));
        case "teleport.backAllowInNeutralTerritory" -> cfg.teleport().setBackAllowInNeutralTerritory(toBool(value));
        case "teleport.backAllowInEnemyTerritory" -> cfg.teleport().setBackAllowInEnemyTerritory(toBool(value));
        case "teleport.backAllowInWilderness" -> cfg.teleport().setBackAllowInWilderness(toBool(value));
        case "teleport.rtpCenterX" -> cfg.teleport().setRtpCenterX(toInt(value));
        case "teleport.rtpCenterZ" -> cfg.teleport().setRtpCenterZ(toInt(value));
        case "teleport.rtpMinRadius" -> cfg.teleport().setRtpMinRadius(toInt(value));
        case "teleport.rtpMaxRadius" -> cfg.teleport().setRtpMaxRadius(toInt(value));
        case "teleport.rtpMaxAttempts" -> cfg.teleport().setRtpMaxAttempts(toInt(value));
        case "teleport.rtpPlayerRelative" -> cfg.teleport().setRtpPlayerRelative(toBool(value));
        case "teleport.rtpFactionAvoidanceEnabled" -> cfg.teleport().setRtpFactionAvoidanceEnabled(toBool(value));
        case "teleport.rtpFactionAvoidanceBufferRadius" -> cfg.teleport().setRtpFactionAvoidanceBufferRadius(toInt(value));
        case "teleport.rtpSafetyAvoidWater" -> cfg.teleport().setRtpSafetyAvoidWater(toBool(value));
        case "teleport.rtpSafetyAvoidDangerousFluids" -> cfg.teleport().setRtpSafetyAvoidDangerousFluids(toBool(value));
        case "teleport.rtpSafetyMinY" -> cfg.teleport().setRtpSafetyMinY(toInt(value));
        case "teleport.rtpSafetyMaxY" -> cfg.teleport().setRtpSafetyMaxY(toInt(value));
        case "teleport.rtpSafetyAirAboveHead" -> cfg.teleport().setRtpSafetyAirAboveHead(toInt(value));

        // === WarmupConfig ===
        case "warmup.enabled" -> cfg.warmup().setEnabled(toBool(value));
        case "warmup.cancelOnMove" -> cfg.warmup().setCancelOnMove(toBool(value));
        case "warmup.cancelOnDamage" -> cfg.warmup().setCancelOnDamage(toBool(value));
        case "warmup.safeTeleport" -> cfg.warmup().setSafeTeleport(toBool(value));
        case "warmup.safeRadius" -> cfg.warmup().setSafeRadius(toInt(value));

        // === KitsConfig ===
        case "kits.enabled" -> cfg.kits().setEnabled(toBool(value));
        case "kits.defaultCooldownSeconds" -> cfg.kits().setDefaultCooldownSeconds(toInt(value));
        case "kits.oneTimeDefault" -> cfg.kits().setOneTimeDefault(toBool(value));

        // === ModerationConfig ===
        case "moderation.enabled" -> cfg.moderation().setEnabled(toBool(value));
        case "moderation.defaultBanReason" -> cfg.moderation().setDefaultBanReason(toStr(value));
        case "moderation.defaultMuteReason" -> cfg.moderation().setDefaultMuteReason(toStr(value));
        case "moderation.defaultKickReason" -> cfg.moderation().setDefaultKickReason(toStr(value));
        case "moderation.defaultWarnReason" -> cfg.moderation().setDefaultWarnReason(toStr(value));
        case "moderation.mutedChatMessage" -> cfg.moderation().setMutedChatMessage(toStr(value));
        case "moderation.freezeMessage" -> cfg.moderation().setFreezeMessage(toStr(value));
        case "moderation.freezeCheckIntervalMs" -> cfg.moderation().setFreezeCheckIntervalMs(toInt(value));
        case "moderation.broadcastBans" -> cfg.moderation().setBroadcastBans(toBool(value));
        case "moderation.broadcastKicks" -> cfg.moderation().setBroadcastKicks(toBool(value));
        case "moderation.broadcastMutes" -> cfg.moderation().setBroadcastMutes(toBool(value));
        case "moderation.broadcastWarnings" -> cfg.moderation().setBroadcastWarnings(toBool(value));
        case "moderation.maxWarningsBeforeBan" -> cfg.moderation().setMaxWarningsBeforeBan(toInt(value));
        case "moderation.maxHistoryPerPlayer" -> cfg.moderation().setMaxHistoryPerPlayer(toInt(value));

        // === VanishConfig ===
        case "vanish.enabled" -> cfg.vanish().setEnabled(toBool(value));
        case "vanish.fakeLeaveMessage" -> cfg.vanish().setFakeLeaveMessage(toBool(value));
        case "vanish.fakeJoinMessage" -> cfg.vanish().setFakeJoinMessage(toBool(value));
        case "vanish.vanishEnableMessage" -> cfg.vanish().setVanishEnableMessage(toStr(value));
        case "vanish.vanishDisableMessage" -> cfg.vanish().setVanishDisableMessage(toStr(value));
        case "vanish.silentJoin" -> cfg.vanish().setSilentJoin(toBool(value));

        // === UtilityConfig ===
        case "utility.enabled" -> cfg.utility().setEnabled(toBool(value));
        case "utility.clearChatEnabled" -> cfg.utility().setClearChatEnabled(toBool(value));
        case "utility.clearInventoryEnabled" -> cfg.utility().setClearInventoryEnabled(toBool(value));
        case "utility.repairEnabled" -> cfg.utility().setRepairEnabled(toBool(value));
        case "utility.nearEnabled" -> cfg.utility().setNearEnabled(toBool(value));
        case "utility.healEnabled" -> cfg.utility().setHealEnabled(toBool(value));
        case "utility.flyEnabled" -> cfg.utility().setFlyEnabled(toBool(value));
        case "utility.godEnabled" -> cfg.utility().setGodEnabled(toBool(value));
        case "utility.defaultNearRadius" -> cfg.utility().setDefaultNearRadius(toInt(value));
        case "utility.maxNearRadius" -> cfg.utility().setMaxNearRadius(toInt(value));
        case "utility.clearChatLines" -> cfg.utility().setClearChatLines(toInt(value));
        case "utility.durabilityEnabled" -> cfg.utility().setDurabilityEnabled(toBool(value));
        case "utility.motdEnabled" -> cfg.utility().setMotdEnabled(toBool(value));
        case "utility.rulesEnabled" -> cfg.utility().setRulesEnabled(toBool(value));
        case "utility.discordEnabled" -> cfg.utility().setDiscordEnabled(toBool(value));
        case "utility.listEnabled" -> cfg.utility().setListEnabled(toBool(value));
        case "utility.playtimeEnabled" -> cfg.utility().setPlaytimeEnabled(toBool(value));
        case "utility.joindateEnabled" -> cfg.utility().setJoindateEnabled(toBool(value));
        case "utility.afkEnabled" -> cfg.utility().setAfkEnabled(toBool(value));
        case "utility.invseeEnabled" -> cfg.utility().setInvseeEnabled(toBool(value));
        case "utility.staminaEnabled" -> cfg.utility().setStaminaEnabled(toBool(value));
        case "utility.trashEnabled" -> cfg.utility().setTrashEnabled(toBool(value));
        case "utility.maxstackEnabled" -> cfg.utility().setMaxstackEnabled(toBool(value));
        case "utility.sleepPercentageEnabled" -> cfg.utility().setSleepPercentageEnabled(toBool(value));
        case "utility.discordUrl" -> cfg.utility().setDiscordUrl(toStr(value));
        case "utility.afkTimeoutSeconds" -> cfg.utility().setAfkTimeoutSeconds(toInt(value));
        case "utility.sleepPercentage" -> cfg.utility().setSleepPercentage(toInt(value));

        // === AnnouncementsConfig ===
        case "announcements.enabled" -> cfg.announcements().setEnabled(toBool(value));
        case "announcements.intervalSeconds" -> cfg.announcements().setIntervalSeconds(toInt(value));
        case "announcements.randomize" -> cfg.announcements().setRandomize(toBool(value));
        case "announcements.prefixText" -> cfg.announcements().setPrefixText(toStr(value));
        case "announcements.prefixColor" -> cfg.announcements().setPrefixColor(toStr(value));
        case "announcements.messageColor" -> cfg.announcements().setMessageColor(toStr(value));
        case "announcements.joinMessagesEnabled" -> cfg.announcements().setJoinMessagesEnabled(toBool(value));
        case "announcements.leaveMessagesEnabled" -> cfg.announcements().setLeaveMessagesEnabled(toBool(value));
        case "announcements.welcomeMessagesEnabled" -> cfg.announcements().setWelcomeMessagesEnabled(toBool(value));

        // === DebugConfig ===
        case "debug.enabled" -> cfg.debug().setEnabled(toBool(value));
        case "debug.enabledByDefault" -> cfg.debug().setEnabledByDefault(toBool(value));
        case "debug.logToConsole" -> cfg.debug().setLogToConsole(toBool(value));
        case "debug.sentryEnabled" -> cfg.debug().setSentryEnabled(toBool(value));
        case "debug.sentryDebug" -> cfg.debug().setSentryDebug(toBool(value));
        case "debug.sentryTracesSampleRate" -> cfg.debug().setSentryTracesSampleRate(toDouble(value));
        case "debug.homes" -> cfg.debug().setHomes(toBool(value));
        case "debug.warps" -> cfg.debug().setWarps(toBool(value));
        case "debug.spawns" -> cfg.debug().setSpawns(toBool(value));
        case "debug.teleport" -> cfg.debug().setTeleport(toBool(value));
        case "debug.kits" -> cfg.debug().setKits(toBool(value));
        case "debug.moderation" -> cfg.debug().setModeration(toBool(value));
        case "debug.utility" -> cfg.debug().setUtility(toBool(value));
        case "debug.rtp" -> cfg.debug().setRtp(toBool(value));
        case "debug.announcements" -> cfg.debug().setAnnouncements(toBool(value));
        case "debug.integration" -> cfg.debug().setIntegration(toBool(value));
        case "debug.economy" -> cfg.debug().setEconomy(toBool(value));
        case "debug.storage" -> cfg.debug().setStorage(toBool(value));

        // === BackupConfig ===
        case "backup.enabled" -> cfg.backup().setEnabled(toBool(value));
        case "backup.hourlyRetention" -> cfg.backup().setHourlyRetention(toInt(value));
        case "backup.dailyRetention" -> cfg.backup().setDailyRetention(toInt(value));
        case "backup.weeklyRetention" -> cfg.backup().setWeeklyRetention(toInt(value));
        case "backup.manualRetention" -> cfg.backup().setManualRetention(toInt(value));
        case "backup.onShutdown" -> cfg.backup().setOnShutdown(toBool(value));
        case "backup.shutdownRetention" -> cfg.backup().setShutdownRetention(toInt(value));

        default -> Logger.warn("[ConfigEditor] Unknown setting key: %s", key);
      }
    } catch (Exception e) {
      ErrorHandler.report("[ConfigEditor] Failed to apply change for key '" + key + "'", e);
    }
  }

  /**
   * Returns the current value for a config key.
   *
   * @param key the setting key
   * @return the current value, or null if unknown
   */
  public static Object getValue(String key) {
    ConfigManager cfg = ConfigManager.get();
    return switch (key) {
      // CoreConfig
      case "core.prefixText" -> cfg.core().getPrefixText();
      case "core.prefixColor" -> cfg.core().getPrefixColor();
      case "core.prefixBracketColor" -> cfg.core().getPrefixBracketColor();
      case "core.primaryColor" -> cfg.core().getPrimaryColor();
      case "core.secondaryColor" -> cfg.core().getSecondaryColor();
      case "core.errorColor" -> cfg.core().getErrorColor();
      case "core.adminRequiresOp" -> cfg.core().isAdminRequiresOp();
      case "core.allowWithoutPermissionMod" -> cfg.core().isAllowWithoutPermissionMod();
      case "core.updateCheck" -> cfg.core().isUpdateCheck();
      case "core.defaultLanguage" -> cfg.core().getDefaultLanguage();
      case "core.usePlayerLanguage" -> cfg.core().isUsePlayerLanguage();
      // HomesConfig
      case "homes.enabled" -> cfg.homes().isEnabled();
      case "homes.defaultHomeLimit" -> cfg.homes().getDefaultHomeLimit();
      case "homes.factionsEnabled" -> cfg.homes().isFactionsEnabled();
      case "homes.allowInOwnTerritory" -> cfg.homes().isAllowInOwnTerritory();
      case "homes.allowInAllyTerritory" -> cfg.homes().isAllowInAllyTerritory();
      case "homes.allowInNeutralTerritory" -> cfg.homes().isAllowInNeutralTerritory();
      case "homes.allowInEnemyTerritory" -> cfg.homes().isAllowInEnemyTerritory();
      case "homes.allowInWilderness" -> cfg.homes().isAllowInWilderness();
      case "homes.bedSyncEnabled" -> cfg.homes().isBedSyncEnabled();
      case "homes.bedHomeName" -> cfg.homes().getBedHomeName();
      case "homes.shareEnabled" -> cfg.homes().isShareEnabled();
      case "homes.maxSharesPerHome" -> cfg.homes().getMaxSharesPerHome();
      // WarpsConfig
      case "warps.enabled" -> cfg.warps().isEnabled();
      case "warps.defaultCategory" -> cfg.warps().getDefaultCategory();
      // SpawnsConfig
      case "spawns.enabled" -> cfg.spawns().isEnabled();
      case "spawns.defaultSpawnName" -> cfg.spawns().getDefaultSpawnName();
      case "spawns.teleportOnJoin" -> cfg.spawns().isTeleportOnJoin();
      case "spawns.teleportOnRespawn" -> cfg.spawns().isTeleportOnRespawn();
      case "spawns.perWorldSpawns" -> cfg.spawns().isPerWorldSpawns();
      // TeleportConfig
      case "teleport.enabled" -> cfg.teleport().isEnabled();
      case "teleport.tpaTimeout" -> cfg.teleport().getTpaTimeout();
      case "teleport.tpaCooldown" -> cfg.teleport().getTpaCooldown();
      case "teleport.maxPendingTpa" -> cfg.teleport().getMaxPendingTpa();
      case "teleport.backHistorySize" -> cfg.teleport().getBackHistorySize();
      case "teleport.saveBackOnDeath" -> cfg.teleport().isSaveBackOnDeath();
      case "teleport.saveBackOnTeleport" -> cfg.teleport().isSaveBackOnTeleport();
      case "teleport.backAllowSelectAny" -> cfg.teleport().isBackAllowSelectAny();
      case "teleport.backFactionsEnabled" -> cfg.teleport().isBackFactionsEnabled();
      case "teleport.backAllowInOwnTerritory" -> cfg.teleport().isBackAllowInOwnTerritory();
      case "teleport.backAllowInAllyTerritory" -> cfg.teleport().isBackAllowInAllyTerritory();
      case "teleport.backAllowInNeutralTerritory" -> cfg.teleport().isBackAllowInNeutralTerritory();
      case "teleport.backAllowInEnemyTerritory" -> cfg.teleport().isBackAllowInEnemyTerritory();
      case "teleport.backAllowInWilderness" -> cfg.teleport().isBackAllowInWilderness();
      case "teleport.rtpCenterX" -> cfg.teleport().getRtpCenterX();
      case "teleport.rtpCenterZ" -> cfg.teleport().getRtpCenterZ();
      case "teleport.rtpMinRadius" -> cfg.teleport().getRtpMinRadius();
      case "teleport.rtpMaxRadius" -> cfg.teleport().getRtpMaxRadius();
      case "teleport.rtpMaxAttempts" -> cfg.teleport().getRtpMaxAttempts();
      case "teleport.rtpPlayerRelative" -> cfg.teleport().isRtpPlayerRelative();
      case "teleport.rtpFactionAvoidanceEnabled" -> cfg.teleport().isRtpFactionAvoidanceEnabled();
      case "teleport.rtpFactionAvoidanceBufferRadius" -> cfg.teleport().getRtpFactionAvoidanceBufferRadius();
      case "teleport.rtpSafetyAvoidWater" -> cfg.teleport().isRtpSafetyAvoidWater();
      case "teleport.rtpSafetyAvoidDangerousFluids" -> cfg.teleport().isRtpSafetyAvoidDangerousFluids();
      case "teleport.rtpSafetyMinY" -> cfg.teleport().getRtpSafetyMinY();
      case "teleport.rtpSafetyMaxY" -> cfg.teleport().getRtpSafetyMaxY();
      case "teleport.rtpSafetyAirAboveHead" -> cfg.teleport().getRtpSafetyAirAboveHead();
      // WarmupConfig
      case "warmup.enabled" -> cfg.warmup().isEnabled();
      case "warmup.cancelOnMove" -> cfg.warmup().isCancelOnMove();
      case "warmup.cancelOnDamage" -> cfg.warmup().isCancelOnDamage();
      case "warmup.safeTeleport" -> cfg.warmup().isSafeTeleport();
      case "warmup.safeRadius" -> cfg.warmup().getSafeRadius();
      // KitsConfig
      case "kits.enabled" -> cfg.kits().isEnabled();
      case "kits.defaultCooldownSeconds" -> cfg.kits().getDefaultCooldownSeconds();
      case "kits.oneTimeDefault" -> cfg.kits().isOneTimeDefault();
      // ModerationConfig
      case "moderation.enabled" -> cfg.moderation().isEnabled();
      case "moderation.broadcastBans" -> cfg.moderation().isBroadcastBans();
      case "moderation.broadcastKicks" -> cfg.moderation().isBroadcastKicks();
      case "moderation.broadcastMutes" -> cfg.moderation().isBroadcastMutes();
      case "moderation.broadcastWarnings" -> cfg.moderation().isBroadcastWarnings();
      case "moderation.freezeCheckIntervalMs" -> cfg.moderation().getFreezeCheckIntervalMs();
      case "moderation.maxWarningsBeforeBan" -> cfg.moderation().getMaxWarningsBeforeBan();
      case "moderation.maxHistoryPerPlayer" -> cfg.moderation().getMaxHistoryPerPlayer();
      case "moderation.defaultBanReason" -> cfg.moderation().getDefaultBanReason();
      case "moderation.defaultMuteReason" -> cfg.moderation().getDefaultMuteReason();
      case "moderation.defaultKickReason" -> cfg.moderation().getDefaultKickReason();
      case "moderation.defaultWarnReason" -> cfg.moderation().getDefaultWarnReason();
      case "moderation.mutedChatMessage" -> cfg.moderation().getMutedChatMessage();
      case "moderation.freezeMessage" -> cfg.moderation().getFreezeMessage();
      // VanishConfig
      case "vanish.enabled" -> cfg.vanish().isEnabled();
      case "vanish.fakeLeaveMessage" -> cfg.vanish().isFakeLeaveMessage();
      case "vanish.fakeJoinMessage" -> cfg.vanish().isFakeJoinMessage();
      case "vanish.silentJoin" -> cfg.vanish().isSilentJoin();
      case "vanish.vanishEnableMessage" -> cfg.vanish().getVanishEnableMessage();
      case "vanish.vanishDisableMessage" -> cfg.vanish().getVanishDisableMessage();
      // UtilityConfig
      case "utility.enabled" -> cfg.utility().isEnabled();
      case "utility.defaultNearRadius" -> cfg.utility().getDefaultNearRadius();
      case "utility.maxNearRadius" -> cfg.utility().getMaxNearRadius();
      case "utility.clearChatLines" -> cfg.utility().getClearChatLines();
      case "utility.afkTimeoutSeconds" -> cfg.utility().getAfkTimeoutSeconds();
      case "utility.sleepPercentage" -> cfg.utility().getSleepPercentage();
      case "utility.discordUrl" -> cfg.utility().getDiscordUrl();
      case "utility.clearChatEnabled" -> cfg.utility().isClearChatEnabled();
      case "utility.clearInventoryEnabled" -> cfg.utility().isClearInventoryEnabled();
      case "utility.repairEnabled" -> cfg.utility().isRepairEnabled();
      case "utility.nearEnabled" -> cfg.utility().isNearEnabled();
      case "utility.healEnabled" -> cfg.utility().isHealEnabled();
      case "utility.flyEnabled" -> cfg.utility().isFlyEnabled();
      case "utility.godEnabled" -> cfg.utility().isGodEnabled();
      case "utility.durabilityEnabled" -> cfg.utility().isDurabilityEnabled();
      case "utility.motdEnabled" -> cfg.utility().isMotdEnabled();
      case "utility.rulesEnabled" -> cfg.utility().isRulesEnabled();
      case "utility.discordEnabled" -> cfg.utility().isDiscordEnabled();
      case "utility.listEnabled" -> cfg.utility().isListEnabled();
      case "utility.playtimeEnabled" -> cfg.utility().isPlaytimeEnabled();
      case "utility.joindateEnabled" -> cfg.utility().isJoindateEnabled();
      case "utility.afkEnabled" -> cfg.utility().isAfkEnabled();
      case "utility.invseeEnabled" -> cfg.utility().isInvseeEnabled();
      case "utility.staminaEnabled" -> cfg.utility().isStaminaEnabled();
      case "utility.trashEnabled" -> cfg.utility().isTrashEnabled();
      case "utility.maxstackEnabled" -> cfg.utility().isMaxstackEnabled();
      case "utility.sleepPercentageEnabled" -> cfg.utility().isSleepPercentageEnabled();
      // AnnouncementsConfig
      case "announcements.enabled" -> cfg.announcements().isEnabled();
      case "announcements.intervalSeconds" -> cfg.announcements().getIntervalSeconds();
      case "announcements.randomize" -> cfg.announcements().isRandomize();
      case "announcements.prefixText" -> cfg.announcements().getPrefixText();
      case "announcements.prefixColor" -> cfg.announcements().getPrefixColor();
      case "announcements.messageColor" -> cfg.announcements().getMessageColor();
      case "announcements.joinMessagesEnabled" -> cfg.announcements().isJoinMessagesEnabled();
      case "announcements.leaveMessagesEnabled" -> cfg.announcements().isLeaveMessagesEnabled();
      case "announcements.welcomeMessagesEnabled" -> cfg.announcements().isWelcomeMessagesEnabled();
      // DebugConfig
      case "debug.enabled" -> cfg.debug().isEnabled();
      case "debug.enabledByDefault" -> cfg.debug().isEnabledByDefault();
      case "debug.logToConsole" -> cfg.debug().isLogToConsole();
      case "debug.sentryEnabled" -> cfg.debug().isSentryEnabled();
      case "debug.sentryDebug" -> cfg.debug().isSentryDebug();
      case "debug.sentryTracesSampleRate" -> cfg.debug().getSentryTracesSampleRate();
      case "debug.homes" -> cfg.debug().isHomes();
      case "debug.warps" -> cfg.debug().isWarps();
      case "debug.spawns" -> cfg.debug().isSpawns();
      case "debug.teleport" -> cfg.debug().isTeleport();
      case "debug.kits" -> cfg.debug().isKits();
      case "debug.moderation" -> cfg.debug().isModeration();
      case "debug.utility" -> cfg.debug().isUtility();
      case "debug.rtp" -> cfg.debug().isRtp();
      case "debug.announcements" -> cfg.debug().isAnnouncements();
      case "debug.integration" -> cfg.debug().isIntegration();
      case "debug.economy" -> cfg.debug().isEconomy();
      case "debug.storage" -> cfg.debug().isStorage();
      // BackupConfig
      case "backup.enabled" -> cfg.backup().isEnabled();
      case "backup.hourlyRetention" -> cfg.backup().getHourlyRetention();
      case "backup.dailyRetention" -> cfg.backup().getDailyRetention();
      case "backup.weeklyRetention" -> cfg.backup().getWeeklyRetention();
      case "backup.manualRetention" -> cfg.backup().getManualRetention();
      case "backup.onShutdown" -> cfg.backup().isOnShutdown();
      case "backup.shutdownRetention" -> cfg.backup().getShutdownRetention();
      default -> null;
    };
  }

  /**
   * Returns the setting type for a config key.
   *
   * @param key the setting key
   * @return the setting type
   */
  public static SettingType getSettingType(String key) {
    Object value = getValue(key);
    if (value instanceof Boolean) return SettingType.BOOLEAN;
    if (value instanceof Integer) return SettingType.INT;
    if (value instanceof Double) return SettingType.DOUBLE;
    // Color detection for hex string values
    if (value instanceof String s && s.startsWith("#") && s.length() == 7) return SettingType.COLOR;
    if (value instanceof String) return SettingType.STRING;
    return SettingType.STRING;
  }

  /**
   * Returns the step size for an integer setting key.
   *
   * @param key the setting key
   * @return the step size
   */
  public static int getIntStep(String key) {
    return switch (key) {
      case "teleport.tpaCooldown" -> 10;
      case "teleport.rtpMaxRadius" -> 500;
      case "teleport.rtpMinRadius" -> 50;
      case "utility.defaultNearRadius", "utility.maxNearRadius" -> 50;
      case "utility.clearChatLines" -> 10;
      case "utility.afkTimeoutSeconds" -> 30;
      case "kits.defaultCooldownSeconds" -> 30;
      case "moderation.maxHistoryPerPlayer" -> 10;
      case "announcements.intervalSeconds" -> 30;
      default -> 1;
    };
  }

  /**
   * Returns the step size for a double setting key.
   *
   * @param key the setting key
   * @return the step size
   */
  public static double getDoubleStep(String key) {
    return switch (key) {
      case "debug.sentryTracesSampleRate" -> 0.1;
      default -> 1.0;
    };
  }

  /**
   * Returns a human-readable display name for a config key.
   * Converts "backAllowInOwnTerritory" → "Back Allow In Own Territory".
   */
  public static String getSettingDisplayName(String key) {
    String name = key.contains(".") ? key.substring(key.indexOf('.') + 1) : key;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      if (i > 0 && Character.isUpperCase(c)) {
        sb.append(' ');
      }
      sb.append(i == 0 ? Character.toUpperCase(c) : c);
    }
    return sb.toString();
  }

  /**
   * Returns the minimum valid value for an integer setting.
   */
  public static int getIntMin(String key) {
    return 0;
  }

  /**
   * Returns the maximum valid value for an integer setting.
   */
  public static int getIntMax(String key) {
    return switch (key) {
      case "teleport.rtpMaxRadius" -> 100000;
      case "teleport.rtpMaxAttempts" -> 100;
      case "utility.maxNearRadius" -> 5000;
      case "utility.clearChatLines" -> 1000;
      case "moderation.maxHistoryPerPlayer" -> 1000;
      default -> Integer.MAX_VALUE;
    };
  }

  /**
   * Returns the minimum valid value for a double setting.
   */
  public static double getDoubleMin(String key) {
    return 0.0;
  }

  /**
   * Returns the maximum valid value for a double setting.
   */
  public static double getDoubleMax(String key) {
    return switch (key) {
      case "debug.sentryTracesSampleRate" -> 1.0;
      default -> Double.MAX_VALUE;
    };
  }

  private static boolean toBool(Object value) {
    if (value instanceof Boolean b) return b;
    return Boolean.parseBoolean(String.valueOf(value));
  }

  private static int toInt(Object value) {
    if (value instanceof Number n) return n.intValue();
    return Integer.parseInt(String.valueOf(value));
  }

  private static double toDouble(Object value) {
    if (value instanceof Number n) return n.doubleValue();
    return Double.parseDouble(String.valueOf(value));
  }

  private static String toStr(Object value) {
    return String.valueOf(value);
  }
}
