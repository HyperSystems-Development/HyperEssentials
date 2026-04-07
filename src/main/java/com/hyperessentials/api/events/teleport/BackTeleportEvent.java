package com.hyperessentials.api.events.teleport;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a player has teleported back to their previous location.
 */
public record BackTeleportEvent(@NotNull UUID playerUuid, @NotNull String world,
                                double x, double y, double z, @NotNull String source) {}
