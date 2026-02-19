package com.hyperessentials.storage;

import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for warp data.
 */
public interface WarpStorage {

    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
}
