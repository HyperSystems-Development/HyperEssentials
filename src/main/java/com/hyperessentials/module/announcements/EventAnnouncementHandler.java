package com.hyperessentials.module.announcements;

import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.AnnouncementsConfig;
import com.hyperessentials.integration.PermissionManager;
import com.hyperessentials.module.announcements.data.AnnouncementEvent;
import com.hyperessentials.module.announcements.data.AnnouncementType;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.storage.PlayerDataStorage;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Handles event-triggered announcements: join, leave, and first-join messages.
 * <p>
 * Registers as connect/disconnect handlers with HyperEssentials core.
 * Uses PlayerData.firstJoin to detect first-time players.
 */
public class EventAnnouncementHandler {

  @Nullable private BiConsumer<UUID, String> connectHandler;
  @Nullable private Consumer<UUID> disconnectHandler;
  @Nullable private PlayerDataStorage playerDataStorage;

  /**
   * Registers event listeners.
   */
  public void register(@NotNull PlayerDataStorage playerDataStorage) {
    this.playerDataStorage = playerDataStorage;

    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    connectHandler = (uuid, username) -> handleConnect(uuid, username);
    disconnectHandler = uuid -> handleDisconnect(uuid);

    plugin.getHyperEssentials().registerConnectHandler(connectHandler);
    plugin.getHyperEssentials().registerDisconnectHandler(disconnectHandler);

    Logger.debug("[Announcements] Event handlers registered");
  }

  /**
   * Unregisters event listeners.
   */
  public void unregister() {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    if (connectHandler != null) {
      plugin.getHyperEssentials().unregisterConnectHandler(connectHandler);
      connectHandler = null;
    }
    if (disconnectHandler != null) {
      plugin.getHyperEssentials().unregisterDisconnectHandler(disconnectHandler);
      disconnectHandler = null;
    }
  }

  /**
   * Handles a player connect event. Determines if this is a first join
   * and broadcasts appropriate join/welcome messages.
   */
  private void handleConnect(@NotNull UUID uuid, @NotNull String username) {
    try {
      AnnouncementsConfig config = ConfigManager.get().announcements();
      HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
      if (plugin == null) return;

      // Load player data to check if first join
      if (playerDataStorage != null) {
        playerDataStorage.loadPlayerData(uuid).thenAccept(opt -> {
          boolean isFirstJoin = opt.isEmpty() || isNewPlayer(opt.get().getFirstJoin());

          // First join / welcome message
          if (isFirstJoin && config.isWelcomeMessagesEnabled()) {
            broadcastEventMessages("first_join", username, plugin, config);
          }

          // Regular join message (always if enabled, even on first join)
          if (config.isJoinMessagesEnabled()) {
            broadcastEventMessages("join", username, plugin, config);
          }
        }).exceptionally(e -> {
          ErrorHandler.report("[Announcements] Failed to check first join", (Throwable) e);
          // Fall back to regular join message
          if (config.isJoinMessagesEnabled()) {
            broadcastEventMessages("join", username, plugin, config);
          }
          return null;
        });
      } else {
        // No player data storage, just send join message
        if (config.isJoinMessagesEnabled()) {
          broadcastEventMessages("join", username, plugin, config);
        }
      }
    } catch (Exception e) {
      ErrorHandler.report("[Announcements] Error handling connect event", e);
    }
  }

  /**
   * Handles a player disconnect event. Broadcasts leave messages.
   */
  private void handleDisconnect(@NotNull UUID uuid) {
    try {
      AnnouncementsConfig config = ConfigManager.get().announcements();
      if (!config.isLeaveMessagesEnabled()) return;

      HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
      if (plugin == null) return;

      // Get the username before the player reference is removed
      PlayerRef ref = plugin.getTrackedPlayer(uuid);
      String username = ref != null ? ref.getUsername() : uuid.toString();

      broadcastEventMessages("leave", username, plugin, config);
    } catch (Exception e) {
      ErrorHandler.report("[Announcements] Error handling disconnect event", e);
    }
  }

  /**
   * Checks if a player is new based on their firstJoin time.
   * A player is considered new if their firstJoin is within the last 5 seconds.
   */
  private boolean isNewPlayer(@NotNull Instant firstJoin) {
    return Duration.between(firstJoin, Instant.now()).toMillis() < 5000;
  }

  /**
   * Broadcasts all event announcements matching the given event type.
   */
  private void broadcastEventMessages(
      @NotNull String eventType,
      @NotNull String username,
      @NotNull HyperEssentialsPlugin plugin,
      @NotNull AnnouncementsConfig config
  ) {
    int onlineCount = plugin.getTrackedPlayers().size();

    for (AnnouncementEvent evt : config.getEventAnnouncements()) {
      if (!evt.enabled() || !evt.eventType().equals(eventType)) continue;

      String formatted = formatEventMessage(evt.message(), username, onlineCount);

      for (PlayerRef player : plugin.getTrackedPlayers().values()) {
        // Permission filter
        if (evt.permission() != null && !evt.permission().isBlank()) {
          if (!PermissionManager.get().hasPermission(player.getUuid(), evt.permission())) {
            continue;
          }
        }

        switch (evt.type()) {
          case CHAT -> player.sendMessage(buildEventChatMessage(formatted, config));
          case NOTIFICATION -> AnnouncementScheduler.sendNotificationToPlayer(
              player, config.getPrefixText(), formatted);
        }
      }
    }
  }

  /**
   * Formats an event message with placeholders.
   */
  @NotNull
  private String formatEventMessage(@NotNull String template, @NotNull String playerName,
                                    int onlineCount) {
    return template
        .replace("{player}", playerName)
        .replace("{online}", String.valueOf(onlineCount))
        .replace("{max}", "100"); // Could be configurable in the future
  }

  @NotNull
  private Message buildEventChatMessage(@NotNull String text, @NotNull AnnouncementsConfig config) {
    return Message.raw("[").color(CommandUtil.COLOR_DARK_GRAY)
        .insert(Message.raw(config.getPrefixText()).color(config.getPrefixColor()))
        .insert(Message.raw("] ").color(CommandUtil.COLOR_DARK_GRAY))
        .insert(Message.raw(text).color(config.getMessageColor()));
  }
}
