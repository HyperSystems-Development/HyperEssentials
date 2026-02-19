package com.hyperessentials.command.util;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.integration.PermissionManager;
import com.hypixel.hytale.server.core.Message;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Unified messaging utilities for HyperEssentials commands.
 */
public final class CommandUtil {

    public static final String COLOR_GOLD = "#FFAA00";
    public static final String COLOR_GREEN = "#55FF55";
    public static final String COLOR_RED = "#FF5555";
    public static final String COLOR_YELLOW = "#FFFF55";
    public static final String COLOR_GRAY = "#AAAAAA";
    public static final String COLOR_WHITE = "#FFFFFF";
    public static final String COLOR_AQUA = "#55FFFF";
    public static final String COLOR_DARK_GRAY = "#555555";

    private CommandUtil() {}

    @NotNull
    public static Message prefix() {
        ConfigManager config = ConfigManager.get();
        String bracketColor = config.core().getPrefixBracketColor();
        String textColor = config.core().getPrefixColor();
        String text = config.core().getPrefixText();

        return Message.raw("[").color(bracketColor)
            .insert(Message.raw(text).color(textColor))
            .insert(Message.raw("] ").color(bracketColor));
    }

    @NotNull
    public static Message msg(@NotNull String text, @NotNull String color) {
        return prefix().insert(Message.raw(text).color(color));
    }

    @NotNull
    public static Message error(@NotNull String text) {
        return msg(text, COLOR_RED);
    }

    @NotNull
    public static Message success(@NotNull String text) {
        return msg(text, COLOR_GREEN);
    }

    @NotNull
    public static Message info(@NotNull String text) {
        return msg(text, COLOR_YELLOW);
    }

    public static boolean hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
        return PermissionManager.get().hasPermission(playerUuid, permission);
    }

    public static int getPermissionValue(@NotNull UUID playerUuid, @NotNull String prefix, int defaultValue) {
        return PermissionManager.get().getPermissionValue(playerUuid, prefix, defaultValue);
    }

    @NotNull
    public static String formatTime(long ms) {
        long seconds = (ms + 999) / 1000;
        if (seconds < 60) {
            return seconds + " second" + (seconds == 1 ? "" : "s");
        }
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (seconds == 0) {
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        }
        return minutes + "m " + seconds + "s";
    }
}
