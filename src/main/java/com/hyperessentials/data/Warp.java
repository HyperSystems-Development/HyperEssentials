package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a server warp location that players can teleport to.
 *
 * @param uuid        unique identifier for file naming
 * @param name        the unique name of the warp (lowercase)
 * @param displayName the display name of the warp (can have formatting)
 * @param category    the category/group this warp belongs to
 * @param world       the world display name
 * @param worldUuid   the world UUID string for resolution
 * @param x           the x coordinate
 * @param y           the y coordinate
 * @param z           the z coordinate
 * @param yaw         the yaw rotation
 * @param pitch       the pitch rotation
 * @param permission  optional permission required to use this warp (null = no permission)
 * @param description optional description of the warp
 * @param createdAt   when the warp was created (epoch milliseconds)
 * @param createdBy   UUID string of the player who created the warp
 */
public record Warp(
  @NotNull UUID uuid,
  @NotNull String name,
  @NotNull String displayName,
  @NotNull String category,
  @NotNull String world,
  @NotNull String worldUuid,
  double x,
  double y,
  double z,
  float yaw,
  float pitch,
  @Nullable String permission,
  @Nullable String description,
  long createdAt,
  @Nullable String createdBy
) {
  public Warp {
    name = name.toLowerCase();
    if (displayName == null || displayName.isEmpty()) {
      displayName = name;
    }
    if (category == null || category.isEmpty()) {
      category = "general";
    }
  }

  public static Warp create(@NotNull String name, @NotNull String world, @NotNull String worldUuid,
                double x, double y, double z, float yaw, float pitch,
                @Nullable String createdBy) {
    return new Warp(
      UUID.randomUUID(),
      name.toLowerCase(),
      name,
      "general",
      world,
      worldUuid,
      x, y, z,
      yaw, pitch,
      null,
      null,
      System.currentTimeMillis(),
      createdBy
    );
  }

  public Warp withDisplayName(@NotNull String newDisplayName) {
    return new Warp(uuid, name, newDisplayName, category, world, worldUuid, x, y, z, yaw, pitch,
             permission, description, createdAt, createdBy);
  }

  public Warp withCategory(@NotNull String newCategory) {
    return new Warp(uuid, name, displayName, newCategory, world, worldUuid, x, y, z, yaw, pitch,
             permission, description, createdAt, createdBy);
  }

  public Warp withPermission(@Nullable String newPermission) {
    return new Warp(uuid, name, displayName, category, world, worldUuid, x, y, z, yaw, pitch,
             newPermission, description, createdAt, createdBy);
  }

  public Warp withDescription(@Nullable String newDescription) {
    return new Warp(uuid, name, displayName, category, world, worldUuid, x, y, z, yaw, pitch,
             permission, newDescription, createdAt, createdBy);
  }

  public Warp withLocation(@NotNull String newWorld, @NotNull String newWorldUuid,
               double newX, double newY, double newZ,
               float newYaw, float newPitch) {
    return new Warp(uuid, name, displayName, category, newWorld, newWorldUuid, newX, newY, newZ, newYaw, newPitch,
             permission, description, createdAt, createdBy);
  }

  public boolean requiresPermission() {
    return permission != null && !permission.isEmpty();
  }
}
