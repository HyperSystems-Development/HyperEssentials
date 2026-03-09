package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a server spawn point — one per world.
 *
 * @param worldUuid the world UUID string (used as key and for resolution)
 * @param worldName the world display name
 * @param x         the x coordinate
 * @param y         the y coordinate
 * @param z         the z coordinate
 * @param yaw       the yaw rotation
 * @param pitch     the pitch rotation
 * @param isGlobal  whether this is the global spawn (replaces isDefault)
 * @param createdAt when the spawn was created (epoch milliseconds)
 * @param createdBy UUID string of the player who created the spawn
 */
public record Spawn(
  @NotNull String worldUuid,
  @NotNull String worldName,
  double x,
  double y,
  double z,
  float yaw,
  float pitch,
  boolean isGlobal,
  long createdAt,
  @Nullable String createdBy
) {
  public static Spawn create(@NotNull String worldUuid, @NotNull String worldName,
                 double x, double y, double z, float yaw, float pitch,
                 @Nullable String createdBy) {
    return new Spawn(worldUuid, worldName, x, y, z, yaw, pitch, false,
            System.currentTimeMillis(), createdBy);
  }

  public Spawn withGlobal(boolean isGlobal) {
    return new Spawn(worldUuid, worldName, x, y, z, yaw, pitch, isGlobal, createdAt, createdBy);
  }

  public Spawn withLocation(double newX, double newY, double newZ,
                float newYaw, float newPitch) {
    return new Spawn(worldUuid, worldName, newX, newY, newZ, newYaw, newPitch, isGlobal, createdAt, createdBy);
  }
}
