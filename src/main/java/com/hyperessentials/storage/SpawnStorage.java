package com.hyperessentials.storage;

import com.hyperessentials.data.Spawn;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for spawn data.
 * Files: data/spawns/<worldUuid>.json (one spawn per world)
 */
public interface SpawnStorage {

  CompletableFuture<Void> init();
  CompletableFuture<Void> shutdown();

  /**
   * Scans data/spawns/ directory and loads all spawns.
   * @return map keyed by world UUID string
   */
  CompletableFuture<Map<String, Spawn>> loadAllSpawns();

  /**
   * Saves a single spawn to data/spawns/<worldUuid>.json.
   */
  CompletableFuture<Void> saveSpawn(@NotNull Spawn spawn);

  /**
   * Deletes a spawn file by world UUID.
   */
  CompletableFuture<Void> deleteSpawn(@NotNull String worldUuid);
}
