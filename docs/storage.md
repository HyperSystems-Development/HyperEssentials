# Storage

> **Status:** Interfaces defined, JSON provider is a stub.

## Overview

HyperEssentials uses a pluggable storage system. The `StorageProvider` interface provides access to domain-specific storage interfaces.

## Storage Interfaces

| Interface | Domain | Key Operations |
|-----------|--------|----------------|
| `HomeStorage` | Homes | CRUD for player homes |
| `WarpStorage` | Warps | CRUD for server warps |
| `SpawnStorage` | Spawns | CRUD for spawn points |
| `PlayerDataStorage` | Player data | TPA toggle, back history, preferences |

All operations return `CompletableFuture` for async execution.

## Providers

| Provider | Config Value | Status |
|----------|-------------|--------|
| `JsonStorageProvider` | `"json"` | Stub |

## Data Directory

```
mods/com.hyperessentials_HyperEssentials/
  data/
    homes/           Player home data (JSON per player)
    warps/           Server warps
    spawns/          Spawn points
    players/         Per-player preferences
```
