package com.hyperessentials.api;

import com.hyperessentials.HyperEssentials;
import com.hyperessentials.data.Location;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.data.Warp;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hyperessentials.module.spawns.SpawnsModule;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.module.warps.WarpManager;
import com.hyperessentials.module.warps.WarpsModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Public API for HyperEssentials.
 * Other plugins can use this to interact with HyperEssentials modules.
 */
public final class HyperEssentialsAPI {

  private static HyperEssentials instance;

  private HyperEssentialsAPI() {}

  public static void setInstance(@Nullable HyperEssentials instance) {
    HyperEssentialsAPI.instance = instance;
  }

  @Nullable
  public static HyperEssentials getInstance() {
    return instance;
  }

  public static boolean isAvailable() {
    return instance != null;
  }

  // ========== Warp API ==========

  @Nullable
  public static Warp getWarp(@NotNull String name) {
    WarpManager wm = getWarpManager();
    return wm != null ? wm.getWarp(name) : null;
  }

  @NotNull
  public static Collection<Warp> getAllWarps() {
    WarpManager wm = getWarpManager();
    return wm != null ? wm.getAllWarps() : Collections.emptyList();
  }

  @NotNull
  public static List<Warp> getAccessibleWarps(@NotNull UUID uuid) {
    WarpManager wm = getWarpManager();
    return wm != null ? wm.getAccessibleWarps(uuid) : Collections.emptyList();
  }

  // ========== Spawn API ==========

  @Nullable
  public static Spawn getSpawnForWorld(@NotNull String worldUuid) {
    SpawnManager sm = getSpawnManager();
    return sm != null ? sm.getSpawnForWorld(worldUuid) : null;
  }

  @Nullable
  public static Spawn getGlobalSpawn() {
    SpawnManager sm = getSpawnManager();
    return sm != null ? sm.getGlobalSpawn() : null;
  }

  @NotNull
  public static Collection<Spawn> getAllSpawns() {
    SpawnManager sm = getSpawnManager();
    return sm != null ? sm.getAllSpawns() : Collections.emptyList();
  }

  // ========== Back API ==========

  public static void saveBackLocation(@NotNull UUID uuid, @NotNull Location location) {
    BackManager bm = getBackManager();
    if (bm != null) {
      bm.saveBackLocation(uuid, location);
    }
  }

  public static boolean hasBackHistory(@NotNull UUID uuid) {
    BackManager bm = getBackManager();
    return bm != null && bm.hasBackHistory(uuid);
  }

  // ========== TPA API ==========

  public static boolean isAcceptingTpa(@NotNull UUID uuid) {
    TpaManager tm = getTpaManager();
    return tm == null || tm.isAcceptingRequests(uuid);
  }

  // ========== Internal helpers ==========

  @Nullable
  private static WarpManager getWarpManager() {
    if (!isAvailable()) return null;
    WarpsModule module = instance.getWarpsModule();
    return (module != null && module.isEnabled()) ? module.getWarpManager() : null;
  }

  @Nullable
  private static SpawnManager getSpawnManager() {
    if (!isAvailable()) return null;
    SpawnsModule module = instance.getSpawnsModule();
    return (module != null && module.isEnabled()) ? module.getSpawnManager() : null;
  }

  @Nullable
  private static BackManager getBackManager() {
    if (!isAvailable()) return null;
    TeleportModule module = instance.getTeleportModule();
    return (module != null && module.isEnabled()) ? module.getBackManager() : null;
  }

  @Nullable
  private static TpaManager getTpaManager() {
    if (!isAvailable()) return null;
    TeleportModule module = instance.getTeleportModule();
    return (module != null && module.isEnabled()) ? module.getTpaManager() : null;
  }
}
