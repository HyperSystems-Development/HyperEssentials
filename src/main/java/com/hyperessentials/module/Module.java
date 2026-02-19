package com.hyperessentials.module;

import com.hyperessentials.config.ModuleConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for HyperEssentials modules.
 * Each module represents a feature group that can be independently enabled/disabled.
 */
public interface Module {

    /**
     * Gets the unique module name.
     */
    @NotNull
    String getName();

    /**
     * Gets the display name for UI.
     */
    @NotNull
    String getDisplayName();

    /**
     * Whether this module is currently enabled.
     */
    boolean isEnabled();

    /**
     * Called when the module is enabled.
     */
    void onEnable();

    /**
     * Called when the module is disabled.
     */
    void onDisable();

    /**
     * Gets the module's configuration.
     */
    @Nullable
    ModuleConfig getModuleConfig();
}
