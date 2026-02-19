package com.hyperessentials.storage.json;

import com.hyperessentials.storage.*;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * JSON file-based storage provider.
 */
public class JsonStorageProvider implements StorageProvider {

    private final Path dataDir;

    public JsonStorageProvider(@NotNull Path dataDir) {
        this.dataDir = dataDir;
    }

    @Override
    public CompletableFuture<Void> init() {
        Logger.info("[Storage] JSON storage provider initialized");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        Logger.info("[Storage] JSON storage provider shut down");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @NotNull
    public HomeStorage getHomeStorage() {
        // TODO: Return actual implementation when homes module is built
        return new HomeStorage() {
            @Override public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
            @Override public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }
        };
    }

    @Override
    @NotNull
    public WarpStorage getWarpStorage() {
        return new WarpStorage() {
            @Override public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
            @Override public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }
        };
    }

    @Override
    @NotNull
    public SpawnStorage getSpawnStorage() {
        return new SpawnStorage() {
            @Override public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
            @Override public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }
        };
    }

    @Override
    @NotNull
    public PlayerDataStorage getPlayerDataStorage() {
        return new PlayerDataStorage() {
            @Override public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
            @Override public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }
        };
    }
}
