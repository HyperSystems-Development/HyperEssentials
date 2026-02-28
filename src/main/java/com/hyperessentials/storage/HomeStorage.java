package com.hyperessentials.storage;

import com.hyperessentials.data.PlayerHomes;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for home data.
 */
public interface HomeStorage {

  CompletableFuture<Void> init();
  CompletableFuture<Void> shutdown();

  /**
   * Loads a player's homes from storage.
   */
  CompletableFuture<Optional<PlayerHomes>> loadPlayerHomes(@NotNull UUID uuid);

  /**
   * Saves a player's homes to storage.
   */
  CompletableFuture<Void> savePlayerHomes(@NotNull PlayerHomes playerHomes);
}
