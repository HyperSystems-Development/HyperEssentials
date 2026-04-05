package com.hyperessentials.storage;

import com.hyperessentials.data.PlayerHomes;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for home data.
 * Files: data/homes/<playerUuid>.json
 */
public interface HomeStorage {

  CompletableFuture<Void> init();
  CompletableFuture<Void> shutdown();

  /**
   * Loads a player's homes (including inline shares) from storage.
   */
  CompletableFuture<Optional<PlayerHomes>> loadPlayerHomes(@NotNull UUID uuid);

  /**
   * Saves a player's homes (including inline shares) to storage.
   */
  CompletableFuture<Void> savePlayerHomes(@NotNull PlayerHomes playerHomes);

  /**
   * Scans all home files at startup to build the reverse share index.
   * Returns: owner UUID -> (homeName -> set of viewer UUIDs)
   */
  CompletableFuture<Map<UUID, Map<String, Set<UUID>>>> scanAllShares();
}
