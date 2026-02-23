package com.hyperessentials.storage;

import com.hyperessentials.data.Spawn;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for spawn data.
 */
public interface SpawnStorage {

    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
    CompletableFuture<Map<String, Spawn>> loadSpawns();
    CompletableFuture<Void> saveSpawns(@NotNull Map<String, Spawn> spawns);
}
