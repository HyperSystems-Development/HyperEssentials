package com.hyperessentials;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.PageRegistry;
import com.hyperessentials.gui.GuiUpdateService;
import com.hyperessentials.gui.admin.AdminAnnouncementsPage;
import com.hyperessentials.gui.admin.AdminDashboardPage;
import com.hyperessentials.gui.admin.AdminKitsPage;
import com.hyperessentials.gui.admin.AdminModerationPage;
import com.hyperessentials.gui.admin.AdminPlayersPage;
import com.hyperessentials.gui.admin.AdminSettingsPage;
import com.hyperessentials.gui.admin.AdminSpawnsPage;
import com.hyperessentials.gui.admin.AdminWarpsPage;
import com.hyperessentials.gui.player.HomesPage;
import com.hyperessentials.gui.player.KitsPage;
import com.hyperessentials.gui.player.PlayerDashboardPage;
import com.hyperessentials.gui.player.StatsPage;
import com.hyperessentials.gui.player.TpaPage;
import com.hyperessentials.gui.player.WarpsPage;
import com.hyperessentials.integration.EcotaleIntegration;
import com.hyperessentials.integration.HyperFactionsIntegration;
import com.hyperessentials.integration.PermissionManager;
import com.hyperessentials.integration.SentryIntegration;
import com.hyperessentials.integration.WerchatIntegration;
import com.hyperessentials.integration.economy.VaultEconomyProvider;
import com.hyperessentials.module.ModuleRegistry;
import com.hyperessentials.module.announcements.AnnouncementsModule;
import com.hyperessentials.module.homes.HomesModule;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.module.spawns.SpawnsModule;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.module.utility.UtilityManager;
import com.hyperessentials.module.utility.UtilityModule;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.module.warmup.WarmupModule;
import com.hyperessentials.module.warps.WarpsModule;
import com.hyperessentials.storage.StorageProvider;
import com.hyperessentials.storage.json.JsonStorageProvider;
import com.hyperessentials.util.HEMessages;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.logger.HytaleLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Core singleton for HyperEssentials.
 * Manages all subsystems: config, permissions, modules, GUI, storage.
 */
public class HyperEssentials {

  private final Path dataDir;
  private final HytaleLogger hytaleLogger;

  private ConfigManager configManager;
  private ModuleRegistry moduleRegistry;
  private GuiManager guiManager;
  private WarmupManager warmupManager;
  private StorageProvider storageProvider;
  private VaultEconomyProvider vaultEconomy;
  private GuiUpdateService guiUpdateService;
  private final CopyOnWriteArrayList<Consumer<UUID>> disconnectHandlers = new CopyOnWriteArrayList<>();
  private final CopyOnWriteArrayList<BiConsumer<UUID, String>> connectHandlers = new CopyOnWriteArrayList<>();

  public HyperEssentials(@NotNull Path dataDir, @NotNull HytaleLogger hytaleLogger) {
    this.dataDir = dataDir;
    this.hytaleLogger = hytaleLogger;
  }

