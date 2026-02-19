package com.hyperessentials.storage;

import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for per-player data (TPA toggle, back history, etc.).
 */
public interface PlayerDataStorage {

    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
}
