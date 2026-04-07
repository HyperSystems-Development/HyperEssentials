package com.hyperessentials.api.events.kits;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a kit is successfully deleted.
 */
public record KitDeleteEvent(@NotNull String kitName, @NotNull UUID actorUuid) {}
