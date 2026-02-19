package com.hyperessentials.module.homes;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.module.AbstractModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Homes module for HyperEssentials.
 */
public class HomesModule extends AbstractModule {

    @Override
    @NotNull
    public String getName() {
        return "homes";
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Homes";
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
        return ConfigManager.get().homes();
    }
}