  /**
   * Enables HyperEssentials - loads config, initializes integrations, registers modules.
   */
  public void enable() {
    // Initialize logger
    Logger.init(hytaleLogger);

    // Load configuration
    configManager = ConfigManager.get();
    configManager.loadAll(dataDir);

    // Apply debug config to logger
    configManager.debug().applyToLogger();

    // Initialize Sentry error tracking
    SentryIntegration.init(configManager.debug());

    // Initialize i18n message system
    Logger.debug("[i18n] Initializing HEMessages with default language: %s", configManager.core().getDefaultLanguage());

    // Ensure standard directory structure
    ensureDirectories();

    // Initialize integrations
    PermissionManager.get().init();
    HyperFactionsIntegration.init();
    EcotaleIntegration.init();
    WerchatIntegration.init();

    // Initialize VaultUnlocked economy
    vaultEconomy = new VaultEconomyProvider();
    vaultEconomy.init();

    // Initialize storage
    storageProvider = new JsonStorageProvider(dataDir);
    storageProvider.init().join();

    // Initialize GUI
    guiManager = new GuiManager();
    registerDisconnectHandler(uuid -> guiManager.getPageTracker().unregister(uuid));

    // Wire i18n language override cleanup on disconnect
    registerDisconnectHandler(HEMessages::clearLanguageOverride);

    // Wire i18n language preference loading on connect
    registerConnectHandler((uuid, username) -> {
      storageProvider.getPlayerDataStorage().loadPlayerData(uuid).thenAccept(opt -> {
        opt.ifPresent(data -> {
          if (data.getLanguagePreference() != null) {
            HEMessages.setLanguageOverride(uuid, data.getLanguagePreference());
          }
        });
      });
    });

    // Initialize warmup manager
    warmupManager = new WarmupManager();

    // Register modules (warmup first, then feature modules)
    moduleRegistry = new ModuleRegistry();
    moduleRegistry.register(new WarmupModule());
    moduleRegistry.register(new HomesModule());
    moduleRegistry.register(new WarpsModule());
    moduleRegistry.register(new SpawnsModule());
    moduleRegistry.register(new TeleportModule());
    moduleRegistry.register(new KitsModule());
    moduleRegistry.register(new ModerationModule());
    moduleRegistry.register(new UtilityModule());
    moduleRegistry.register(new AnnouncementsModule());

    // Enable modules based on config
    moduleRegistry.enableAll();

    // Initialize module managers with storage (post-enable)
    initModuleManagers();

    // Register GUI pages (post-init, modules + managers must be ready)
    registerPages();

    Logger.info("HyperEssentials enabled with %d modules", moduleRegistry.getEnabledModules().size());
  }

  /**
   * Disables HyperEssentials - disables modules, shuts down storage, saves config.
   */
  public void disable() {
    // Disable modules in reverse order
    if (moduleRegistry != null) {
      moduleRegistry.disableAll();
    }

    // Shutdown storage
    if (storageProvider != null) {
      storageProvider.shutdown().join();
    }

    // Shutdown GUI update service
    if (guiUpdateService != null) {
      guiUpdateService.shutdown();
    }

    // Shutdown GUI
    if (guiManager != null) {
      guiManager.shutdown();
    }

    // Shutdown warmup scheduler
    if (warmupManager != null) {
      warmupManager.shutdown();
    }

    // Save config
    if (configManager != null) {
      configManager.saveAll();
    }

    // Close Sentry (flushes pending events)
    SentryIntegration.close();

    Logger.info("HyperEssentials disabled");
  }

  /**
   * Reloads configuration.
   */
  public void reloadConfig() {
    ConfigManager.get().reloadAll();
    Logger.info("Configuration reloaded");
  }

  /**
   * Initializes module managers with storage after modules are enabled.
   */
  private void initModuleManagers() {
    HomesModule homes = getHomesModule();
    if (homes != null && homes.isEnabled()) {
      homes.initManager(storageProvider.getHomeStorage());
    }

    WarpsModule warps = getWarpsModule();
    if (warps != null && warps.isEnabled()) {
      warps.initManager(storageProvider.getWarpStorage());
    }

    SpawnsModule spawns = getSpawnsModule();
    if (spawns != null && spawns.isEnabled()) {
      spawns.initManager(storageProvider.getSpawnStorage());
    }

    TeleportModule teleport = getTeleportModule();
    if (teleport != null && teleport.isEnabled()) {
      teleport.initManagers(storageProvider.getPlayerDataStorage());
    }

    // Wire TpaManager reference to UtilityManager and ModerationManager
    TpaManager tpaMgr = (teleport != null && teleport.isEnabled()) ? teleport.getTpaManager() : null;

    UtilityModule utilityModule = moduleRegistry.getModule(UtilityModule.class);
    if (utilityModule != null && utilityModule.isEnabled() && utilityModule.getUtilityManager() != null) {
      utilityModule.getUtilityManager().setTpaManager(tpaMgr);
      utilityModule.getUtilityManager().setPlayerDataStorage(storageProvider.getPlayerDataStorage());
    }

    ModerationModule modModule = moduleRegistry.getModule(ModerationModule.class);
    if (modModule != null && modModule.isEnabled() && modModule.getModerationManager() != null) {
      modModule.getModerationManager().setTpaManager(tpaMgr);
    }
  }

