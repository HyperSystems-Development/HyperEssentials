package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a server warp location that players can teleport to.
 *
 * @param name        the unique name of the warp (lowercase)
 * @param displayName the display name of the warp (can have formatting)
 * @param category    the category/group this warp belongs to
 * @param world       the world the warp is in
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
    @NotNull String name,
    @NotNull String displayName,
    @NotNull String category,
    @NotNull String world,
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

    public static Warp create(@NotNull String name, @NotNull String world,
                              double x, double y, double z, float yaw, float pitch,
                              @Nullable String createdBy) {
        return new Warp(
            name.toLowerCase(),
            name,
            "general",
            world,
            x, y, z,
            yaw, pitch,
            null,
            null,
            System.currentTimeMillis(),
            createdBy
        );
    }

    public Warp withDisplayName(@NotNull String newDisplayName) {
        return new Warp(name, newDisplayName, category, world, x, y, z, yaw, pitch,
                       permission, description, createdAt, createdBy);
    }

    public Warp withCategory(@NotNull String newCategory) {
        return new Warp(name, displayName, newCategory, world, x, y, z, yaw, pitch,
                       permission, description, createdAt, createdBy);
    }

    public Warp withPermission(@Nullable String newPermission) {
        return new Warp(name, displayName, category, world, x, y, z, yaw, pitch,
                       newPermission, description, createdAt, createdBy);
    }

    public Warp withDescription(@Nullable String newDescription) {
        return new Warp(name, displayName, category, world, x, y, z, yaw, pitch,
                       permission, newDescription, createdAt, createdBy);
    }

    public Warp withLocation(@NotNull String newWorld, double newX, double newY, double newZ,
                             float newYaw, float newPitch) {
        return new Warp(name, displayName, category, newWorld, newX, newY, newZ, newYaw, newPitch,
                       permission, description, createdAt, createdBy);
    }

    public boolean requiresPermission() {
        return permission != null && !permission.isEmpty();
    }
}
