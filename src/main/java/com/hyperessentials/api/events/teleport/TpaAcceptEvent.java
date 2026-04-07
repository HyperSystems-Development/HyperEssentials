package com.hyperessentials.api.events.teleport;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a TPA request has been accepted successfully.
 */
public record TpaAcceptEvent(@NotNull UUID accepterUuid, @NotNull UUID requesterUuid) {}
