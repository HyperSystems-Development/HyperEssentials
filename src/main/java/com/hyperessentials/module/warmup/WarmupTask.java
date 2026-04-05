package com.hyperessentials.module.warmup;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents an active warmup for a player.
 */
public record WarmupTask(
  @NotNull UUID playerUuid,
  @NotNull String moduleName,
  @NotNull String commandName,
  int warmupSeconds,
  @NotNull Runnable callback
) {}
