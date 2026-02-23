package com.hyperessentials.storage;

import com.hyperessentials.data.Warp;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for warp data.
 */
public interface WarpStorage {

    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
    CompletableFuture<Map<String, Warp>> loadWarps();
    CompletableFuture<Void> saveWarps(@NotNull Map<String, Warp> warps);
}
