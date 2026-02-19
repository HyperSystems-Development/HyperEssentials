package com.hyperessentials.module;

import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base implementation of {@link Module} with common lifecycle logic.
 */
public abstract class AbstractModule implements Module {

    private boolean enabled = false;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void onEnable() {
        enabled = true;
        Logger.info("[Module] %s enabled", getDisplayName());
    }

    @Override
    public void onDisable() {
        enabled = false;
        Logger.info("[Module] %s disabled", getDisplayName());
    }

    @Override
    @Nullable
    public ModuleConfig getModuleConfig() {
        return null;
    }
}
