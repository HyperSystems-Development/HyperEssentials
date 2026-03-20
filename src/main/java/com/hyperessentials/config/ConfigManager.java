package com.hyperessentials.config;

import com.hyperessentials.config.modules.*;
import com.hyperessentials.migration.MigrationRunner;
import com.hyperessentials.migration.MigrationType;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Central manager for all HyperEssentials configuration.
 */
public class ConfigManager {

  private static ConfigManager instance;

  private Path dataDir;
  private CoreConfig coreConfig;
  private HomesConfig homesConfig;
  private WarpsConfig warpsConfig;
  private SpawnsConfig spawnsConfig;
  private TeleportConfig teleportConfig;
  private WarmupConfig warmupConfig;
  private KitsConfig kitsConfig;
  private ModerationConfig moderationConfig;
  private VanishConfig vanishConfig;
  private UtilityConfig utilityConfig;
  private AnnouncementsConfig announcementsConfig;
  private DebugConfig debugConfig;
  private BackupConfig backupConfig;

  private ConfigManager() {}

  @NotNull
  public static ConfigManager get() {
    if (instance == null) {
      instance = new ConfigManager();
    }
    return instance;
  }

  public void loadAll(@NotNull Path dataDir) {
    this.dataDir = dataDir;
    Logger.info("[Config] Loading configuration from: %s", dataDir.toAbsolutePath());

    // Run pending config migrations before loading
    MigrationRunner.runPendingMigrations(dataDir, MigrationType.CONFIG);

    coreConfig = new CoreConfig(dataDir.resolve("config.json"));
    coreConfig.load();

    Path configDir = dataDir.resolve("config");

    homesConfig = new HomesConfig(configDir.resolve("homes.json"));
    homesConfig.load();

    warpsConfig = new WarpsConfig(configDir.resolve("warps.json"));
    warpsConfig.load();

    spawnsConfig = new SpawnsConfig(configDir.resolve("spawns.json"));
    spawnsConfig.load();

    teleportConfig = new TeleportConfig(configDir.resolve("teleport.json"));
    teleportConfig.load();

    warmupConfig = new WarmupConfig(configDir.resolve("warmup.json"));
    warmupConfig.load();

    kitsConfig = new KitsConfig(configDir.resolve("kits.json"));
    kitsConfig.load();

    moderationConfig = new ModerationConfig(configDir.resolve("moderation.json"));
    moderationConfig.load();

    vanishConfig = new VanishConfig(configDir.resolve("vanish.json"));
    vanishConfig.load();

    utilityConfig = new UtilityConfig(configDir.resolve("utility.json"));
    utilityConfig.load();

    announcementsConfig = new AnnouncementsConfig(configDir.resolve("announcements.json"));
    announcementsConfig.load();

    debugConfig = new DebugConfig(configDir.resolve("debug.json"));
    debugConfig.load();

    backupConfig = new BackupConfig(configDir.resolve("backup.json"));
    backupConfig.load();

    validateAll();
    Logger.info("[Config] Configuration loaded successfully");
  }

  private void validateAll() {
    ValidationResult combined = new ValidationResult();
    validateAndMerge(combined, coreConfig);
    validateAndMerge(combined, homesConfig);
    validateAndMerge(combined, warpsConfig);
    validateAndMerge(combined, spawnsConfig);
    validateAndMerge(combined, teleportConfig);
    validateAndMerge(combined, warmupConfig);

    if (combined.hasIssues()) {
      Logger.info("[Config] Validation complete: %d warning(s), %d error(s)",
        combined.getWarnings().size(), combined.getErrors().size());
    }
  }

  private void validateAndMerge(@NotNull ValidationResult combined, @NotNull ConfigFile config) {
    config.validateAndLog();
    if (config.getLastValidationResult() != null) {
      combined.merge(config.getLastValidationResult());
    }
  }

  public void reloadAll() {
    Logger.info("[Config] Reloading configuration...");
    coreConfig.reload();
    homesConfig.reload();
    warpsConfig.reload();
    spawnsConfig.reload();
    teleportConfig.reload();
    warmupConfig.reload();
    kitsConfig.reload();
    moderationConfig.reload();
    vanishConfig.reload();
    utilityConfig.reload();
    announcementsConfig.reload();
    debugConfig.reload();
    backupConfig.reload();
    validateAll();
    Logger.info("[Config] Configuration reloaded");
  }

  public void saveAll() {
    coreConfig.save();
    homesConfig.save();
    warpsConfig.save();
    spawnsConfig.save();
    teleportConfig.save();
    warmupConfig.save();
    kitsConfig.save();
    moderationConfig.save();
    vanishConfig.save();
    utilityConfig.save();
    announcementsConfig.save();
    debugConfig.save();
    backupConfig.save();
  }

  @NotNull public CoreConfig core() { return coreConfig; }
  @NotNull public HomesConfig homes() { return homesConfig; }
  @NotNull public WarpsConfig warps() { return warpsConfig; }
  @NotNull public SpawnsConfig spawns() { return spawnsConfig; }
  @NotNull public TeleportConfig teleport() { return teleportConfig; }
  @NotNull public WarmupConfig warmup() { return warmupConfig; }
  @NotNull public KitsConfig kits() { return kitsConfig; }
  @NotNull public ModerationConfig moderation() { return moderationConfig; }
  @NotNull public VanishConfig vanish() { return vanishConfig; }
  @NotNull public UtilityConfig utility() { return utilityConfig; }
  @NotNull public AnnouncementsConfig announcements() { return announcementsConfig; }
  @NotNull public DebugConfig debug() { return debugConfig; }
  @NotNull public BackupConfig backup() { return backupConfig; }

  @SuppressWarnings("unchecked")
  public <T extends ModuleConfig> T getModule(@NotNull Class<T> clazz) {
    if (clazz == HomesConfig.class) return (T) homesConfig;
    if (clazz == WarpsConfig.class) return (T) warpsConfig;
    if (clazz == SpawnsConfig.class) return (T) spawnsConfig;
    if (clazz == TeleportConfig.class) return (T) teleportConfig;
    if (clazz == WarmupConfig.class) return (T) warmupConfig;
    if (clazz == KitsConfig.class) return (T) kitsConfig;
    if (clazz == ModerationConfig.class) return (T) moderationConfig;
    if (clazz == VanishConfig.class) return (T) vanishConfig;
    if (clazz == UtilityConfig.class) return (T) utilityConfig;
    if (clazz == AnnouncementsConfig.class) return (T) announcementsConfig;
    throw new IllegalArgumentException("Unknown module config: " + clazz.getName());
  }

  public boolean isModuleEnabled(@NotNull String moduleName) {
    return switch (moduleName) {
      case "homes" -> homesConfig.isEnabled();
      case "warps" -> warpsConfig.isEnabled();
      case "spawns" -> spawnsConfig.isEnabled();
      case "teleport" -> teleportConfig.isEnabled();
      case "warmup" -> warmupConfig.isEnabled();
      case "kits" -> kitsConfig.isEnabled();
      case "moderation" -> moderationConfig.isEnabled();
      case "vanish" -> vanishConfig.isEnabled();
      case "utility" -> utilityConfig.isEnabled();
      case "announcements" -> announcementsConfig.isEnabled();
      default -> false;
    };
  }
}
