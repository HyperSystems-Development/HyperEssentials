package com.hyperessentials.module.utility;

import com.hyperessentials.HyperEssentials;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.config.modules.UtilityConfig;
import com.hyperessentials.module.AbstractModule;
import com.hyperessentials.module.utility.command.*;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Utility module for HyperEssentials.
 * Provides convenience commands: heal, fly, god, clearchat, clearinventory, repair, near.
 */
public class UtilityModule extends AbstractModule {

    private UtilityManager utilityManager;
    private Consumer<UUID> disconnectHandler;

    @Override
    @NotNull
    public String getName() {
        return "utility";
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return "Utility";
    }

    @Override
    public void onEnable() {
        super.onEnable();

        HyperEssentials core = HyperEssentialsAPI.getInstance();
        if (core == null) return;

        utilityManager = new UtilityManager();

        // Register disconnect handler for state cleanup
        disconnectHandler = utilityManager::onPlayerDisconnect;
        core.registerDisconnectHandler(disconnectHandler);

        // Register commands based on config toggles
        HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
        if (plugin != null) {
            UtilityConfig config = ConfigManager.get().utility();

            try {
                if (config.isHealEnabled())
                    plugin.getCommandRegistry().registerCommand(new HealCommand());
                if (config.isFlyEnabled())
                    plugin.getCommandRegistry().registerCommand(new FlyCommand(this));
                if (config.isGodEnabled())
                    plugin.getCommandRegistry().registerCommand(new GodCommand(this));
                if (config.isClearChatEnabled())
                    plugin.getCommandRegistry().registerCommand(new ClearChatCommand());
                if (config.isClearInventoryEnabled())
                    plugin.getCommandRegistry().registerCommand(new ClearInventoryCommand());
                if (config.isRepairEnabled())
                    plugin.getCommandRegistry().registerCommand(new RepairCommand());
                if (config.isNearEnabled())
                    plugin.getCommandRegistry().registerCommand(new NearCommand());

                Logger.info("[Utility] Registered utility commands");
            } catch (Exception e) {
                Logger.severe("[Utility] Failed to register commands: %s", e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        if (utilityManager != null) {
            utilityManager.shutdown();
        }

        if (disconnectHandler != null) {
            HyperEssentials core = HyperEssentialsAPI.getInstance();
            if (core != null) {
                core.unregisterDisconnectHandler(disconnectHandler);
            }
        }

        super.onDisable();
    }

    @NotNull
    public UtilityManager getUtilityManager() {
        return utilityManager;
    }

    @Override
    @Nullable
    public ModuleConfig getModuleConfig() {
        return ConfigManager.get().utility();
    }
}
