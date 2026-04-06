package com.hyperessentials.module.homes;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.HomesConfig;
import com.hyperessentials.data.Home;
import com.hyperessentials.data.PlayerHomes;
import com.hyperessentials.storage.HomeStorage;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages player homes - loading, saving, CRUD operations, limit enforcement, and sharing.
 * Shares are stored inline in each player's homes file (no separate home_shares.json).
 * A reverse index maps viewer UUID -> list of (owner, homeName) for fast lookup.
 */
public class HomeManager {

  private final HomeStorage storage;
  private final Map<UUID, PlayerHomes> cache = new ConcurrentHashMap<>();

  // Reverse share index: viewer UUID -> list of SharedHomeRef (owner + homeName)
  private final Map<UUID, List<SharedHomeRef>> reverseShareIndex = new ConcurrentHashMap<>();

  @Nullable private Consumer<UUID> onHomeChanged;

  public HomeManager(@NotNull HomeStorage storage) {
    this.storage = storage;
  }

  public void setOnHomeChanged(@Nullable Consumer<UUID> callback) {
    this.onHomeChanged = callback;
  }

  private void fireHomeChanged(@NotNull UUID uuid) {
    if (onHomeChanged != null) {
      try { onHomeChanged.accept(uuid); } catch (Exception e) {
        ErrorHandler.report("[Homes] Error in home-changed callback", e);
      }
    }
  }

  // ========== Lifecycle ==========

  /**
   * Initializes the reverse share index by scanning all home files at startup.
   */
  public CompletableFuture<Void> initShares() {
    return storage.scanAllShares().thenAccept(allShares -> {
      reverseShareIndex.clear();
      for (Map.Entry<UUID, Map<String, Set<UUID>>> ownerEntry : allShares.entrySet()) {
        UUID ownerUuid = ownerEntry.getKey();
        for (Map.Entry<String, Set<UUID>> homeEntry : ownerEntry.getValue().entrySet()) {
          String homeName = homeEntry.getKey();
          for (UUID viewerUuid : homeEntry.getValue()) {
            reverseShareIndex.computeIfAbsent(viewerUuid, k -> new ArrayList<>())
                .add(new SharedHomeRef(ownerUuid, homeName));
          }
        }
      }
      Logger.info("[Homes] Initialized reverse share index: %d viewer(s)", reverseShareIndex.size());
    });
  }

  /**
   * Loads a player's homes from storage into the cache.
   */
  public CompletableFuture<Void> loadPlayer(@NotNull UUID uuid, @NotNull String username) {
    return storage.loadPlayerHomes(uuid).thenAccept(opt -> {
      PlayerHomes homes = opt.orElseGet(() -> new PlayerHomes(uuid, username));
      homes.setUsername(username);
      cache.put(uuid, homes);

      // Register shares from loaded data into reverse index
      for (Map.Entry<String, Set<UUID>> entry : homes.getAllShares().entrySet()) {
        String homeName = entry.getKey();
        for (UUID viewerUuid : entry.getValue()) {
          reverseShareIndex.computeIfAbsent(viewerUuid, k -> new ArrayList<>())
              .add(new SharedHomeRef(uuid, homeName));
        }
      }

      Logger.debug("[Homes] Loaded %d homes for %s", homes.count(), username);
    });
  }

