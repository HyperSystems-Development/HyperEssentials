package com.hyperessentials.gui;

import com.hypixel.hytale.server.core.Message;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper utilities for UI formatting and styling.
 */
public final class UIHelper {

    private static final Map<Character, String> COLOR_MAP = new HashMap<>();
    static {
        COLOR_MAP.put('0', "#000000");
        COLOR_MAP.put('1', "#0000AA");
        COLOR_MAP.put('2', "#00AA00");
        COLOR_MAP.put('3', "#00AAAA");
        COLOR_MAP.put('4', "#AA0000");
        COLOR_MAP.put('5', "#AA00AA");
        COLOR_MAP.put('6', "#FFAA00");
        COLOR_MAP.put('7', "#AAAAAA");
        COLOR_MAP.put('8', "#555555");
        COLOR_MAP.put('9', "#5555FF");
        COLOR_MAP.put('a', "#55FF55");
        COLOR_MAP.put('b', "#55FFFF");
        COLOR_MAP.put('c', "#FF5555");
        COLOR_MAP.put('d', "#FF55FF");
        COLOR_MAP.put('e', "#FFFF55");
        COLOR_MAP.put('f', "#FFFFFF");
        COLOR_MAP.put('r', "#FFFFFF");
    }

    // Brand colors
    public static final String COLOR_GOLD = "#FFAA00";
    public static final String COLOR_GOLD_LIGHT = "#FFD700";

    // Status colors
    public static final String COLOR_SUCCESS = "#4aff7f";
    public static final String COLOR_DANGER = "#ff4a4a";
    public static final String COLOR_PRIMARY = "#4a9eff";
    public static final String COLOR_WARNING = "#FFFF55";

    // Text colors
    public static final String COLOR_TEXT_PRIMARY = "#ffffff";
    public static final String COLOR_TEXT_SECONDARY = "#96a9be";
    public static final String COLOR_TEXT_MUTED = "#556677";

    // Background colors
    public static final String COLOR_BG_DARK = "#0a1119";
    public static final String COLOR_BG_LIGHT = "#141c26";

    private UIHelper() {}

    public static String formatCoords(double x, double y, double z) {
        return String.format("%.0f, %.0f, %.0f", x, y, z);
    }

    public static String formatNumber(long value) {
        return String.format("%,d", value);
    }

    public static String formatDuration(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        int minutes = seconds / 60;
        int secs = seconds % 60;
        if (secs == 0) {
            return minutes + "m";
        }
        return minutes + "m " + secs + "s";
    }

    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String formatWorldName(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return "Unknown";
        }
        String result = worldName.replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : result.toCharArray()) {
            if (c == ' ') {
                capitalizeNext = true;
                sb.append(c);
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    public static String formatRelativeTime(long timestamp) {
        if (timestamp == 0) {
            return "Never";
        }
        long diff = System.currentTimeMillis() - timestamp;
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + (days == 1 ? " day ago" : " days ago");
        } else if (hours > 0) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else if (minutes > 0) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else {
            return "Just now";
        }
    }

    public static String formatLimit(int limit) {
        return limit < 0 ? "\u221E" : String.valueOf(limit);
    }

    public static Message parseColorCodes(String text) {
        if (text == null || text.isEmpty()) {
            return Message.raw("");
        }
        Message result = null;
        StringBuilder currentSegment = new StringBuilder();
        String currentColor = "#FFFFFF";
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\u00A7') {
                if (currentSegment.length() > 0) {
                    Message segment = Message.raw(currentSegment.toString()).color(currentColor);
                    result = (result == null) ? segment : result.insert(segment);
                    currentSegment = new StringBuilder();
                }
                if (i + 1 < text.length()) {
                    char colorCode = Character.toLowerCase(text.charAt(i + 1));
                    String newColor = COLOR_MAP.get(colorCode);
                    if (newColor != null) {
                        currentColor = newColor;
                    }
                    i += 2;
                    continue;
                }
            }
            currentSegment.append(c);
            i++;
        }
        if (currentSegment.length() > 0) {
            Message segment = Message.raw(currentSegment.toString()).color(currentColor);
            result = (result == null) ? segment : result.insert(segment);
        }
        return result != null ? result : Message.raw("");
    }
}
