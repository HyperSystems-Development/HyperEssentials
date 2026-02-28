package com.hyperessentials.module.kits.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents a kit definition.
 *
 * @param name            unique lowercase identifier
 * @param displayName     display name shown to players
 * @param items           items included in the kit
 * @param cooldownSeconds cooldown between claims (0 = no cooldown)
 * @param oneTime         whether the kit can only be claimed once per player
 * @param permission      custom permission override, or null for default
 */
public record Kit(
  @NotNull String name,
  @NotNull String displayName,
  @NotNull List<KitItem> items,
  int cooldownSeconds,
  boolean oneTime,
  @Nullable String permission
) {
  public Kit {
    if (cooldownSeconds < 0) cooldownSeconds = 0;
    items = List.copyOf(items);
  }

  /**
   * Returns the effective permission node for this kit.
   * Uses custom permission if set, otherwise derives from kit name.
   */
  @NotNull
  public String getEffectivePermission() {
    return permission != null ? permission : "hyperessentials.kit.use." + name;
  }
}