  /**
   * Saves a player's homes and removes from cache.
   */
  public CompletableFuture<Void> unloadPlayer(@NotNull UUID uuid) {
    PlayerHomes homes = cache.remove(uuid);
    if (homes != null) {
      // Remove this player's shares from reverse index
      for (Map.Entry<String, Set<UUID>> entry : homes.getAllShares().entrySet()) {
        for (UUID viewerUuid : entry.getValue()) {
          List<SharedHomeRef> refs = reverseShareIndex.get(viewerUuid);
          if (refs != null) {
            refs.removeIf(ref -> ref.ownerUuid().equals(uuid));
            if (refs.isEmpty()) reverseShareIndex.remove(viewerUuid);
          }
        }
      }
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

  public boolean setHome(@NotNull UUID uuid, @NotNull Home home) {
    PlayerHomes homes = cache.get(uuid);
    if (homes == null) return false;

    if (!homes.hasHome(home.name())) {
      if (isAtLimit(uuid)) return false;
    }

    homes.setHome(home);
    savePlayer(uuid);
    fireHomeChanged(uuid);
    Logger.debug("[Homes] Set home '%s' for %s", home.name(), uuid);
    return true;
  }

  public boolean deleteHome(@NotNull UUID uuid, @NotNull String name) {
    PlayerHomes homes = cache.get(uuid);
    if (homes == null) return false;

    // Get shares before removing (for reverse index cleanup)
    Set<UUID> sharedWith = homes.getShares(name);

    boolean removed = homes.removeHome(name); // also removes shares
    if (removed) {
      String defaultName = homes.getDefaultHome();
      if (defaultName != null && defaultName.equalsIgnoreCase(name)) {
        homes.setDefaultHome(null);
      }

      // Clean reverse index for removed shares
      for (UUID viewerUuid : sharedWith) {
        List<SharedHomeRef> refs = reverseShareIndex.get(viewerUuid);
        if (refs != null) {
          refs.removeIf(ref -> ref.ownerUuid().equals(uuid) && ref.homeName().equalsIgnoreCase(name));
          if (refs.isEmpty()) reverseShareIndex.remove(viewerUuid);
        }
      }

      savePlayer(uuid);
      fireHomeChanged(uuid);
      Logger.debug("[Homes] Deleted home '%s' for %s", name, uuid);
    }
    return removed;
  }

  @Nullable
  public Home getHome(@NotNull UUID uuid, @NotNull String name) {
    PlayerHomes homes = cache.get(uuid);
    return homes != null ? homes.getHome(name) : null;
  }

  @NotNull
  public Collection<Home> getHomes(@NotNull UUID uuid) {
    PlayerHomes homes = cache.get(uuid);
    return homes != null ? homes.getHomes() : Collections.emptyList();
  }

  // ========== Limits ==========

  public int getHomeLimit(@NotNull UUID uuid) {
    if (CommandUtil.hasPermission(uuid, Permissions.HOME_UNLIMITED)) return -1;
    if (CommandUtil.hasPermission(uuid, Permissions.BYPASS_LIMIT)) return -1;

    int permLimit = CommandUtil.getPermissionValue(uuid, Permissions.HOME_LIMIT_PREFIX, -1);
    if (permLimit > 0) return permLimit;

    return ConfigManager.get().homes().getDefaultHomeLimit();
  }

  public boolean isAtLimit(@NotNull UUID uuid) {
    int limit = getHomeLimit(uuid);
    if (limit < 0) return false;
    PlayerHomes homes = cache.get(uuid);
    return homes != null && homes.count() >= limit;
  }

  public int getHomeCount(@NotNull UUID uuid) {
    PlayerHomes homes = cache.get(uuid);
    return homes != null ? homes.count() : 0;
  }

  // ========== Default Home ==========

  @Nullable
  public String getDefaultHome(@NotNull UUID uuid) {
    PlayerHomes homes = cache.get(uuid);
    return homes != null ? homes.getDefaultHome() : null;
  }

  public void setDefaultHome(@NotNull UUID uuid, @Nullable String name) {
    PlayerHomes homes = cache.get(uuid);
    if (homes == null) return;
    homes.setDefaultHome(name);
    savePlayer(uuid);
    fireHomeChanged(uuid);
    Logger.debug("[Homes] Set default home to '%s' for %s", name, uuid);
  }

  @Nullable
  public Home resolveDefaultHome(@NotNull UUID uuid) {
    PlayerHomes playerHomes = cache.get(uuid);
    if (playerHomes == null) return null;

    String defaultName = playerHomes.getDefaultHome();
    if (defaultName != null) {
      Home home = playerHomes.getHome(defaultName);
      if (home != null) return home;
      // Default home was deleted — clear the stale reference
      playerHomes.setDefaultHome(null);
      savePlayer(uuid);
    }

    return null;
  }

  // ========== Bed Sync ==========

  public void syncBedHome(@NotNull UUID uuid, @NotNull String world, @NotNull String worldUuid,
                          double x, double y, double z, float yaw, float pitch) {
    PlayerHomes homes = cache.get(uuid);
    if (homes == null) return;

    HomesConfig config = ConfigManager.get().homes();
    if (!config.isBedSyncEnabled()) return;

    String bedName = config.getBedHomeName();
    Home bedHome = Home.create(bedName, world, worldUuid, x, y, z, yaw, pitch);
    homes.setHome(bedHome);
    savePlayer(uuid);
    fireHomeChanged(uuid);
    Logger.debug("[Homes] Synced bed home for %s at %.0f, %.0f, %.0f", uuid, x, y, z);
  }

  // ========== Sharing ==========

  public void shareHome(@NotNull UUID ownerUuid, @NotNull String homeName, @NotNull UUID targetUuid) {
    PlayerHomes homes = cache.get(ownerUuid);
    if (homes == null) return;

    homes.addShare(homeName, targetUuid);
    reverseShareIndex.computeIfAbsent(targetUuid, k -> new ArrayList<>())
        .add(new SharedHomeRef(ownerUuid, homeName.toLowerCase()));

    savePlayer(ownerUuid);
    fireHomeChanged(ownerUuid);
    fireHomeChanged(targetUuid);
    Logger.debug("[Homes] %s shared home '%s' with %s", ownerUuid, homeName, targetUuid);
  }

  public void unshareHome(@NotNull UUID ownerUuid, @NotNull String homeName, @NotNull UUID targetUuid) {
    PlayerHomes homes = cache.get(ownerUuid);
    if (homes == null) return;

    homes.removeShare(homeName, targetUuid);

    List<SharedHomeRef> refs = reverseShareIndex.get(targetUuid);
    if (refs != null) {
      refs.removeIf(ref -> ref.ownerUuid().equals(ownerUuid) && ref.homeName().equalsIgnoreCase(homeName));
      if (refs.isEmpty()) reverseShareIndex.remove(targetUuid);
    }

    savePlayer(ownerUuid);
    fireHomeChanged(ownerUuid);
    fireHomeChanged(targetUuid);
    Logger.debug("[Homes] %s unshared home '%s' from %s", ownerUuid, homeName, targetUuid);
  }

  @NotNull
  public Set<UUID> getSharedWith(@NotNull UUID ownerUuid, @NotNull String homeName) {
    PlayerHomes homes = cache.get(ownerUuid);
    return homes != null ? homes.getShares(homeName) : Set.of();
  }

  /**
   * Gets all homes shared TO a player (from other players).
   * Uses the reverse index to find shares, then loads owner files on-demand for offline owners.
   */
  @NotNull
  public List<SharedHome> getSharedHomes(@NotNull UUID playerUuid) {
    List<SharedHomeRef> refs = reverseShareIndex.get(playerUuid);
    if (refs == null || refs.isEmpty()) return List.of();

    List<SharedHome> result = new ArrayList<>();
    for (SharedHomeRef ref : refs) {
      Home home = getHome(ref.ownerUuid(), ref.homeName());
      if (home != null) {
        result.add(new SharedHome(ref.ownerUuid(), home));
      }
    }
    return result;
  }

  public boolean isSharedWith(@NotNull UUID ownerUuid, @NotNull String homeName, @NotNull UUID targetUuid) {
    PlayerHomes homes = cache.get(ownerUuid);
    return homes != null && homes.getShares(homeName).contains(targetUuid);
  }

  /** A home shared from another player. */
  public record SharedHome(@NotNull UUID ownerUuid, @NotNull Home home) {}

  /** Reference to a shared home (owner + home name) for the reverse index. */
  private record SharedHomeRef(@NotNull UUID ownerUuid, @NotNull String homeName) {}
}
