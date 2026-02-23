package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a TPA (teleport ask) request between players.
 *
 * @param requester  the UUID of the player who sent the request
 * @param target     the UUID of the player receiving the request
 * @param type       the type of request (TPA or TPAHERE)
 * @param createdAt  when the request was created (epoch milliseconds)
 * @param expiresAt  when the request expires (epoch milliseconds)
 */
public record TeleportRequest(
    @NotNull UUID requester,
    @NotNull UUID target,
    @NotNull Type type,
    long createdAt,
    long expiresAt
) {
    public enum Type {
        /** Requester wants to teleport TO the target. */
        TPA,
        /** Requester wants the target to teleport TO them. */
        TPAHERE
    }

    public static TeleportRequest create(@NotNull UUID requester, @NotNull UUID target,
                                         @NotNull Type type, int timeoutSecs) {
        long now = System.currentTimeMillis();
        return new TeleportRequest(requester, target, type, now, now + (timeoutSecs * 1000L));
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public long getRemainingTime() {
        return Math.max(0, expiresAt - System.currentTimeMillis());
    }

    @NotNull
    public UUID getTeleportingPlayer() {
        return type == Type.TPA ? requester : target;
    }

    @NotNull
    public UUID getDestinationPlayer() {
        return type == Type.TPA ? target : requester;
    }
}
