package com.hyperessentials.api.events.utility;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a player's AFK state has been toggled.
 */
public record AfkToggleEvent(@NotNull UUID playerUuid, boolean newState) {}
