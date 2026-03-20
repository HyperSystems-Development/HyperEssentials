package com.hyperessentials.module.announcements;

import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.AnnouncementsConfig;
import com.hyperessentials.integration.PermissionManager;
import com.hyperessentials.module.announcements.data.Announcement;
import com.hyperessentials.module.announcements.data.AnnouncementType;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Schedules periodic broadcast announcements with support for multiple types,
 * per-announcement cron schedules, world filtering, and permission filtering.
 */
public class AnnouncementScheduler {

  private final AtomicInteger currentIndex = new AtomicInteger(0);
  private final Random random = new Random();
  private ScheduledFuture<?> globalTask;
  private ScheduledFuture<?> cronTask;

  /** Parsed cron schedulers keyed by announcement UUID. */
  private final Map<UUID, CronScheduler> cronSchedulers = new ConcurrentHashMap<>();

  /** Tracks the last minute a cron check ran to avoid duplicate sends. */
  private long lastCronCheckMinute = -1;

  public void start() {
    AnnouncementsConfig config = ConfigManager.get().announcements();

    // Build cron schedulers for announcements with cron expressions
    rebuildCronSchedulers(config);

    // Global interval task for non-cron announcements
    int intervalSeconds = config.getIntervalSeconds();
    if (intervalSeconds > 0) {
      globalTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
          this::broadcastNext, intervalSeconds, intervalSeconds, TimeUnit.SECONDS
      );
      Logger.info("[Announcements] Scheduler started (interval: %ds)", intervalSeconds);
    }

