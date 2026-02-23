package com.hyperessentials.storage;

import com.hyperessentials.data.PlayerTeleportData;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for per-player data (TPA toggle, back history, etc.).
 */
public interface PlayerDataStorage {

    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
    CompletableFuture<Optional<PlayerTeleportData>> loadPlayerData(@NotNull UUID uuid);
    CompletableFuture<Void> savePlayerData(@NotNull PlayerTeleportData data);
    CompletableFuture<Void> deletePlayerData(@NotNull UUID uuid);
}
