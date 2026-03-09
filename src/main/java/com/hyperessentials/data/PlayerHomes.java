package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Holds all homes for a single player, including sharing information.
 * Keys are stored lowercase for case-insensitive lookup;
 * the original casing is preserved in {@link Home#name()}.
 */
public class PlayerHomes {

  private final UUID uuid;
  private String username;
  private final Map<String, Home> homes;
  private String defaultHome;
  private final Map<String, Set<UUID>> shares; // homeName (lowercase) -> sharedWith UUIDs

  public PlayerHomes(@NotNull UUID uuid, @NotNull String username) {
    this.uuid = uuid;
    this.username = username;
    this.homes = new LinkedHashMap<>();
    this.shares = new HashMap<>();
  }

  @NotNull public UUID getUuid() { return uuid; }
  @NotNull public String getUsername() { return username; }
  public void setUsername(@NotNull String username) { this.username = username; }

  @Nullable
  public String getDefaultHome() { return defaultHome; }

  public void setDefaultHome(@Nullable String name) {
    this.defaultHome = name != null ? name.toLowerCase() : null;
  }

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
   * Also removes any shares for the home.
   *
   * @return true if the home existed and was removed
   */
  public boolean removeHome(@NotNull String name) {
    String key = name.toLowerCase();
    shares.remove(key);
    return homes.remove(key) != null;
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

  // === Sharing ===

  /**
   * Returns the set of UUIDs a home is shared with (unmodifiable).
   */
  @NotNull
  public Set<UUID> getShares(@NotNull String homeName) {
    Set<UUID> set = shares.get(homeName.toLowerCase());
    return set != null ? Collections.unmodifiableSet(set) : Set.of();
  }

  /**
   * Shares a home with a target player.
   */
  public void addShare(@NotNull String homeName, @NotNull UUID targetUuid) {
    shares.computeIfAbsent(homeName.toLowerCase(), k -> new HashSet<>()).add(targetUuid);
  }

  /**
   * Removes a share for a home.
   */
  public void removeShare(@NotNull String homeName, @NotNull UUID targetUuid) {
    String key = homeName.toLowerCase();
    Set<UUID> set = shares.get(key);
    if (set != null) {
      set.remove(targetUuid);
      if (set.isEmpty()) {
        shares.remove(key);
      }
    }
  }

  /**
   * Returns all shares for serialization (unmodifiable view).
   */
  @NotNull
  public Map<String, Set<UUID>> getAllShares() {
    return Collections.unmodifiableMap(shares);
  }

  /**
   * Sets shares from deserialized data (replaces all shares).
   */
  public void setShares(@NotNull Map<String, Set<UUID>> sharesData) {
    this.shares.clear();
    sharesData.forEach((key, uuids) -> {
      if (!uuids.isEmpty()) {
        this.shares.put(key.toLowerCase(), new HashSet<>(uuids));
      }
    });
  }
}
