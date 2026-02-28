package com.hyperessentials.module.warps;

import com.hyperessentials.data.Warp;
import com.hyperessentials.integration.PermissionManager;
import com.hyperessentials.storage.WarpStorage;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages server warps - loading, saving, and CRUD operations.
 */
public class WarpManager {

  private final WarpStorage storage;
  private final Map<String, Warp> warps;

  public WarpManager(@NotNull WarpStorage storage) {
    this.storage = storage;
    this.warps = new ConcurrentHashMap<>();
  }

  public CompletableFuture<Void> loadWarps() {
    return storage.loadWarps().thenAccept(loaded -> {
      warps.clear();
      warps.putAll(loaded);
      Logger.info("[Warps] Loaded %d warps", warps.size());
    });
  }

  public CompletableFuture<Void> saveWarps() {
    return storage.saveWarps(new ConcurrentHashMap<>(warps));
  }

  public boolean setWarp(@NotNull Warp warp) {
    boolean isNew = !warps.containsKey(warp.name());
    warps.put(warp.name(), warp);
    saveWarps();
    Logger.info("[Warps] Warp '%s' %s", warp.name(), isNew ? "created" : "updated");
    return isNew;
  }

  @Nullable
  public Warp getWarp(@NotNull String name) {
    return warps.get(name.toLowerCase());
  }

  public boolean deleteWarp(@NotNull String name) {
    Warp removed = warps.remove(name.toLowerCase());
    if (removed != null) {
      saveWarps();
      Logger.info("[Warps] Warp '%s' deleted", name);
      return true;
    }
    return false;
  }

  @NotNull
  public Collection<Warp> getAllWarps() {
    return Collections.unmodifiableCollection(warps.values());
  }

  @NotNull
  public List<Warp> getAccessibleWarps(@NotNull UUID playerUuid) {
    return warps.values().stream()
      .filter(warp -> canAccess(playerUuid, warp))
      .collect(Collectors.toList());
  }

  @NotNull
  public List<Warp> getWarpsByCategory(@NotNull String category) {
    return warps.values().stream()
      .filter(warp -> warp.category().equalsIgnoreCase(category))
      .collect(Collectors.toList());
  }

  @NotNull
  public List<Warp> getAccessibleWarpsByCategory(@NotNull UUID playerUuid, @NotNull String category) {
    return warps.values().stream()
      .filter(warp -> warp.category().equalsIgnoreCase(category))
      .filter(warp -> canAccess(playerUuid, warp))
      .collect(Collectors.toList());
  }

  @NotNull
  public Set<String> getCategories() {
    return warps.values().stream()
      .map(Warp::category)
      .collect(Collectors.toCollection(HashSet::new));
  }

  public boolean canAccess(@NotNull UUID playerUuid, @NotNull Warp warp) {
    if (!warp.requiresPermission()) {
      return true;
    }
    return PermissionManager.get().hasPermission(playerUuid, warp.permission());
  }

  public boolean warpExists(@NotNull String name) {
    return warps.containsKey(name.toLowerCase());
  }

  @NotNull
  public List<String> getWarpNames() {
    return new ArrayList<>(warps.keySet());
  }

  @NotNull
  public List<String> getAccessibleWarpNames(@NotNull UUID playerUuid) {
    return warps.values().stream()
      .filter(warp -> canAccess(playerUuid, warp))
      .map(Warp::name)
      .collect(Collectors.toList());
  }

  public int getWarpCount() {
    return warps.size();
  }
}
