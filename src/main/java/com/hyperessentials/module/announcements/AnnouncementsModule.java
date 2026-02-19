package com.hyperessentials.module.announcements;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.module.AbstractModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Announcements module for HyperEssentials.
 */
public class AnnouncementsModule extends AbstractModule {

    @Override
    @NotNull
    public String getName() {
        return "announcements";
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Announcements";
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
        return ConfigManager.get().announcements();
    }
}
