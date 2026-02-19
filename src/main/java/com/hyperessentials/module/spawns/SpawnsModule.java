package com.hyperessentials.module.spawns;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.module.AbstractModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spawns module for HyperEssentials.
 */
public class SpawnsModule extends AbstractModule {

    @Override
    @NotNull
    public String getName() {
        return "spawns";
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Spawns";
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
        return ConfigManager.get().spawns();
    }
}
