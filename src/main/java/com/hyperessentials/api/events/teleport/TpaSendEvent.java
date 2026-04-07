package com.hyperessentials.api.events.teleport;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired after a TPA or TPAHere request has been sent successfully.
 */
public record TpaSendEvent(@NotNull UUID senderUuid, @NotNull UUID targetUuid, boolean here) {}
