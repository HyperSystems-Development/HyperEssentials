package com.hyperessentials.integration;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface for permission providers.
 */
public interface PermissionProvider {

  @NotNull
  String getName();

  boolean isAvailable();

  @NotNull
  Optional<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission);

  int getPermissionValue(@NotNull UUID playerUuid, @NotNull String prefix, int defaultValue);

  @NotNull
  String getPrimaryGroup(@NotNull UUID playerUuid);
}
