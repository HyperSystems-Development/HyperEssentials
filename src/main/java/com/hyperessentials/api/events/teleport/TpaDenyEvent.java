package com.hyperessentials.api.events.teleport;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a TPA request has been denied.
 */
public record TpaDenyEvent(@NotNull UUID denierUuid, @NotNull UUID requesterUuid) {}
