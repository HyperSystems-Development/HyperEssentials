package com.hyperessentials.module.announcements;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Simple 5-field cron parser: minute, hour, day-of-month, month, day-of-week.
 * <p>
 * Supports:
 * <ul>
 *   <li>{@code *} — match all</li>
 *   <li>{@code N} — exact value</li>
 *   <li>{@code N-M} — range (inclusive)</li>
 *   <li>{@code N,M,O} — list</li>
 *   <li>{@code *&#47;N} — step (every N)</li>
 * </ul>
 * <p>
 * Example: "0 *&#47;2 * * *" = every 2 hours at minute 0
 */
public class CronScheduler {

  private final int[][] fields; // [5][values], null entry = wildcard

  private CronScheduler(int[][] fields) {
    this.fields = fields;
  }

  /**
   * Parses a 5-field cron expression.
   *
   * @param expression the cron string (e.g. "0 12 * * 1-5")
   * @return a CronScheduler instance, or null if parsing fails
   */
  @Nullable
  public static CronScheduler parse(@NotNull String expression) {
    String[] parts = expression.trim().split("\\s+");
    if (parts.length != 5) {
      return null;
    }

    int[][] fields = new int[5][];
    int[] mins = {0, 0, 1, 1, 0};
    int[] maxs = {59, 23, 31, 12, 6};

    for (int i = 0; i < 5; i++) {
      fields[i] = parseField(parts[i], mins[i], maxs[i]);
      if (fields[i] == null && !"*".equals(parts[i])) {
        return null; // parse error
      }
    }

    return new CronScheduler(fields);
  }

  /**
   * Checks whether the cron expression matches the given instant (to-the-minute precision).
   *
   * @param now the time to check
   * @return true if the cron matches
   */
  public boolean shouldRun(@NotNull Instant now) {
    ZonedDateTime zdt = now.atZone(ZoneId.systemDefault());
    int[] values = {
        zdt.getMinute(),
        zdt.getHour(),
        zdt.getDayOfMonth(),
        zdt.getMonthValue(),
        zdt.getDayOfWeek().getValue() % 7  // Sunday = 0
    };

    for (int i = 0; i < 5; i++) {
      if (fields[i] != null && !contains(fields[i], values[i])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Calculates the next run time after the given instant.
   * Uses brute-force minute scanning (up to 1 year ahead).
   *
   * @param from the start time
   * @return the next matching instant, or null if none found within a year
   */
  @Nullable
  public Instant nextRun(@NotNull Instant from) {
    ZonedDateTime zdt = from.atZone(ZoneId.systemDefault())
        .plusMinutes(1)
        .withSecond(0)
        .withNano(0);

    // Scan up to ~525960 minutes (1 year)
    for (int i = 0; i < 525960; i++) {
      if (shouldRun(zdt.toInstant())) {
        return zdt.toInstant();
      }
      zdt = zdt.plusMinutes(1);
    }
    return null;
  }

  /**
   * Parses a single cron field.
   *
   * @return array of matching values, or null for wildcard (*)
   */
  @Nullable
  private static int[] parseField(@NotNull String field, int min, int max) {
    // Wildcard
    if ("*".equals(field)) {
      return null;
    }

    // Step: */N
    if (field.startsWith("*/")) {
      try {
        int step = Integer.parseInt(field.substring(2));
        if (step <= 0) return null;
        java.util.List<Integer> values = new java.util.ArrayList<>();
        for (int v = min; v <= max; v += step) {
          values.add(v);
        }
        return values.stream().mapToInt(Integer::intValue).toArray();
      } catch (NumberFormatException e) {
        return null;
      }
    }

    // List: N,M,O
    if (field.contains(",")) {
      String[] parts = field.split(",");
      java.util.List<Integer> values = new java.util.ArrayList<>();
      for (String part : parts) {
        int[] parsed = parseField(part.trim(), min, max);
        if (parsed == null) return null;
        for (int v : parsed) values.add(v);
      }
      return values.stream().mapToInt(Integer::intValue).toArray();
    }

    // Range: N-M
    if (field.contains("-")) {
      String[] parts = field.split("-", 2);
      try {
        int start = Integer.parseInt(parts[0].trim());
        int end = Integer.parseInt(parts[1].trim());
        if (start > end || start < min || end > max) return null;
        int[] values = new int[end - start + 1];
        for (int i = 0; i <= end - start; i++) {
          values[i] = start + i;
        }
        return values;
      } catch (NumberFormatException e) {
        return null;
      }
    }

    // Exact value
    try {
      int value = Integer.parseInt(field);
      if (value < min || value > max) return null;
      return new int[]{value};
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static boolean contains(int[] arr, int value) {
    for (int v : arr) {
      if (v == value) return true;
    }
    return false;
  }
}