  /**
   * Registers GUI pages for enabled modules.
   */
  private void registerPages() {
    PageRegistry playerReg = guiManager.getPlayerRegistry();

    // Homes page
    HomesModule homes = getHomesModule();
    if (homes != null && homes.isEnabled() && homes.getHomeManager() != null) {
      playerReg.registerEntry(new PageRegistry.Entry(
          "homes", "Homes", "homes", Permissions.HOME_LIST,
          (player, ref, store, playerRef, gm) ->
              new HomesPage(player, playerRef, homes.getHomeManager(), warmupManager, gm),
          true, 10
      ));
    }

    // Warps page
    WarpsModule warps = getWarpsModule();
    if (warps != null && warps.isEnabled() && warps.getWarpManager() != null) {
      playerReg.registerEntry(new PageRegistry.Entry(
          "warps", "Warps", "warps", Permissions.WARP_LIST,
          (player, ref, store, playerRef, gm) ->
              new WarpsPage(player, playerRef, warps.getWarpManager(), warmupManager, gm),
          true, 20
      ));
    }

    // Kits page
    KitsModule kitsModule = moduleRegistry.getModule(KitsModule.class);
    if (kitsModule != null && kitsModule.isEnabled()) {
      playerReg.registerEntry(new PageRegistry.Entry(
          "kits", "Kits", "kits", Permissions.KIT_LIST,
          (player, ref, store, playerRef, gm) ->
              new KitsPage(player, playerRef, kitsModule.getKitManager(), gm),
          true, 30
      ));
    }

    // TPA page
    TeleportModule teleport = getTeleportModule();
    if (teleport != null && teleport.isEnabled() && teleport.getTpaManager() != null) {
      playerReg.registerEntry(new PageRegistry.Entry(
          "tpa", "TPA", "teleport", Permissions.TPA,
          (player, ref, store, playerRef, gm) ->
              new TpaPage(player, playerRef, teleport.getTpaManager(), gm),
          true, 40
      ));
    }

    // Stats page
    UtilityModule utilityModule = moduleRegistry.getModule(UtilityModule.class);
    UtilityManager utilMgr = utilityModule != null && utilityModule.isEnabled()
        ? utilityModule.getUtilityManager() : null;

    playerReg.registerEntry(new PageRegistry.Entry(
        "stats", "Stats", "core", null,
        (player, ref, store, playerRef, gm) ->
            new StatsPage(player, playerRef, gm, utilMgr),
        true, 50
    ));

    // Dashboard (registered last since it references other module managers)
    HomesModule homesForDash = getHomesModule();
    playerReg.registerEntry(new PageRegistry.Entry(
        "dashboard", "Dashboard", "core", null,
        (player, ref, store, playerRef, gm) ->
            new PlayerDashboardPage(player, playerRef, gm,
                homesForDash != null && homesForDash.isEnabled() ? homesForDash.getHomeManager() : null,
                teleport != null && teleport.isEnabled() ? teleport.getTpaManager() : null,
                utilMgr),
        true, 0
    ));

    int pageCount = playerReg.getEntries().size();
    if (pageCount > 0) {
      Logger.info("[GUI] Registered %d player page(s)", pageCount);
    }

    // === Admin Pages ===
    PageRegistry adminReg = guiManager.getAdminRegistry();

    // Admin Dashboard
    adminReg.registerEntry(new PageRegistry.Entry(
        "dashboard", "Dashboard", "core", null,
        (player, ref, store, playerRef, gm) ->
            new AdminDashboardPage(player, playerRef, gm, moduleRegistry),
        true, 0
    ));

    // Admin Warps
    if (warps != null && warps.isEnabled() && warps.getWarpManager() != null) {
      adminReg.registerEntry(new PageRegistry.Entry(
          "warps", "Warps", "warps", Permissions.WARP_SET,
          (player, ref, store, playerRef, gm) ->
              new AdminWarpsPage(player, playerRef, warps.getWarpManager(), gm),
          true, 20
      ));
    }

    // Admin Spawns
    SpawnsModule spawnsForAdmin = getSpawnsModule();
    if (spawnsForAdmin != null && spawnsForAdmin.isEnabled() && spawnsForAdmin.getSpawnManager() != null) {
      adminReg.registerEntry(new PageRegistry.Entry(
          "spawns", "Spawns", "spawns", Permissions.SPAWN_SET,
          (player, ref, store, playerRef, gm) ->
              new AdminSpawnsPage(player, playerRef, spawnsForAdmin.getSpawnManager(), gm),
          true, 30
      ));
    }

    // Admin Kits
    if (kitsModule != null && kitsModule.isEnabled() && kitsModule.getKitManager() != null) {
      adminReg.registerEntry(new PageRegistry.Entry(
          "kits", "Kits", "kits", Permissions.KIT_CREATE,
          (player, ref, store, playerRef, gm) ->
              new AdminKitsPage(player, playerRef, ref, store, kitsModule.getKitManager(), gm),
          true, 40
      ));
    }

    // Admin Players
    adminReg.registerEntry(new PageRegistry.Entry(
        "players", "Players", "core", Permissions.ADMIN_GUI,
        (player, ref, store, playerRef, gm) ->
            new AdminPlayersPage(player, playerRef, gm),
        true, 10
    ));

    // Admin Moderation
    ModerationModule modModule = moduleRegistry.getModule(ModerationModule.class);
    if (modModule != null && modModule.isEnabled() && modModule.getModerationManager() != null) {
      adminReg.registerEntry(new PageRegistry.Entry(
          "moderation", "Moderation", "moderation", Permissions.ADMIN_GUI,
          (player, ref, store, playerRef, gm) ->
              new AdminModerationPage(player, playerRef, modModule.getModerationManager(), gm),
          true, 50
      ));
    }

    // Admin Announcements
    AnnouncementsModule annModule = moduleRegistry.getModule(AnnouncementsModule.class);
    if (annModule != null && annModule.isEnabled()) {
      adminReg.registerEntry(new PageRegistry.Entry(
          "announcements", "Announcements", "announcements", Permissions.ADMIN_GUI,
          (player, ref, store, playerRef, gm) ->
              new AdminAnnouncementsPage(player, playerRef, gm, annModule.getScheduler()),
          true, 60
      ));
    }

    // Admin Settings
    adminReg.registerEntry(new PageRegistry.Entry(
        "settings", "Settings", "core", Permissions.ADMIN_SETTINGS,
        (player, ref, store, playerRef, gm) ->
            new AdminSettingsPage(player, playerRef, gm, moduleRegistry, dataDir),
        true, 70
    ));

    int adminPageCount = adminReg.getEntries().size();
    if (adminPageCount > 0) {
      Logger.info("[GUI] Registered %d admin page(s)", adminPageCount);
    }

    // Wire real-time update service
    guiUpdateService = new GuiUpdateService(guiManager.getPageTracker());

    if (homes != null && homes.isEnabled() && homes.getHomeManager() != null) {
      homes.getHomeManager().setOnHomeChanged(guiUpdateService::onHomeChanged);
    }
    if (warps != null && warps.isEnabled() && warps.getWarpManager() != null) {
      warps.getWarpManager().setOnWarpChanged(guiUpdateService::onWarpChanged);
    }
    if (kitsModule != null && kitsModule.isEnabled() && kitsModule.getKitManager() != null) {
      kitsModule.getKitManager().setOnKitClaimed(guiUpdateService::onKitClaimed);
    }
    if (teleport != null && teleport.isEnabled() && teleport.getTpaManager() != null) {
      teleport.getTpaManager().setOnTpaChanged(guiUpdateService::onTpaRequestChanged);
    }

    // Start periodic cooldown refresh for GUI pages
    guiUpdateService.start();
  }

