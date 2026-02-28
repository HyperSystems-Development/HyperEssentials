package com.hyperessentials.util;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class for time formatting.
 */
public final class TimeUtil {

  private TimeUtil() {}

  @NotNull
  public static String formatDuration(long millis) {
    if (millis < 1000) {
      return "0s";
    }

    long seconds = millis / 1000;
    long minutes = seconds / 60;
    long hours = minutes / 60;

    seconds %= 60;
    minutes %= 60;

    StringBuilder sb = new StringBuilder();
    if (hours > 0) {
      sb.append(hours).append("h ");
    }
    if (minutes > 0) {
      sb.append(minutes).append("m ");
    }
    if (seconds > 0 || sb.isEmpty()) {
      sb.append(seconds).append("s");
    }

    return sb.toString().trim();
  }

  @NotNull
  public static String formatSeconds(int seconds) {
    return formatDuration(seconds * 1000L);
  }

  @NotNull
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
}
