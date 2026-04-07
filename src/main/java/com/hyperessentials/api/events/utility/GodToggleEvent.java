package com.hyperessentials.api.events.utility;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a player's god mode has been toggled.
 */
public record GodToggleEvent(@NotNull UUID playerUuid, boolean newState) {}
