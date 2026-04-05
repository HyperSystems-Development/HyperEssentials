package com.hyperessentials.integration;

import org.jetbrains.annotations.NotNull;

/**
 * Hook for world border enforcement in RTP.
 * Default implementation allows all locations.
 * When HyperBorder is built, it registers via {@code rtpManager.setBorderHook(hook)}.
 */
@FunctionalInterface
public interface BorderHook {

  /**
   * Checks whether the given coordinates are within the world border.
   *
   * @param worldName the world name
   * @param x         world X coordinate
   * @param z         world Z coordinate
   * @return true if within border, false if outside
   */
  boolean isWithinBorder(@NotNull String worldName, double x, double z);
}
