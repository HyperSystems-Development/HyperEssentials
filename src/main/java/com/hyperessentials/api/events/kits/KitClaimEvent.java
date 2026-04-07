package com.hyperessentials.api.events.kits;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a player successfully claims a kit.
 */
public record KitClaimEvent(@NotNull UUID playerUuid, @NotNull String kitName) {}
