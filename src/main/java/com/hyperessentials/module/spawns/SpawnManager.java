package com.hyperessentials.module.spawns;

import com.hyperessentials.config.modules.SpawnsConfig;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.integration.PermissionManager;
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
import java.util.stream.Collectors;

/**
 * Manages server spawns - loading, saving, and CRUD operations.
 */
public class SpawnManager {

  private final SpawnStorage storage;
  private final SpawnsConfig config;
  private final Map<String, Spawn> spawns;

  public SpawnManager(@NotNull SpawnStorage storage, @NotNull SpawnsConfig config) {
    this.storage = storage;
    this.config = config;
    this.spawns = new ConcurrentHashMap<>();
  }

  public CompletableFuture<Void> loadSpawns() {
    return storage.loadSpawns().thenAccept(loaded -> {
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
   * Creates or updates spawn records for each world that has a configured spawn point.
   * Existing user-created spawns that don't match a world name are preserved.
   * The default world's spawn is marked as default if no default exists yet.
   *
   * @return the number of spawns imported
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
      String defaultWorldName = defaultWorld != null ? defaultWorld.getName() : null;
      boolean hasExistingDefault = getDefaultSpawn() != null;
      int imported = 0;

      for (World world : worlds.values()) {
        ISpawnProvider provider = world.getWorldConfig().getSpawnProvider();
        if (provider == null) {
          Logger.debug("[Spawns] World '%s' has no spawn provider, skipping", world.getName());
          continue;
        }

        @SuppressWarnings("deprecation")
        Transform[] spawnPoints = provider.getSpawnPoints();
        if (spawnPoints == null || spawnPoints.length == 0) {
          Logger.debug("[Spawns] World '%s' spawn provider returned no spawn points, skipping", world.getName());
          continue;
        }

        Transform transform = spawnPoints[0];
        Vector3d pos = transform.getPosition();
        Vector3f rot = transform.getRotation();

        String spawnName = world.getName().toLowerCase();
        boolean isDefault = !hasExistingDefault
          && defaultWorldName != null
          && world.getName().equalsIgnoreCase(defaultWorldName);

        Spawn spawn = new Spawn(
          spawnName,
          world.getName(),
          pos.getX(), pos.getY(), pos.getZ(),
          rot.getY(), rot.getX(),  // yaw = rotation.y, pitch = rotation.x
          null,   // no permission
          null,   // no group permission
          isDefault,
          System.currentTimeMillis(),
          "server" // created by server import
        );

        boolean isUpdate = spawns.containsKey(spawnName);
        spawns.put(spawnName, spawn);
        if (isDefault) hasExistingDefault = true;
        imported++;

        Logger.info("[Spawns] %s spawn for world '%s' at %.1f, %.1f, %.1f%s",
          isUpdate ? "Updated" : "Imported",
          world.getName(), pos.getX(), pos.getY(), pos.getZ(),
          isDefault ? " (default)" : "");
      }

      if (imported > 0) {
        saveSpawns();
        Logger.info("[Spawns] Imported %d world spawn(s)", imported);
      }
      return imported;
    } catch (Exception e) {
      Logger.warn("[Spawns] Failed to import world spawns: %s", e.getMessage());
      return 0;
    }
  }

  public CompletableFuture<Void> saveSpawns() {
    return storage.saveSpawns(new ConcurrentHashMap<>(spawns));
  }

  public boolean setSpawn(@NotNull Spawn spawn) {
    if (spawn.isDefault()) {
      for (Map.Entry<String, Spawn> entry : spawns.entrySet()) {
        if (entry.getValue().isDefault() && !entry.getKey().equals(spawn.name())) {
          spawns.put(entry.getKey(), entry.getValue().withDefault(false));
        }
      }
    }

    boolean isNew = !spawns.containsKey(spawn.name());
    spawns.put(spawn.name(), spawn);
    saveSpawns();
    Logger.info("[Spawns] Spawn '%s' %s%s", spawn.name(), isNew ? "created" : "updated",
           spawn.isDefault() ? " (default)" : "");
    return isNew;
  }

  @Nullable
  public Spawn getSpawn(@NotNull String name) {
    return spawns.get(name.toLowerCase());
  }

  public boolean deleteSpawn(@NotNull String name) {
    Spawn removed = spawns.remove(name.toLowerCase());
    if (removed != null) {
      saveSpawns();
      Logger.info("[Spawns] Spawn '%s' deleted", name);
      return true;
    }
    return false;
  }

  @Nullable
  public Spawn getDefaultSpawn() {
    for (Spawn spawn : spawns.values()) {
      if (spawn.isDefault()) {
        return spawn;
      }
    }
    String defaultName = config.getDefaultSpawnName();
    return spawns.get(defaultName.toLowerCase());
  }

  @Nullable
  public Spawn getSpawnForPlayer(@NotNull UUID playerUuid) {
    for (Spawn spawn : spawns.values()) {
      if (spawn.isGroupRestricted()) {
        if (PermissionManager.get().hasPermission(playerUuid, spawn.groupPermission())) {
          return spawn;
        }
      }
    }
    return getDefaultSpawn();
  }

  @Nullable
  public Spawn getSpawnForWorld(@NotNull String worldName) {
    for (Spawn spawn : spawns.values()) {
      if (spawn.world().equalsIgnoreCase(worldName)) {
        return spawn;
      }
    }
    return null;
  }

  @NotNull
  public Collection<Spawn> getAllSpawns() {
    return Collections.unmodifiableCollection(spawns.values());
  }

  @NotNull
  public List<Spawn> getAccessibleSpawns(@NotNull UUID playerUuid) {
    return spawns.values().stream()
      .filter(spawn -> canAccess(playerUuid, spawn))
      .collect(Collectors.toList());
  }

  public boolean canAccess(@NotNull UUID playerUuid, @NotNull Spawn spawn) {
    if (!spawn.requiresPermission()) {
      return true;
    }
    return PermissionManager.get().hasPermission(playerUuid, spawn.permission());
  }

  public boolean spawnExists(@NotNull String name) {
    return spawns.containsKey(name.toLowerCase());
  }

  public boolean setDefaultSpawn(@NotNull String name) {
    Spawn spawn = spawns.get(name.toLowerCase());
    if (spawn == null) {
      return false;
    }

    for (Map.Entry<String, Spawn> entry : spawns.entrySet()) {
      if (entry.getValue().isDefault()) {
        spawns.put(entry.getKey(), entry.getValue().withDefault(false));
      }
    }

    spawns.put(spawn.name(), spawn.withDefault(true));
    saveSpawns();
    return true;
  }

  @NotNull
  public List<String> getSpawnNames() {
    return new ArrayList<>(spawns.keySet());
  }

  public int getSpawnCount() {
    return spawns.size();
  }
}
