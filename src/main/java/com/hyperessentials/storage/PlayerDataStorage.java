package com.hyperessentials.storage;

import com.hyperessentials.data.PlayerData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for unified per-player data (teleport + stats + punishments).
 * Files: data/players/<uuid>.json
 */
public interface PlayerDataStorage {

  CompletableFuture<Void> init();
  CompletableFuture<Void> shutdown();
  CompletableFuture<Optional<PlayerData>> loadPlayerData(@NotNull UUID uuid);
  CompletableFuture<Void> savePlayerData(@NotNull PlayerData data);
  CompletableFuture<Void> deletePlayerData(@NotNull UUID uuid);

  /**
   * Scans data/players/ directory and loads all player data.
   * Expensive operation — use sparingly (admin queries only).
   */
  CompletableFuture<List<PlayerData>> loadAllPlayerData();
}
