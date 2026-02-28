package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a server spawn point that players can teleport to.
 *
 * @param name            the unique name of the spawn (lowercase)
 * @param world           the world the spawn is in
 * @param x               the x coordinate
 * @param y               the y coordinate
 * @param z               the z coordinate
 * @param yaw             the yaw rotation
 * @param pitch           the pitch rotation
 * @param permission      optional permission required to use this spawn (null = no permission)
 * @param groupPermission optional group-based permission for auto-selection (null = available to all)
 * @param isDefault       whether this is the default spawn
 * @param createdAt       when the spawn was created (epoch milliseconds)
 * @param createdBy       UUID string of the player who created the spawn
 */
public record Spawn(
  @NotNull String name,
  @NotNull String world,
  double x,
  double y,
  double z,
  float yaw,
  float pitch,
  @Nullable String permission,
  @Nullable String groupPermission,
  boolean isDefault,
  long createdAt,
  @Nullable String createdBy
) {
  public Spawn {
    name = name.toLowerCase();
  }

  public static Spawn create(@NotNull String name, @NotNull String world,
                 double x, double y, double z, float yaw, float pitch,
                 @Nullable String createdBy) {
    return new Spawn(
      name.toLowerCase(),
      world,
      x, y, z,
      yaw, pitch,
      null,
      null,
      false,
      System.currentTimeMillis(),
      createdBy
    );
  }

  public Spawn withDefault(boolean isDefault) {
    return new Spawn(name, world, x, y, z, yaw, pitch, permission, groupPermission,
            isDefault, createdAt, createdBy);
  }

  public Spawn withPermission(@Nullable String newPermission) {
    return new Spawn(name, world, x, y, z, yaw, pitch, newPermission, groupPermission,
            isDefault, createdAt, createdBy);
  }

  public Spawn withGroupPermission(@Nullable String newGroupPermission) {
    return new Spawn(name, world, x, y, z, yaw, pitch, permission, newGroupPermission,
            isDefault, createdAt, createdBy);
  }

  public Spawn withLocation(@NotNull String newWorld, double newX, double newY, double newZ,
                float newYaw, float newPitch) {
    return new Spawn(name, newWorld, newX, newY, newZ, newYaw, newPitch, permission, groupPermission,
            isDefault, createdAt, createdBy);
  }

  public boolean requiresPermission() {
    return permission != null && !permission.isEmpty();
  }

  public boolean isGroupRestricted() {
    return groupPermission != null && !groupPermission.isEmpty();
  }
}
