package com.hyperessentials;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.integration.EcotaleIntegration;
import com.hyperessentials.integration.HyperFactionsIntegration;
import com.hyperessentials.integration.PermissionManager;
import com.hyperessentials.integration.WerchatIntegration;
import com.hyperessentials.module.ModuleRegistry;
import com.hyperessentials.module.announcements.AnnouncementsModule;
import com.hyperessentials.module.homes.HomesModule;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.module.rtp.RtpModule;
import com.hyperessentials.module.spawns.SpawnsModule;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.module.utility.UtilityModule;
import com.hyperessentials.module.vanish.VanishModule;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.module.warmup.WarmupModule;
import com.hyperessentials.module.warps.WarpsModule;
import com.hyperessentials.storage.StorageProvider;
import com.hyperessentials.storage.json.JsonStorageProvider;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Core singleton for HyperEssentials.
 * Manages all subsystems: config, permissions, modules, GUI, storage.
 */
public class HyperEssentials {

    private final Path dataDir;
    private final java.util.logging.Logger javaLogger;

    private ConfigManager configManager;
    private ModuleRegistry moduleRegistry;
    private GuiManager guiManager;
    private WarmupManager warmupManager;
    private StorageProvider storageProvider;

    public HyperEssentials(@NotNull Path dataDir, @NotNull java.util.logging.Logger javaLogger) {
        this.dataDir = dataDir;
        this.javaLogger = javaLogger;
    }

    /**
     * Enables HyperEssentials - loads config, initializes integrations, registers modules.
     */
    public void enable() {
        // Initialize logger
        Logger.init(javaLogger);

        // Load configuration
        configManager = ConfigManager.get();
        configManager.loadAll(dataDir);

        // Initialize integrations
        PermissionManager.get().init();
        HyperFactionsIntegration.init();
        EcotaleIntegration.init();
        WerchatIntegration.init();

        // Initialize storage
        storageProvider = new JsonStorageProvider(dataDir);
        storageProvider.init().join();

        // Initialize GUI
        guiManager = new GuiManager();

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
        moduleRegistry.register(new VanishModule());
        moduleRegistry.register(new UtilityModule());
        moduleRegistry.register(new AnnouncementsModule());
        moduleRegistry.register(new RtpModule());

        // Enable modules based on config
        moduleRegistry.enableAll();

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

        // Shutdown GUI
        if (guiManager != null) {
            guiManager.shutdown();
        }

        // Clear warmup state
        if (warmupManager != null) {
            warmupManager.clear();
        }

        // Save config
        if (configManager != null) {
            configManager.saveAll();
        }

        Logger.info("HyperEssentials disabled");
    }

    /**
     * Reloads configuration.
     */
    public void reloadConfig() {
        ConfigManager.get().reloadAll();
        Logger.info("Configuration reloaded");
    }

    // Getters

    @NotNull public Path getDataDir() { return dataDir; }
    @NotNull public ConfigManager getConfigManager() { return configManager; }
    @NotNull public ModuleRegistry getModuleRegistry() { return moduleRegistry; }
    @NotNull public GuiManager getGuiManager() { return guiManager; }
    @NotNull public WarmupManager getWarmupManager() { return warmupManager; }
    @NotNull public StorageProvider getStorageProvider() { return storageProvider; }

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
}
