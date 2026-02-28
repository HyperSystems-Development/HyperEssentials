package com.hyperessentials.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses duration strings like "1h30m", "7d", "30s" into milliseconds
 * and formats milliseconds back into human-readable strings.
 */
public final class DurationParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile(
        "(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?",
        Pattern.CASE_INSENSITIVE
    );

    private DurationParser() {}

    /**
     * Parses a duration string into milliseconds.
     *
     * @param input duration string like "1h30m", "7d", "30s", "2d12h"
     * @return milliseconds, or -1 if invalid
     */
    public static long parse(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return -1;
        }

        String trimmed = input.trim().toLowerCase();
        Matcher matcher = DURATION_PATTERN.matcher(trimmed);

        if (!matcher.matches()) {
            return -1;
        }

        String days = matcher.group(1);
        String hours = matcher.group(2);
        String minutes = matcher.group(3);
        String seconds = matcher.group(4);

        if (days == null && hours == null && minutes == null && seconds == null) {
            return -1;
        }

        long total = 0;
        if (days != null) total += Long.parseLong(days) * 86400000L;
        if (hours != null) total += Long.parseLong(hours) * 3600000L;
        if (minutes != null) total += Long.parseLong(minutes) * 60000L;
        if (seconds != null) total += Long.parseLong(seconds) * 1000L;

        return total > 0 ? total : -1;
    }

    /**
     * Formats milliseconds into a human-readable string like "1 hour 30 minutes".
     */
    @NotNull
    public static String formatHuman(long millis) {
        if (millis < 1000) return "0 seconds";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " day " : " days ");
        if (hours > 0) sb.append(hours).append(hours == 1 ? " hour " : " hours ");
        if (minutes > 0) sb.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
        if (seconds > 0 && days == 0) sb.append(seconds).append(seconds == 1 ? " second" : " seconds");

        return sb.toString().trim();
    }

    /**
     * Formats milliseconds into a compact string like "1d2h30m".
     */
    @NotNull
    public static String formatCompact(long millis) {
        if (millis < 1000) return "0s";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        seconds %= 60;
        minutes %= 60;
        hours %= 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d");
        if (hours > 0) sb.append(hours).append("h");
        if (minutes > 0) sb.append(minutes).append("m");
        if (seconds > 0 && days == 0) sb.append(seconds).append("s");

        return sb.isEmpty() ? "0s" : sb.toString();
    }
}
