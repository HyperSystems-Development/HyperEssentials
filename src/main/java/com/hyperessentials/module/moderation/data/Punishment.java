package com.hyperessentials.module.moderation.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a punishment record (ban, mute, or kick).
 */
public record Punishment(
  @NotNull UUID id,
  @NotNull PunishmentType type,
  @NotNull UUID playerUuid,
  @NotNull String playerName,
  @Nullable UUID issuerUuid,
  @NotNull String issuerName,
  @Nullable String reason,
  @NotNull Instant issuedAt,
  @Nullable Instant expiresAt,
  boolean active,
  @Nullable UUID revokedBy,
  @Nullable Instant revokedAt
) {
  public boolean isPermanent() {
    return expiresAt == null;
  }

  public boolean hasExpired() {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }

  /**
   * Returns whether this punishment is currently in effect.
   */
  public boolean isEffective() {
    return active && !hasExpired();
  }

  /**
   * Creates a revoked copy of this punishment.
   */
  @NotNull
  public Punishment revoke(@Nullable UUID revokerUuid, @NotNull String revokerName) {
    return new Punishment(
      id, type, playerUuid, playerName, issuerUuid, issuerName,
      reason, issuedAt, expiresAt, false, revokerUuid, Instant.now()
    );
  }

  /**
   * Returns the remaining time in milliseconds, or 0 if expired/permanent.
   */
  public long getRemainingMillis() {
    if (expiresAt == null) return Long.MAX_VALUE;
    long remaining = expiresAt.toEpochMilli() - System.currentTimeMillis();
    return Math.max(0, remaining);
  }
}
