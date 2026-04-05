package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a player home location.
 *
 * @param name      the display name of the home (original casing)
 * @param world     the world display name
 * @param worldUuid the world UUID string for resolution
 * @param x         x coordinate
 * @param y         y coordinate
 * @param z         z coordinate
 * @param yaw       yaw rotation
 * @param pitch     pitch rotation
 * @param createdAt when the home was created (epoch milliseconds)
 * @param lastUsed  when the home was last teleported to (epoch milliseconds)
 */
public record Home(
    @NotNull String name,
    @NotNull String world,
    @NotNull String worldUuid,
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    long createdAt,
    long lastUsed
) {
  /**
   * Creates a new home with current timestamps.
   */
  public static Home create(@NotNull String name, @NotNull String world, @NotNull String worldUuid,
                             double x, double y, double z,
                             float yaw, float pitch) {
    long now = System.currentTimeMillis();
    return new Home(name, world, worldUuid, x, y, z, yaw, pitch, now, now);
  }

  /**
   * Returns a copy with updated lastUsed timestamp.
   */
  public Home withLastUsed(long timestamp) {
    return new Home(name, world, worldUuid, x, y, z, yaw, pitch, createdAt, timestamp);
  }

  /**
   * Returns a copy with updated location (for overwriting a home in place).
   */
  public Home withLocation(@NotNull String newWorld, @NotNull String newWorldUuid,
                            double newX, double newY, double newZ,
                            float newYaw, float newPitch) {
    return new Home(name, newWorld, newWorldUuid, newX, newY, newZ, newYaw, newPitch, createdAt, lastUsed);
  }
}
