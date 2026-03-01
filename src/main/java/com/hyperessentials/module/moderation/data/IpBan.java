package com.hyperessentials.module.moderation.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an IP ban record.
 */
public record IpBan(
  @NotNull String ip,
  @Nullable String reason,
  @Nullable UUID issuerUuid,
  @NotNull String issuerName,
  @NotNull Instant issuedAt,
  @Nullable Instant expiresAt
) {
  public boolean isPermanent() { return expiresAt == null; }
  public boolean hasExpired() { return expiresAt != null && Instant.now().isAfter(expiresAt); }
  public boolean isEffective() { return !hasExpired(); }

  public long getRemainingMillis() {
    if (expiresAt == null) return Long.MAX_VALUE;
    long remaining = expiresAt.toEpochMilli() - System.currentTimeMillis();
    return Math.max(0, remaining);
  }
}
