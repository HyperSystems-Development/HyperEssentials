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
) {}
