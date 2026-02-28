package com.hyperessentials.module.homes;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.HomesConfig;
import com.hyperessentials.data.Home;
import com.hyperessentials.data.PlayerHomes;
import com.hyperessentials.storage.HomeStorage;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player homes - loading, saving, CRUD operations, and limit enforcement.
 */
public class HomeManager {

  private final HomeStorage storage;
  private final Map<UUID, PlayerHomes> cache = new ConcurrentHashMap<>();

  public HomeManager(@NotNull HomeStorage storage) {
    this.storage = storage;
  }

  // ========== Lifecycle ==========

  /**
   * Loads a player's homes from storage into the cache.
   * Called on player connect.
   */
  public CompletableFuture<Void> loadPlayer(@NotNull UUID uuid, @NotNull String username) {
    return storage.loadPlayerHomes(uuid).thenAccept(opt -> {
      PlayerHomes homes = opt.orElseGet(() -> new PlayerHomes(uuid, username));
      homes.setUsername(username);
      cache.put(uuid, homes);
      Logger.debug("[Homes] Loaded %d homes for %s", homes.count(), username);
    });
  }

  /**
   * Saves a player's homes and removes from cache.
   * Called on player disconnect.
   */
  public CompletableFuture<Void> unloadPlayer(@NotNull UUID uuid) {
    PlayerHomes homes = cache.remove(uuid);
    if (homes != null) {
      return storage.savePlayerHomes(homes);
    }
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Saves a player's homes without removing from cache.
   */
  public CompletableFuture<Void> savePlayer(@NotNull UUID uuid) {
    PlayerHomes homes = cache.get(uuid);
    if (homes != null) {
      return storage.savePlayerHomes(homes);
    }
    return CompletableFuture.completedFuture(null);
  }

  /**
   * Saves all cached players to storage. Called on shutdown.
   */
  public CompletableFuture<Void> saveAll() {
    CompletableFuture<?>[] futures = cache.values().stream()
        .map(storage::savePlayerHomes)
        .toArray(CompletableFuture[]::new);
    return CompletableFuture.allOf(futures);
  }

  // ========== CRUD ==========

  /**
   * Sets a home for a player. Checks home limit unless bypassed.
   *
   * @return true if the home was set, false if at limit
   */
  public boolean setHome(@NotNull UUID uuid, @NotNull Home home) {
    PlayerHomes homes = cache.get(uuid);
    if (homes == null) return false;

    // If home already exists (overwrite), always allow
    if (!homes.hasHome(home.name())) {
      // New home — check limit
      if (isAtLimit(uuid)) {
        return false;
      }
    }

    homes.setHome(home);
    savePlayer(uuid);
    Logger.debug("[Homes] Set home '%s' for %s", home.name(), uuid);
    return true;
  }

  /**
   * Deletes a home for a player.
   *
   * @return true if the home existed and was deleted
   */
  public boolean deleteHome(@NotNull UUID uuid, @NotNull String name) {
    PlayerHomes homes = cache.get(uuid);
    if (homes == null) return false;

    boolean removed = homes.removeHome(name);
    if (removed) {
      savePlayer(uuid);
      Logger.debug("[Homes] Deleted home '%s' for %s", name, uuid);
    }
    return removed;
  }

  /**
   * Gets a specific home (case-insensitive).
   */
  @Nullable
  public Home getHome(@NotNull UUID uuid, @NotNull String name) {
    PlayerHomes homes = cache.get(uuid);
    return homes != null ? homes.getHome(name) : null;
  }

  /**
   * Gets all homes for a player.
   */
  @NotNull
  public Collection<Home> getHomes(@NotNull UUID uuid) {
    PlayerHomes homes = cache.get(uuid);
    return homes != null ? homes.getHomes() : Collections.emptyList();
  }

  // ========== Limits ==========

  /**
   * Gets the home limit for a player.
   * Checks: unlimited perm → bypass.limit perm → home.limit.N perm → config default.
   *
   * @return the limit, or -1 for unlimited
   */
  public int getHomeLimit(@NotNull UUID uuid) {
    if (CommandUtil.hasPermission(uuid, Permissions.HOME_UNLIMITED)) {
      return -1;
    }
    if (CommandUtil.hasPermission(uuid, Permissions.BYPASS_LIMIT)) {
      return -1;
    }

    int permLimit = CommandUtil.getPermissionValue(uuid, Permissions.HOME_LIMIT_PREFIX, -1);
    if (permLimit > 0) {
      return permLimit;
    }

    return ConfigManager.get().homes().getDefaultHomeLimit();
  }

  /**
   * Checks if a player is at their home limit.
   */
  public boolean isAtLimit(@NotNull UUID uuid) {
    int limit = getHomeLimit(uuid);
    if (limit < 0) return false; // unlimited
    PlayerHomes homes = cache.get(uuid);
    return homes != null && homes.count() >= limit;
  }

  /**
   * Gets the current home count for a player.
   */
  public int getHomeCount(@NotNull UUID uuid) {
    PlayerHomes homes = cache.get(uuid);
    return homes != null ? homes.count() : 0;
  }

  // ========== Bed Sync ==========

  /**
   * Syncs a bed location as a home, bypassing the home limit.
   * Uses the configured bed home name.
   */
  public void syncBedHome(@NotNull UUID uuid, @NotNull String world,
                          double x, double y, double z, float yaw, float pitch) {
    PlayerHomes homes = cache.get(uuid);
    if (homes == null) return;

    HomesConfig config = ConfigManager.get().homes();
    if (!config.isBedSyncEnabled()) return;

    String bedName = config.getBedHomeName();
    Home bedHome = Home.create(bedName, world, x, y, z, yaw, pitch);
    homes.setHome(bedHome);
    savePlayer(uuid);
    Logger.debug("[Homes] Synced bed home for %s at %.0f, %.0f, %.0f", uuid, x, y, z);
  }
}
