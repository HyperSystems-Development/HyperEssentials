package com.hyperessentials.module.warps;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.module.AbstractModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Warps module for HyperEssentials.
 */
public class WarpsModule extends AbstractModule {

    @Override
    @NotNull
    public String getName() {
        return "warps";
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Warps";
    }

    @Override
    public void onEnable() {
        super.onEnable();
        // TODO: Register commands, listeners, and storage
    }

    @Override
    public void onDisable() {
        // TODO: Unregister commands, save data, cleanup
        super.onDisable();
    }

    @Override
    @Nullable
    public ModuleConfig getModuleConfig() {
        return ConfigManager.get().warps();
    }
}
