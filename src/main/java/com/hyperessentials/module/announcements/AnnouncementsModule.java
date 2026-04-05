package com.hyperessentials.module.announcements;

import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.module.AbstractModule;
import com.hyperessentials.module.announcements.command.AnnounceCommand;
import com.hyperessentials.module.announcements.command.BroadcastCommand;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Announcements module for HyperEssentials.
 * Provides scheduled auto-broadcast rotation, manual /broadcast,
 * event-triggered join/leave/welcome messages, and GUI management.
 */
public class AnnouncementsModule extends AbstractModule {

  private AnnouncementScheduler scheduler;
  private EventAnnouncementHandler eventHandler;

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

    scheduler = new AnnouncementScheduler();
    scheduler.start();

    // Initialize event handler for join/leave/welcome messages
    eventHandler = new EventAnnouncementHandler();
    com.hyperessentials.HyperEssentials core = HyperEssentialsAPI.getInstance();
    if (core != null) {
      eventHandler.register(core.getStorageProvider().getPlayerDataStorage());
    }

    // Register commands
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      try {
        plugin.getCommandRegistry().registerCommand(new BroadcastCommand(this));
        plugin.getCommandRegistry().registerCommand(new AnnounceCommand(this));
        Logger.info("[Announcements] Registered commands: /broadcast, /announce");
      } catch (Exception e) {
        Logger.severe("[Announcements] Failed to register commands: %s", e.getMessage());
      }
    }
  }

  @Override
  public void onDisable() {
    if (scheduler != null) {
      scheduler.shutdown();
    }
    if (eventHandler != null) {
      eventHandler.unregister();
    }
    super.onDisable();
  }

  @NotNull
  public AnnouncementScheduler getScheduler() {
    return scheduler;
  }

  @NotNull
  public EventAnnouncementHandler getEventHandler() {
    return eventHandler;
  }

  @Override
  @Nullable
  public ModuleConfig getModuleConfig() {
    return ConfigManager.get().announcements();
  }
}