  /**
   * Ensures the standard directory structure exists.
   */
  private void ensureDirectories() {
    try {
      Files.createDirectories(dataDir.resolve("config"));
      Files.createDirectories(dataDir.resolve("data"));
      Files.createDirectories(dataDir.resolve("data/players"));
      Files.createDirectories(dataDir.resolve("data/homes"));
      Files.createDirectories(dataDir.resolve("data/kits"));
      Files.createDirectories(dataDir.resolve("data/spawns"));
      Files.createDirectories(dataDir.resolve("data/warps"));
      Files.createDirectories(dataDir.resolve("backups"));

      // Write .version marker if it doesn't exist
      Path versionFile = dataDir.resolve("data/.version");
      if (!Files.exists(versionFile)) {
        int configVersion = 1;
        Files.writeString(versionFile, String.valueOf(configVersion));
        Logger.debug("[Startup] Created .version marker: %d", configVersion);
      }
    } catch (IOException e) {
      Logger.severe("[Startup] Failed to create directories: %s", e.getMessage());
    }
  }

  // Module getters

  @Nullable
  public WarpsModule getWarpsModule() { return moduleRegistry.getModule(WarpsModule.class); }
  @Nullable
  public SpawnsModule getSpawnsModule() { return moduleRegistry.getModule(SpawnsModule.class); }
  @Nullable
  public HomesModule getHomesModule() { return moduleRegistry.getModule(HomesModule.class); }
  @Nullable
  public TeleportModule getTeleportModule() { return moduleRegistry.getModule(TeleportModule.class); }

