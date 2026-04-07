package com.hyperessentials.api.events.moderation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired after a player is successfully banned.
 */
public record PlayerBanEvent(@NotNull UUID targetUuid, @NotNull UUID actorUuid,
                             @NotNull String reason, @Nullable Long durationMs) {}
