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

Additionally, the kits and moderation modules have their own storage classes:
- `KitStorage` — Kit definitions (`data/kits.json`)
- `ModerationStorage` — Punishment records (`data/punishments.json`)

## Providers

| Provider | Config Value | Status |
|----------|-------------|--------|
| `JsonStorageProvider` | `"json"` | Implemented |

## Data Directory

```
mods/com.hyperessentials_HyperEssentials/
  data/
    .version                   Version marker file (integer, starts at 1)
    warps.json                 Server warps
    spawns.json                Spawn points
    kits.json                  Kit definitions
    punishments.json           Punishment records (keyed by player UUID)
    players/                   Per-player teleport data
      {uuid}.json              TPA toggle, back history, last TPA timestamp
    players/homes/             Per-player home data
      {uuid}.json              Home entries (name, world, xyz, yaw, pitch, timestamps)
  backups/
    backup_migration_v{from}-to-v{to}_{timestamp}.zip
```

## Implementation Details

The `JsonStorageProvider` uses:
- **Atomic writes** — Data is written to a `.tmp` file first, then atomically moved to the target file using `Files.move` with `ATOMIC_MOVE` and `REPLACE_EXISTING`
- **GSON** — All serialization/deserialization uses GSON 2.11.0
- **Async** — All operations run on `CompletableFuture.supplyAsync()` / `runAsync()`
- **Bulk storage** — Warps, spawns, kits, and punishments are stored as a single JSON file each
- **Per-player storage** — Player teleport data and homes are stored as individual JSON files keyed by UUID

## Migration System

The migration framework supports automatic data upgrades between versions:
- `MigrationRunner` creates a timestamped ZIP backup before executing any migration
- Migrations are chained by version number via `MigrationRegistry`
- Types: `CONFIG` (config files), `DATA` (data files), `SCHEMA` (reserved)
- On failure, the runner rolls back from the backup
- Pending migrations run automatically at startup before config loading