    // Cron check task — runs every 30 seconds to check minute boundaries
    if (!cronSchedulers.isEmpty()) {
      cronTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
          this::checkCronAnnouncements, 10, 30, TimeUnit.SECONDS
      );
      Logger.info("[Announcements] Cron scheduler started (%d cron announcements)", cronSchedulers.size());
    }
  }

  public void shutdown() {
    if (globalTask != null) {
      globalTask.cancel(false);
      globalTask = null;
    }
    if (cronTask != null) {
      cronTask.cancel(false);
      cronTask = null;
    }
    cronSchedulers.clear();
  }

  public void restart() {
    shutdown();
    start();
  }

  /**
   * Rebuilds the cached cron schedulers from config.
   */
  private void rebuildCronSchedulers(@NotNull AnnouncementsConfig config) {
    cronSchedulers.clear();
    for (Announcement ann : config.getAnnouncements()) {
      if (ann.enabled() && ann.cronExpression() != null && !ann.cronExpression().isBlank()) {
        CronScheduler cron = CronScheduler.parse(ann.cronExpression());
        if (cron != null) {
          cronSchedulers.put(ann.id(), cron);
        } else {
          Logger.warn("[Announcements] Invalid cron expression for announcement %s: %s",
              ann.id(), ann.cronExpression());
        }
      }
    }
  }

  /**
   * Broadcasts the next announcement in rotation (for non-cron announcements).
   */
  private void broadcastNext() {
    try {
      AnnouncementsConfig config = ConfigManager.get().announcements();
      List<Announcement> eligible = getEligibleGlobalAnnouncements(config);

      if (eligible.isEmpty()) return;

      HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
      if (plugin == null || plugin.getTrackedPlayers().isEmpty()) return;

      Announcement announcement;
      if (config.isRandomize()) {
        announcement = eligible.get(random.nextInt(eligible.size()));
      } else {
        int idx = currentIndex.getAndUpdate(i -> (i + 1) % eligible.size());
        announcement = eligible.get(idx);
      }

      broadcastAnnouncement(announcement, config, plugin);
    } catch (Exception e) {
      ErrorHandler.report("[Announcements] Error broadcasting", e);
    }
  }

  /**
   * Checks cron-scheduled announcements and broadcasts any that match the current minute.
   */
  private void checkCronAnnouncements() {
    try {
      Instant now = Instant.now();
      long currentMinute = now.getEpochSecond() / 60;
      if (currentMinute == lastCronCheckMinute) return;
      lastCronCheckMinute = currentMinute;

      HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
      if (plugin == null || plugin.getTrackedPlayers().isEmpty()) return;

      AnnouncementsConfig config = ConfigManager.get().announcements();

      for (Announcement ann : config.getAnnouncements()) {
        if (!ann.enabled() || ann.cronExpression() == null) continue;

        CronScheduler cron = cronSchedulers.get(ann.id());
        if (cron != null && cron.shouldRun(now)) {
          broadcastAnnouncement(ann, config, plugin);
        }
      }
    } catch (Exception e) {
      ErrorHandler.report("[Announcements] Error in cron check", e);
    }
  }

  /**
   * Returns announcements that should use the global interval (no cron expression).
   */
  @NotNull
  private List<Announcement> getEligibleGlobalAnnouncements(@NotNull AnnouncementsConfig config) {
    List<Announcement> eligible = new ArrayList<>();
    for (Announcement ann : config.getAnnouncements()) {
      if (ann.enabled() && (ann.cronExpression() == null || ann.cronExpression().isBlank())) {
        eligible.add(ann);
      }
    }
    eligible.sort(Comparator.comparingInt(Announcement::order));
    return eligible;
  }

  /**
   * Broadcasts a single announcement to all eligible players.
   */
  private void broadcastAnnouncement(
      @NotNull Announcement announcement,
      @NotNull AnnouncementsConfig config,
      @NotNull HyperEssentialsPlugin plugin
  ) {
    for (PlayerRef player : plugin.getTrackedPlayers().values()) {
      if (!isEligible(player, announcement)) continue;

      switch (announcement.type()) {
        case CHAT -> player.sendMessage(buildChatMessage(announcement.message(), config));
        case NOTIFICATION -> sendNotification(player, announcement.message(), config);
      }
    }
  }

  /**
   * Checks if a player should receive an announcement based on permission and world filters.
   */
  private boolean isEligible(@NotNull PlayerRef player, @NotNull Announcement announcement) {
    // Permission filter
    if (announcement.permission() != null && !announcement.permission().isBlank()) {
      if (!PermissionManager.get().hasPermission(player.getUuid(), announcement.permission())) {
        return false;
      }
    }

    // World filter
    if (announcement.world() != null && !announcement.world().isBlank()) {
      try {
        java.util.UUID worldUuid = player.getWorldUuid();
        if (worldUuid != null) {
          World world = Universe.get().getWorld(worldUuid);
          String playerWorldName = world != null ? world.getName() : null;
          if (playerWorldName == null || !playerWorldName.equalsIgnoreCase(announcement.world())) {
            return false;
          }
        }
      } catch (Exception ignored) {
        // If we can't determine the world, send anyway
      }
    }

    return true;
  }

  /**
   * Broadcasts a single message to all players immediately (for /broadcast command).
   */
  public void broadcastNow(@NotNull String text) {
    AnnouncementsConfig config = ConfigManager.get().announcements();
    Message chatMessage = buildChatMessage(text, config);

    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    for (PlayerRef player : plugin.getTrackedPlayers().values()) {
      player.sendMessage(chatMessage);
    }
  }

  /**
   * Broadcasts a typed announcement to all players immediately (for preview).
   */
  public void broadcastNow(@NotNull String text, @NotNull AnnouncementType type) {
    AnnouncementsConfig config = ConfigManager.get().announcements();
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    for (PlayerRef player : plugin.getTrackedPlayers().values()) {
      switch (type) {
        case CHAT -> player.sendMessage(buildChatMessage(text, config));
        case NOTIFICATION -> sendNotification(player, text, config);
      }
    }
  }

  /**
   * Sends a notification toast to a single player.
   */
  private void sendNotification(@NotNull PlayerRef player, @NotNull String text,
                                @NotNull AnnouncementsConfig config) {
    try {
      Message title = Message.raw(config.getPrefixText()).color(config.getPrefixColor());
      Message body = Message.raw(text).color(config.getMessageColor());
      NotificationUtil.sendNotification(
          player.getPacketHandler(), title, body, NotificationStyle.Default
      );
    } catch (Exception e) {
      // Fall back to chat if notification fails
      player.sendMessage(buildChatMessage(text, config));
    }
  }

  /**
   * Sends a notification toast to a single player with a custom title.
   */
  public static void sendNotificationToPlayer(@NotNull PlayerRef player, @NotNull String title,
                                               @NotNull String text) {
    try {
      NotificationUtil.sendNotification(
          player.getPacketHandler(),
          Message.raw(title).color("#FFAA00"),
          Message.raw(text).color("#FFFFFF"),
          NotificationStyle.Default
      );
    } catch (Exception e) {
      // Fall back to chat
      player.sendMessage(Message.raw("[" + title + "] " + text));
    }
  }

  @NotNull
  private Message buildChatMessage(@NotNull String text, @NotNull AnnouncementsConfig config) {
    return Message.raw("[").color(CommandUtil.COLOR_DARK_GRAY)
        .insert(Message.raw(config.getPrefixText()).color(config.getPrefixColor()))
        .insert(Message.raw("] ").color(CommandUtil.COLOR_DARK_GRAY))
        .insert(Message.raw(text).color(config.getMessageColor()));
  }
}
