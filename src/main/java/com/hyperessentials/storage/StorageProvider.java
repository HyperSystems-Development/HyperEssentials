package com.hyperessentials.storage;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Top-level storage interface for HyperEssentials.
 */
public interface StorageProvider {

  CompletableFuture<Void> init();

  CompletableFuture<Void> shutdown();

  @NotNull
  HomeStorage getHomeStorage();

  @NotNull
  WarpStorage getWarpStorage();

  @NotNull
  SpawnStorage getSpawnStorage();

  @NotNull
  PlayerDataStorage getPlayerDataStorage();
}
