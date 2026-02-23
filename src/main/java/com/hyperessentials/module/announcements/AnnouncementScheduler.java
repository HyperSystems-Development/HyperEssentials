package com.hyperessentials.module.announcements;

import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.AnnouncementsConfig;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Schedules periodic broadcast announcements.
 */
public class AnnouncementScheduler {

    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final Random random = new Random();
    private ScheduledFuture<?> task;

    public void start() {
        int intervalSeconds = ConfigManager.get().announcements().getIntervalSeconds();
        task = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
            this::broadcastNext, intervalSeconds, intervalSeconds, TimeUnit.SECONDS
        );
        Logger.info("[Announcements] Scheduler started (interval: %ds)", intervalSeconds);
    }

    public void shutdown() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    public void restart() {
        shutdown();
        start();
    }

    private void broadcastNext() {
        try {
            AnnouncementsConfig config = ConfigManager.get().announcements();
            List<String> messages = config.getMessages();

            if (messages.isEmpty()) return;

            HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
            if (plugin == null || plugin.getTrackedPlayers().isEmpty()) return;

            String text;
            if (config.isRandomize()) {
                text = messages.get(random.nextInt(messages.size()));
            } else {
                int idx = currentIndex.getAndUpdate(i -> (i + 1) % messages.size());
                text = messages.get(idx);
            }

            Message announcement = buildAnnouncement(text, config);

            for (PlayerRef player : plugin.getTrackedPlayers().values()) {
                player.sendMessage(announcement);
            }
        } catch (Exception e) {
            Logger.warn("[Announcements] Error broadcasting: %s", e.getMessage());
        }
    }

    /**
     * Broadcasts a single message to all players immediately.
     */
    public void broadcastNow(@NotNull String text) {
        AnnouncementsConfig config = ConfigManager.get().announcements();
        Message announcement = buildAnnouncement(text, config);

        HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
        if (plugin == null) return;

        for (PlayerRef player : plugin.getTrackedPlayers().values()) {
            player.sendMessage(announcement);
        }
    }

    @NotNull
    private Message buildAnnouncement(@NotNull String text, @NotNull AnnouncementsConfig config) {
        return Message.raw("[").color(CommandUtil.COLOR_DARK_GRAY)
            .insert(Message.raw(config.getPrefixText()).color(config.getPrefixColor()))
            .insert(Message.raw("] ").color(CommandUtil.COLOR_DARK_GRAY))
            .insert(Message.raw(text).color(config.getMessageColor()));
    }
}
