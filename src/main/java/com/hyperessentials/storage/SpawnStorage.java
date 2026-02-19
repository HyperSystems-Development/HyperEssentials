package com.hyperessentials.storage;

import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for spawn data.
 */
public interface SpawnStorage {

    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
}
