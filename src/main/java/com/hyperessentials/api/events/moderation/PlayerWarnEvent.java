package com.hyperessentials.api.events.moderation;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a player is successfully warned.
 */
public record PlayerWarnEvent(@NotNull UUID targetUuid, @NotNull UUID actorUuid,
                              @NotNull String reason) {}