  // Getters

  @NotNull public Path getDataDir() { return dataDir; }
  @NotNull public ConfigManager getConfigManager() { return configManager; }
  @NotNull public ModuleRegistry getModuleRegistry() { return moduleRegistry; }
  @NotNull public GuiManager getGuiManager() { return guiManager; }
  @NotNull public WarmupManager getWarmupManager() { return warmupManager; }
  @NotNull public StorageProvider getStorageProvider() { return storageProvider; }
  @NotNull public VaultEconomyProvider getVaultEconomy() { return vaultEconomy; }

  /**
   * Gets a module by class.
   */
  @Nullable
  public <T extends com.hyperessentials.module.Module> T getModule(@NotNull Class<T> clazz) {
    return moduleRegistry.getModule(clazz);
  }

  /**
   * Checks if a module is enabled by name.
   */
  public boolean isModuleEnabled(@NotNull String name) {
    return ConfigManager.get().isModuleEnabled(name);
  }

  /**
   * Registers a handler that is called when a player connects.
   * Handler receives (UUID, username).
   */
  public void registerConnectHandler(@NotNull BiConsumer<UUID, String> handler) {
    connectHandlers.add(handler);
  }

  /**
   * Unregisters a connect handler.
   */
  public void unregisterConnectHandler(@NotNull BiConsumer<UUID, String> handler) {
    connectHandlers.remove(handler);
  }

  /**
   * Called by the plugin when a player connects.
   * Notifies all registered connect handlers.
   */
  public void onPlayerConnect(@NotNull UUID uuid, @NotNull String username) {
    for (BiConsumer<UUID, String> handler : connectHandlers) {
      try {
        handler.accept(uuid, username);
      } catch (Exception e) {
        Logger.severe("Error in connect handler: %s", e.getMessage());
      }
    }
  }

  /**
   * Registers a handler that is called when a player disconnects.
   * Used by modules to clean up session state.
   */
  public void registerDisconnectHandler(@NotNull Consumer<UUID> handler) {
    disconnectHandlers.add(handler);
  }

  /**
   * Unregisters a disconnect handler.
   */
  public void unregisterDisconnectHandler(@NotNull Consumer<UUID> handler) {
    disconnectHandlers.remove(handler);
  }

  /**
   * Called by the plugin when a player disconnects.
   * Notifies all registered disconnect handlers.
   */
  public void onPlayerDisconnect(@NotNull UUID uuid) {
    for (Consumer<UUID> handler : disconnectHandlers) {
      try {
        handler.accept(uuid);
      } catch (Exception e) {
        Logger.severe("Error in disconnect handler: %s", e.getMessage());
      }
    }
  }
}
