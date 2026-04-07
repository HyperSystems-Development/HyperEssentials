package com.hyperessentials.api.events.moderation;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a player is successfully unbanned.
 */
public record PlayerUnbanEvent(@NotNull UUID targetUuid, @NotNull UUID actorUuid) {}
