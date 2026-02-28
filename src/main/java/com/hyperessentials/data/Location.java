package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;

/**
 * A simple location record for teleportation and storage.
 *
 * @param world the world name
 * @param x     x coordinate
 * @param y     y coordinate
 * @param z     z coordinate
 * @param yaw   yaw rotation
 * @param pitch pitch rotation
 */
public record Location(
  @NotNull String world,
  double x,
  double y,
  double z,
  float yaw,
  float pitch
) {
  public static Location fromWarp(@NotNull Warp warp) {
    return new Location(warp.world(), warp.x(), warp.y(), warp.z(), warp.yaw(), warp.pitch());
  }

  public static Location fromSpawn(@NotNull Spawn spawn) {
    return new Location(spawn.world(), spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
  }

  public static Location fromHome(@NotNull Home home) {
    return new Location(home.world(), home.x(), home.y(), home.z(), home.yaw(), home.pitch());
  }

  public double distanceSquared(@NotNull Location other) {
    if (!world.equals(other.world)) {
      return Double.MAX_VALUE;
    }
    double dx = x - other.x;
    double dy = y - other.y;
    double dz = z - other.z;
    return dx * dx + dy * dy + dz * dz;
  }
}
