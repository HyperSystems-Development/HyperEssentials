package com.hyperessentials.api.events.kits;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a new kit is successfully created.
 */
public record KitCreateEvent(@NotNull String kitName, @NotNull UUID actorUuid) {}
