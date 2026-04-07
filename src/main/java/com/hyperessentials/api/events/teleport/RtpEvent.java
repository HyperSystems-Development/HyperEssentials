package com.hyperessentials.api.events.teleport;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a random teleport has completed successfully.
 */
public record RtpEvent(@NotNull UUID playerUuid, @NotNull String world,
                       double x, double y, double z) {}
