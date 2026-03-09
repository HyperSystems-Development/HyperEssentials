package com.hyperessentials.storage;

import com.hyperessentials.data.Warp;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for warp data.
 * Files: data/warps/<warpUuid>.json (one file per warp)
 */
public interface WarpStorage {

  CompletableFuture<Void> init();
  CompletableFuture<Void> shutdown();

  /**
   * Scans data/warps/ directory and loads all warps.
   * @return map keyed by warp name (lowercase)
   */
  CompletableFuture<Map<String, Warp>> loadAllWarps();

  /**
   * Saves a single warp to data/warps/<uuid>.json.
   */
  CompletableFuture<Void> saveWarp(@NotNull Warp warp);

  /**
   * Deletes a single warp file by its UUID.
   */
  CompletableFuture<Void> deleteWarp(@NotNull UUID warpUuid);
}
