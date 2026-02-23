# Storage

## Overview

HyperEssentials uses a pluggable storage system. The `StorageProvider` interface provides access to domain-specific storage interfaces.

## Storage Interfaces

| Interface | Domain | Key Operations |
|-----------|--------|----------------|
| `HomeStorage` | Homes | CRUD for player homes |
| `WarpStorage` | Warps | Load/save all warps (bulk) |
| `SpawnStorage` | Spawns | Load/save all spawns (bulk) |
| `PlayerDataStorage` | Player data | Load/save/delete per-player TPA toggle, back history |

All operations return `CompletableFuture` for async execution.

## Providers

| Provider | Config Value | Status |
|----------|-------------|--------|
| `JsonStorageProvider` | `"json"` | Implemented |

## Data Directory

```
mods/com.hyperessentials_HyperEssentials/
  data/
    warps.json         Server warps
    spawns.json        Spawn points
    players/           Per-player teleport data (JSON per player)
    homes/             Player home data (JSON per player)
```

## Implementation Details

The `JsonStorageProvider` uses:
- **Atomic writes** - Data is written to a `.tmp` file first, then atomically moved to the target file using `Files.move` with `ATOMIC_MOVE` and `REPLACE_EXISTING`
- **GSON** - All serialization/deserialization uses GSON 2.11.0
- **Async** - All operations run on `CompletableFuture.supplyAsync()` / `runAsync()`
- **Bulk storage** - Warps and spawns are stored as a single JSON file each (not per-entry)
- **Per-player storage** - Player data is stored as individual JSON files keyed by UUID
