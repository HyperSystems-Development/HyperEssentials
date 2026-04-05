package com.hyperessentials.module.kits.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Represents a kit definition.
 *
 * @param uuid            unique identifier for file naming
 * @param name            unique lowercase identifier
 * @param displayName     display name shown to players
 * @param items           items included in the kit
 * @param cooldownSeconds cooldown between claims (0 = no cooldown)
 * @param oneTime         whether the kit can only be claimed once per player
 * @param permission      custom permission override, or null for default
 */
public record Kit(
  @NotNull UUID uuid,
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

  /** Returns a copy with the given display name. */
  @NotNull
  public Kit withDisplayName(@NotNull String newDisplayName) {
    return new Kit(uuid, name, newDisplayName, items, cooldownSeconds, oneTime, permission);
  }

  /** Returns a copy with the given cooldown. */
  @NotNull
  public Kit withCooldownSeconds(int newCooldown) {
    return new Kit(uuid, name, displayName, items, newCooldown, oneTime, permission);
  }

  /** Returns a copy with the given one-time flag. */
  @NotNull
  public Kit withOneTime(boolean newOneTime) {
    return new Kit(uuid, name, displayName, items, cooldownSeconds, newOneTime, permission);
  }

  /** Returns a copy with the given permission. */
  @NotNull
  public Kit withPermission(@Nullable String newPermission) {
    return new Kit(uuid, name, displayName, items, cooldownSeconds, oneTime, newPermission);
  }

  /** Returns a copy with the given items list. */
  @NotNull
  public Kit withItems(@NotNull List<KitItem> newItems) {
    return new Kit(uuid, name, displayName, newItems, cooldownSeconds, oneTime, permission);
  }
}
