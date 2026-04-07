package com.hyperessentials.module.spawns;

import com.hyperessentials.api.events.EventBus;
import com.hyperessentials.api.events.spawns.*;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.storage.SpawnStorage;
import com.hyperessentials.util.ErrorHandler;
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
   * Ensures a global spawn always exists.
   * <ul>
   *   <li>If spawns exist but none is marked global, marks the first one as global.</li>
   *   <li>If no spawns exist at all, creates one from the server's default world spawn.</li>
   * </ul>
   * Called on module enable after loading spawns.
   */
  public void ensureGlobalSpawn() {
    // Check if any spawn is already global
    for (Spawn spawn : spawns.values()) {
      if (spawn.isGlobal()) return;
    }

    // If spawns exist but none is global, mark the first one
    if (!spawns.isEmpty()) {
      Map.Entry<String, Spawn> first = spawns.entrySet().iterator().next();
      Spawn promoted = first.getValue().withGlobal(true);
      spawns.put(first.getKey(), promoted);
      storage.saveSpawn(promoted);
      Logger.info("[Spawns] No global spawn found, promoted '%s' to global", promoted.worldName());
      return;
    }

    // No spawns at all — create from server default world
    try {
      Universe universe = Universe.get();
      if (universe == null) return;

      World defaultWorld = universe.getDefaultWorld();
      if (defaultWorld == null) {
        // Fallback: use the first available world
        Map<String, World> worlds = universe.getWorlds();
        if (worlds.isEmpty()) return;
        defaultWorld = worlds.values().iterator().next();
      }

      ISpawnProvider provider = defaultWorld.getWorldConfig().getSpawnProvider();
      double x = 0, y = 64, z = 0;
      float yaw = 0, pitch = 0;

      if (provider != null) {
        @SuppressWarnings("deprecation")
        Transform[] spawnPoints = provider.getSpawnPoints();
        if (spawnPoints != null && spawnPoints.length > 0) {
          Transform transform = spawnPoints[0];
          Vector3d pos = transform.getPosition();
          Vector3f rot = transform.getRotation();
          x = pos.getX();
          y = pos.getY();
          z = pos.getZ();
          yaw = rot.getY();
          pitch = rot.getX();
        }
      }

      String worldUuid = defaultWorld.getWorldConfig().getUuid().toString();
      Spawn spawn = new Spawn(worldUuid, defaultWorld.getName(),
          x, y, z, yaw, pitch, true, System.currentTimeMillis(), "server");
      spawns.put(worldUuid, spawn);
      storage.saveSpawn(spawn);
      Logger.info("[Spawns] Created global spawn for default world '%s' at %.1f, %.1f, %.1f",
          defaultWorld.getName(), x, y, z);
    } catch (Exception e) {
      ErrorHandler.report("[Spawns] Failed to create default global spawn", e);
    }
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
      ErrorHandler.report("[Spawns] Failed to import world spawns", e);
      return 0;
    }
  }

  /**
   * Sets/updates a spawn for a world.
   */
  public void setSpawn(@NotNull Spawn spawn) {
    if (spawn.createdBy() != null) {
      try {
        UUID actorUuid = UUID.fromString(spawn.createdBy());
        if (EventBus.publishCancellable(new SpawnSetPreEvent(spawn.worldUuid(), actorUuid))) {
          return;
        }
      } catch (IllegalArgumentException ignored) {}
    }

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

    if (spawn.createdBy() != null) {
      try {
        UUID actorUuid = UUID.fromString(spawn.createdBy());
        EventBus.publish(new SpawnSetEvent(spawn.worldUuid(), actorUuid));
      } catch (IllegalArgumentException ignored) {}
    }
  }

  /**
   * Deletes the spawn for a world.
   */
  public boolean deleteSpawn(@NotNull String worldUuid) {
    Spawn spawn = spawns.get(worldUuid);
    if (spawn == null) return false;

    if (spawn.createdBy() != null) {
      try {
        UUID actorUuid = UUID.fromString(spawn.createdBy());
        if (EventBus.publishCancellable(new SpawnDeletePreEvent(worldUuid, actorUuid))) {
          return false;
        }
      } catch (IllegalArgumentException ignored) {}
    }

    Spawn removed = spawns.remove(worldUuid);
    if (removed != null) {
      storage.deleteSpawn(worldUuid);
      Logger.info("[Spawns] Spawn deleted for world '%s'", removed.worldName());

      if (removed.createdBy() != null) {
        try {
          UUID actorUuid = UUID.fromString(removed.createdBy());
          EventBus.publish(new SpawnDeleteEvent(worldUuid, actorUuid));
        } catch (IllegalArgumentException ignored) {}
      }
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

  /**
   * Returns a map of all loaded server worlds (keyed by world UUID string).
   * Used by the admin GUI to show worlds that don't have custom spawns yet.
   */
  @NotNull
  public Map<String, World> getLoadedWorlds() {
    Universe universe = Universe.get();
    if (universe == null) return Collections.emptyMap();

    Map<String, World> result = new LinkedHashMap<>();
    for (World world : universe.getWorlds().values()) {
      String uuid = world.getWorldConfig().getUuid().toString();
      result.put(uuid, world);
    }
    return result;
  }

  /**
   * Creates a Spawn record from a world's default spawn point.
   * Does NOT persist to storage — used for display purposes in the admin GUI.
   */
  @Nullable
  public Spawn createDefaultSpawnForWorld(@NotNull World world) {
    ISpawnProvider provider = world.getWorldConfig().getSpawnProvider();
    double x = 0, y = 64, z = 0;
    float yaw = 0, pitch = 0;

    if (provider != null) {
      @SuppressWarnings("deprecation")
      Transform[] spawnPoints = provider.getSpawnPoints();
      if (spawnPoints != null && spawnPoints.length > 0) {
        Transform transform = spawnPoints[0];
        Vector3d pos = transform.getPosition();
        Vector3f rot = transform.getRotation();
        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();
        yaw = rot.getY();
        pitch = rot.getX();
      }
    }

    String worldUuid = world.getWorldConfig().getUuid().toString();
    return new Spawn(worldUuid, world.getName(),
        x, y, z, yaw, pitch, false, 0, null);
  }
}
