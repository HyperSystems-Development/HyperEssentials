package com.hyperessentials.storage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage interface for home data.
 */
public interface HomeStorage {

    // TODO: Define home CRUD operations when homes module is implemented
    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
}
