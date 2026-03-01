package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Per-player statistics: first join date and accumulated playtime.
 */
public record PlayerStats(
  @NotNull UUID uuid,
  @NotNull String username,
  @NotNull Instant firstJoin,
  long totalPlaytimeMs,
  @NotNull Instant lastJoin
) {}
