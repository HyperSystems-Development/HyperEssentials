package com.hyperessentials.module.spawns;

import com.hyperessentials.config.modules.SpawnsConfig;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.integration.PermissionManager;
import com.hyperessentials.storage.SpawnStorage;
import com.hyperessentials.util.Logger;
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
