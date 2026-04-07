package com.hyperessentials.api.events.moderation;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a player is successfully kicked.
 */
public record PlayerKickEvent(@NotNull UUID targetUuid, @NotNull UUID actorUuid,
                              @NotNull String reason) {}
