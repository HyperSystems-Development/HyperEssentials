package com.hyperessentials.module.warps;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.module.AbstractModule;
import com.hyperessentials.storage.WarpStorage;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Warps module for HyperEssentials.
 */
public class WarpsModule extends AbstractModule {

    private WarpManager warpManager;

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
        // Note: WarpStorage and WarpManager are initialized here but commands
        // are registered at the platform layer (HyperEssentialsPlugin)
    }

    /**
     * Initializes the warp manager with the given storage.
     * Called by HyperEssentialsPlugin after module is enabled.
     */
    public void initManager(@NotNull WarpStorage storage) {
        this.warpManager = new WarpManager(storage);
        warpManager.loadWarps().join();
        Logger.info("[Warps] WarpManager initialized");
    }

    @Override
    public void onDisable() {
        if (warpManager != null) {
            warpManager.saveWarps().join();
        }
        super.onDisable();
    }

    @Nullable
    public WarpManager getWarpManager() {
        return warpManager;
    }

    @Override
    @Nullable
    public ModuleConfig getModuleConfig() {
        return ConfigManager.get().warps();
    }
}
