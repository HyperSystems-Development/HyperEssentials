package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Holds all homes for a single player.
 * Keys are stored lowercase for case-insensitive lookup;
 * the original casing is preserved in {@link Home#name()}.
 */
public class PlayerHomes {

  private final UUID uuid;
  private String username;
  private final Map<String, Home> homes;

  public PlayerHomes(@NotNull UUID uuid, @NotNull String username) {
    this.uuid = uuid;
    this.username = username;
    this.homes = new LinkedHashMap<>();
  }

  @NotNull public UUID getUuid() { return uuid; }
  @NotNull public String getUsername() { return username; }
  public void setUsername(@NotNull String username) { this.username = username; }

  /**
   * Gets a home by name (case-insensitive).
   */
  @Nullable
  public Home getHome(@NotNull String name) {
    return homes.get(name.toLowerCase());
  }

  /**
   * Returns all homes (unmodifiable).
   */
  @NotNull
  public Collection<Home> getHomes() {
    return Collections.unmodifiableCollection(homes.values());
  }

  /**
   * Sets (creates or overwrites) a home.
   */
  public void setHome(@NotNull Home home) {
    homes.put(home.name().toLowerCase(), home);
  }

  /**
   * Removes a home by name (case-insensitive).
   *
   * @return true if the home existed and was removed
   */
  public boolean removeHome(@NotNull String name) {
    return homes.remove(name.toLowerCase()) != null;
  }

  /**
   * Checks if a home exists (case-insensitive).
   */
  public boolean hasHome(@NotNull String name) {
    return homes.containsKey(name.toLowerCase());
  }

  /**
   * Returns the number of homes.
   */
  public int count() {
    return homes.size();
  }
}
