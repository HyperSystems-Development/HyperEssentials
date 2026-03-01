package com.hyperessentials.module.moderation;

import com.hyperessentials.HyperEssentials;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.module.AbstractModule;
import com.hyperessentials.module.moderation.command.*;
import com.hyperessentials.module.moderation.listener.ModerationListener;
import com.hyperessentials.module.moderation.storage.ModerationStorage;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Moderation module for HyperEssentials.
 * Provides ban, mute, kick, freeze, vanish, punishment history, and IP bans.
 */
public class ModerationModule extends AbstractModule {

  private ModerationStorage storage;
  private ModerationManager moderationManager;
  private FreezeManager freezeManager;
  private VanishManager vanishManager;
  private ModerationListener listener;
  private Consumer<UUID> disconnectHandler;
  private BiConsumer<UUID, String> connectHandler;

  @Override
  @NotNull
  public String getName() {
    return "moderation";
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return "Moderation";
  }

  @Override
  public void onEnable() {
    super.onEnable();

    HyperEssentials core = HyperEssentialsAPI.getInstance();
    if (core == null) return;

    // Initialize storage
    storage = new ModerationStorage(core.getDataDir());
    storage.load();

    // Initialize managers
    moderationManager = new ModerationManager(storage);
    freezeManager = new FreezeManager();
    vanishManager = new VanishManager();

    // Start freeze movement checker
    freezeManager.start();

    // Register listener
    listener = new ModerationListener(this);
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      plugin.getEventRegistry().register(PlayerConnectEvent.class, listener::onPlayerConnect);
      plugin.getEventRegistry().registerGlobal(PlayerChatEvent.class, listener::onPlayerChat);

      // Register disconnect handler
      disconnectHandler = uuid -> {
        freezeManager.onPlayerDisconnect(uuid);
        vanishManager.onPlayerDisconnect(uuid);
      };
      core.registerDisconnectHandler(disconnectHandler);

      // Register connect handler for IP tracking
      connectHandler = (uuid, username) -> {
        PlayerRef ref = plugin.getTrackedPlayer(uuid);
        if (ref != null) {
          moderationManager.onPlayerConnect(ref);
        }
      };
      core.registerConnectHandler(connectHandler);

      // Register commands
      try {
        plugin.getCommandRegistry().registerCommand(new BanCommand(this));
        plugin.getCommandRegistry().registerCommand(new UnbanCommand(this));
        plugin.getCommandRegistry().registerCommand(new MuteCommand(this));
        plugin.getCommandRegistry().registerCommand(new UnmuteCommand(this));
        plugin.getCommandRegistry().registerCommand(new KickCommand(this));
        plugin.getCommandRegistry().registerCommand(new FreezeCommand(this));
        plugin.getCommandRegistry().registerCommand(new VanishCommand(this));
        plugin.getCommandRegistry().registerCommand(new PunishmentsCommand(this));
        plugin.getCommandRegistry().registerCommand(new IpBanCommand(this));
        plugin.getCommandRegistry().registerCommand(new IpUnbanCommand(this));
        Logger.info("[Moderation] Registered 10 commands");
      } catch (Exception e) {
        Logger.severe("[Moderation] Failed to register commands: %s", e.getMessage());
      }
    }
  }

  @Override
  public void onDisable() {
    if (freezeManager != null) freezeManager.shutdown();
    if (vanishManager != null) vanishManager.shutdown();
    if (moderationManager != null) moderationManager.shutdown();

    // Unregister handlers
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
  public ModerationManager getModerationManager() {
    return moderationManager;
  }

  @NotNull
  public FreezeManager getFreezeManager() {
    return freezeManager;
  }

  @NotNull
  public VanishManager getVanishManager() {
    return vanishManager;
  }

  @Override
  @Nullable
  public ModuleConfig getModuleConfig() {
    return ConfigManager.get().moderation();
  }
}
