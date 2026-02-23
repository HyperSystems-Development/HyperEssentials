package com.hyperessentials.module.teleport;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.config.modules.TeleportConfig;
import com.hyperessentials.module.AbstractModule;
import com.hyperessentials.storage.PlayerDataStorage;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Teleport module for HyperEssentials.
 */
public class TeleportModule extends AbstractModule {

    private TpaManager tpaManager;
    private BackManager backManager;

    @Override
    @NotNull
    public String getName() {
        return "teleport";
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Teleport";
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    public void initManagers(@NotNull PlayerDataStorage storage) {
        TeleportConfig config = ConfigManager.get().teleport();
        this.tpaManager = new TpaManager(storage, config);
        this.backManager = new BackManager(tpaManager, config);
        Logger.info("[Teleport] TpaManager and BackManager initialized");
    }

    @Override
    public void onDisable() {
        if (tpaManager != null) {
            tpaManager.saveAll().join();
        }
        super.onDisable();
    }

    @Nullable
    public TpaManager getTpaManager() {
        return tpaManager;
    }

    @Nullable
    public BackManager getBackManager() {
        return backManager;
    }

    @Override
    @Nullable
    public ModuleConfig getModuleConfig() {
        return ConfigManager.get().teleport();
    }
}
