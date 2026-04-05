package com.hyperessentials.storage;

import com.hyperessentials.module.kits.data.Kit;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for kit definitions.
 * Files: data/kits/<kitUuid>.json (one file per kit)
 */
public interface KitStorage {

  CompletableFuture<Void> init();
  CompletableFuture<Void> shutdown();

  /**
   * Scans data/kits/ directory and loads all kit definitions.
   * @return map keyed by kit name (lowercase)
   */
  CompletableFuture<Map<String, Kit>> loadAllKits();

  /**
   * Saves a single kit to data/kits/<uuid>.json.
   */
  CompletableFuture<Void> saveKit(@NotNull Kit kit);

  /**
   * Deletes a kit file by its UUID.
   */
  CompletableFuture<Void> deleteKit(@NotNull UUID kitUuid);
}
