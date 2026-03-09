package com.hyperessentials.module.spawns;

import com.hyperessentials.data.Spawn;
import com.hyperessentials.storage.SpawnStorage;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.spawn.ISpawnProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages server spawns — one spawn per world, keyed by world UUID.
 * Each spawn is stored as an individual file (data/spawns/<worldUuid>.json).
 */
public class SpawnManager {

  private final SpawnStorage storage;
  private final Map<String, Spawn> spawns; // keyed by worldUuid

  public SpawnManager(@NotNull SpawnStorage storage) {
    this.storage = storage;
    this.spawns = new ConcurrentHashMap<>();
  }

  public CompletableFuture<Void> loadSpawns() {
    return storage.loadAllSpawns().thenAccept(loaded -> {
      spawns.clear();
      spawns.putAll(loaded);
      Logger.info("[Spawns] Loaded %d spawns", spawns.size());
    });
  }

  /**
   * Auto-detects world spawn points from the server on first startup.
   * Only runs if no spawns are configured (fresh install).
   */
  public void autoDetectWorldSpawns() {
    if (!spawns.isEmpty()) {
      Logger.debug("[Spawns] Spawns already configured, skipping auto-detection");
      return;
    }
    importWorldSpawns();
  }

  /**
   * Imports world spawn points from the Hytale server's WorldConfig spawn providers.
   */
  public int importWorldSpawns() {
    try {
      Universe universe = Universe.get();
      if (universe == null) {
        Logger.warn("[Spawns] Universe not available, cannot import spawns");
        return 0;
      }

      Map<String, World> worlds = universe.getWorlds();
      if (worlds.isEmpty()) {
        Logger.warn("[Spawns] No worlds found, cannot import spawns");
        return 0;
      }

      World defaultWorld = universe.getDefaultWorld();
      String defaultWorldUuid = defaultWorld != null
          ? defaultWorld.getWorldConfig().getUuid().toString() : null;
      boolean hasExistingGlobal = getGlobalSpawn() != null;
      int imported = 0;

      for (World world : worlds.values()) {
        ISpawnProvider provider = world.getWorldConfig().getSpawnProvider();
        if (provider == null) continue;

        @SuppressWarnings("deprecation")
        Transform[] spawnPoints = provider.getSpawnPoints();
        if (spawnPoints == null || spawnPoints.length == 0) continue;

        Transform transform = spawnPoints[0];
        Vector3d pos = transform.getPosition();
        Vector3f rot = transform.getRotation();

        String worldUuid = world.getWorldConfig().getUuid().toString();
        boolean isGlobal = !hasExistingGlobal
          && defaultWorldUuid != null
          && worldUuid.equals(defaultWorldUuid);

        Spawn spawn = new Spawn(
          worldUuid,
          world.getName(),
          pos.getX(), pos.getY(), pos.getZ(),
          rot.getY(), rot.getX(),
          isGlobal,
          System.currentTimeMillis(),
          "server"
        );

        boolean isUpdate = spawns.containsKey(worldUuid);
        spawns.put(worldUuid, spawn);
        storage.saveSpawn(spawn);
        if (isGlobal) hasExistingGlobal = true;
        imported++;

        Logger.info("[Spawns] %s spawn for world '%s' at %.1f, %.1f, %.1f%s",
          isUpdate ? "Updated" : "Imported",
          world.getName(), pos.getX(), pos.getY(), pos.getZ(),
          isGlobal ? " (global)" : "");
      }

      if (imported > 0) {
        Logger.info("[Spawns] Imported %d world spawn(s)", imported);
      }
      return imported;
    } catch (Exception e) {
      Logger.warn("[Spawns] Failed to import world spawns: %s", e.getMessage());
      return 0;
    }
  }

  /**
   * Sets/updates a spawn for a world.
   */
  public void setSpawn(@NotNull Spawn spawn) {
    if (spawn.isGlobal()) {
      // Clear old global
      for (Map.Entry<String, Spawn> entry : spawns.entrySet()) {
        if (entry.getValue().isGlobal() && !entry.getKey().equals(spawn.worldUuid())) {
          Spawn old = entry.getValue().withGlobal(false);
          spawns.put(entry.getKey(), old);
          storage.saveSpawn(old);
        }
      }
    }

    spawns.put(spawn.worldUuid(), spawn);
    storage.saveSpawn(spawn);
    Logger.info("[Spawns] Spawn set for world '%s'%s", spawn.worldName(), spawn.isGlobal() ? " (global)" : "");
  }

  /**
   * Deletes the spawn for a world.
   */
  public boolean deleteSpawn(@NotNull String worldUuid) {
    Spawn removed = spawns.remove(worldUuid);
    if (removed != null) {
      storage.deleteSpawn(worldUuid);
      Logger.info("[Spawns] Spawn deleted for world '%s'", removed.worldName());
      return true;
    }
    return false;
  }

  /**
   * Gets the spawn for a specific world by UUID.
   */
  @Nullable
  public Spawn getSpawnForWorld(@NotNull String worldUuid) {
    return spawns.get(worldUuid);
  }

  /**
   * Gets the global spawn (the one marked isGlobal=true).
   * Falls back to any spawn if none is explicitly global.
   */
  @Nullable
  public Spawn getGlobalSpawn() {
    for (Spawn spawn : spawns.values()) {
      if (spawn.isGlobal()) return spawn;
    }
    // Fallback: return any spawn if only one exists
    if (spawns.size() == 1) return spawns.values().iterator().next();
    return null;
  }

  /**
   * Sets a world's spawn as the global spawn.
   */
  public boolean setGlobalSpawn(@NotNull String worldUuid) {
    Spawn spawn = spawns.get(worldUuid);
    if (spawn == null) return false;

    // Clear old global
    for (Map.Entry<String, Spawn> entry : spawns.entrySet()) {
      if (entry.getValue().isGlobal()) {
        Spawn old = entry.getValue().withGlobal(false);
        spawns.put(entry.getKey(), old);
        storage.saveSpawn(old);
      }
    }

    // Set new global
    Spawn newGlobal = spawn.withGlobal(true);
    spawns.put(worldUuid, newGlobal);
    storage.saveSpawn(newGlobal);
    Logger.info("[Spawns] Global spawn set to world '%s'", newGlobal.worldName());
    return true;
  }

  @NotNull
  public Collection<Spawn> getAllSpawns() {
    return Collections.unmodifiableCollection(spawns.values());
  }

  public int getSpawnCount() {
    return spawns.size();
  }

  /**
   * Gets all world UUIDs that have spawns.
   */
  @NotNull
  public Set<String> getSpawnWorldUuids() {
    return Collections.unmodifiableSet(spawns.keySet());
  }
}
