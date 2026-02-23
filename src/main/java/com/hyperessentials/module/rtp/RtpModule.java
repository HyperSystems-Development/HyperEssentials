package com.hyperessentials.module.rtp;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.config.modules.RtpConfig;
import com.hyperessentials.module.AbstractModule;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Random Teleport module for HyperEssentials.
 */
public class RtpModule extends AbstractModule {

    private RtpManager rtpManager;

    @Override
    @NotNull
    public String getName() {
        return "rtp";
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Random Teleport";
    }

    @Override
    public void onEnable() {
        super.onEnable();
        RtpConfig config = ConfigManager.get().rtp();
        this.rtpManager = new RtpManager(config);
        Logger.info("[RTP] RtpManager initialized");
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Nullable
    public RtpManager getRtpManager() {
        return rtpManager;
    }

    @Override
    @Nullable
    public ModuleConfig getModuleConfig() {
        return ConfigManager.get().rtp();
    }
}
