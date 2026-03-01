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
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Utility module for HyperEssentials.
 * Provides convenience commands: heal, fly, god, clearchat, clearinventory, repair, near,
 * motd, rules, discord, list, playtime, joindate, afk, invsee, stamina, trash, maxstack, sleeppercentage.
 */
public class UtilityModule extends AbstractModule {

  private UtilityManager utilityManager;
  private Consumer<UUID> disconnectHandler;
  private BiConsumer<UUID, String> connectHandler;

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
    utilityManager.init(core.getDataDir());

    // Register connect handler for session/stats tracking
    connectHandler = (uuid, username) -> utilityManager.onPlayerConnect(uuid, username);
    core.registerConnectHandler(connectHandler);

    // Register disconnect handler for state cleanup
    disconnectHandler = utilityManager::onPlayerDisconnect;
    core.registerDisconnectHandler(disconnectHandler);

    // Register event listeners for AFK activity tracking
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      UtilityConfig config = ConfigManager.get().utility();

      // AFK activity listeners
      if (config.isAfkEnabled()) {
        plugin.getEventRegistry().registerGlobal(PlayerChatEvent.class,
          event -> utilityManager.onPlayerActivity(event.getSender().getUuid()));
        plugin.getEventRegistry().registerGlobal(PlayerInteractEvent.class,
          event -> utilityManager.onPlayerActivity(event.getPlayer().getUuid()));
      }

      // Register commands based on config toggles
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
        if (config.isRepairEnabled()) {
          plugin.getCommandRegistry().registerCommand(new RepairCommand());
          plugin.getCommandRegistry().registerCommand(new RepairMaxCommand());
        }
        if (config.isDurabilityEnabled())
          plugin.getCommandRegistry().registerCommand(new DurabilityCommand());
        if (config.isNearEnabled())
          plugin.getCommandRegistry().registerCommand(new NearCommand());

        // New commands
        if (config.isMotdEnabled())
          plugin.getCommandRegistry().registerCommand(new MotdCommand());
        if (config.isRulesEnabled())
          plugin.getCommandRegistry().registerCommand(new RulesCommand());
        if (config.isDiscordEnabled())
          plugin.getCommandRegistry().registerCommand(new DiscordCommand());
        if (config.isListEnabled())
          plugin.getCommandRegistry().registerCommand(new ListCommand());
        if (config.isPlaytimeEnabled())
          plugin.getCommandRegistry().registerCommand(new PlaytimeCommand(this));
        if (config.isJoindateEnabled())
          plugin.getCommandRegistry().registerCommand(new JoinDateCommand(this));
        if (config.isAfkEnabled())
          plugin.getCommandRegistry().registerCommand(new AfkCommand(this));
        if (config.isInvseeEnabled())
          plugin.getCommandRegistry().registerCommand(new InvSeeCommand());
        if (config.isStaminaEnabled())
          plugin.getCommandRegistry().registerCommand(new StaminaCommand(this));
        if (config.isTrashEnabled())
          plugin.getCommandRegistry().registerCommand(new TrashCommand());
        if (config.isMaxstackEnabled())
          plugin.getCommandRegistry().registerCommand(new MaxStackCommand());
        if (config.isSleepPercentageEnabled())
          plugin.getCommandRegistry().registerCommand(new SleepPercentageCommand());

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

    HyperEssentials core = HyperEssentialsAPI.getInstance();
    if (core != null) {
      if (disconnectHandler != null) {
        core.unregisterDisconnectHandler(disconnectHandler);
      }
      if (connectHandler != null) {
        core.unregisterConnectHandler(connectHandler);
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
