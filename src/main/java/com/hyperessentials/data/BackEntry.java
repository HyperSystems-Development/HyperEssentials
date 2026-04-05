package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;

/**
 * A back history entry wrapping a location with source type and timestamp.
 *
 * @param location  the saved location
 * @param source    the source type (e.g. "death", "home", "warp", "spawn", "rtp", "tpa", "factionhome")
 * @param timestamp when the entry was created (epoch millis)
 */
public record BackEntry(
  @NotNull Location location,
  @NotNull String source,
  long timestamp
) {

  /** Source types for back history entries. */
  public static final String SOURCE_DEATH = "death";
  public static final String SOURCE_HOME = "home";
  public static final String SOURCE_WARP = "warp";
  public static final String SOURCE_SPAWN = "spawn";
  public static final String SOURCE_RTP = "rtp";
  public static final String SOURCE_TPA = "tpa";
  public static final String SOURCE_FACTION_HOME = "factionhome";
  public static final String SOURCE_UNKNOWN = "unknown";

  /**
   * Creates a BackEntry with the current timestamp.
   */
  public static BackEntry of(@NotNull Location location, @NotNull String source) {
    return new BackEntry(location, source, System.currentTimeMillis());
  }

  /**
   * Returns a display label for the source type.
   */
  @NotNull
  public String sourceLabel() {
    return switch (source) {
      case SOURCE_DEATH -> "Death";
      case SOURCE_HOME -> "Home Teleport";
      case SOURCE_WARP -> "Warp Teleport";
      case SOURCE_SPAWN -> "Spawn Teleport";
      case SOURCE_RTP -> "RTP";
      case SOURCE_TPA -> "TPA";
      case SOURCE_FACTION_HOME -> "Faction Home";
      default -> "Teleport";
    };
  }
}
