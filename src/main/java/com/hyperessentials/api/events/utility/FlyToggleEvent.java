package com.hyperessentials.api.events.utility;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a player's fly mode has been toggled.
 */
public record FlyToggleEvent(@NotNull UUID playerUuid, boolean newState) {}
