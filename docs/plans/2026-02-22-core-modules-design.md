# Core Modules Design: Warps, Spawns, Teleport, RTP

**Date:** 2026-02-22
**Branch:** `feat/core-modules`
**Status:** Approved

## Overview

Migrate proven Warps, Spawns, and Teleport logic from HyperWarps into HyperEssentials' modular architecture, plus build a new RTP module. All GUI/CustomUI code is stripped — these modules are command-only at this stage. The GUI layer will be built from the ground up separately.

## Source & Target

- **Source:** HyperWarps (67 Java files, fully implemented)
- **Target:** HyperEssentials (framework scaffolded, modules are stubs)

## Modules

| Module | Source | Commands | Status |
|--------|--------|----------|--------|
| **Warps** | Ported from HyperWarps | `/warp`, `/warps`, `/setwarp`, `/delwarp`, `/warpinfo` | Warp CRUD, categories |
| **Spawns** | Ported from HyperWarps | `/spawn`, `/spawns`, `/setspawn`, `/delspawn`, `/spawninfo` | Spawn CRUD, per-world, respawn |
| **Teleport** | Ported from HyperWarps | `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel`, `/tptoggle`, `/back` | TPA, /back |
| **RTP** | New build | `/rtp` | Random teleport with safety checks |

## Architecture Decisions

### 1. Use Existing WarmupManager

All teleport warmup/cooldown delegates to HyperEssentials' centralized `WarmupManager`. Movement cancellation and damage cancellation are handled at the platform layer as shared concerns rather than being embedded in a module-specific TeleportManager.

### 2. No GUI

All GUI/CustomUI code from HyperWarps is excluded. No page registrations in `onEnable()`. The `/warps` and `/spawns` commands output text lists. Admin operations are CLI-only.

### 3. Bottom-Up Layered Build

Build in dependency order: data models -> storage -> managers -> commands -> platform wiring. Each layer commits independently and compiles.

## Directory Layout

### Config (already in place)

```
{dataDir}/
├── config.json                    (core settings)
└── config/                        (module configs)
    ├── warps.json
    ├── spawns.json
    ├── teleport.json
    ├── rtp.json
    ├── warmup.json
    └── ...
```

### Data Storage

```
{dataDir}/
└── data/                          (all persistent data)
    ├── warps.json                 (all warp definitions)
    ├── spawns.json                (all spawn definitions)
    └── players/                   (per-player data)
        └── {uuid}.json            (tpToggle, backHistory)
```

Aligned with HyperPerms conventions:
- Atomic writes (`.tmp` file then atomic move)
- Safe name validation (regex on file names)
- Subdirectory auto-creation on `init()`
- Pretty-printed GSON, HTML escaping disabled
- Async via CompletableFuture

## Data Models

All in `com.hyperessentials.data`:

### Warp (record)

```
name, displayName, category, world, x, y, z, yaw, pitch,
permission, description, createdAt, createdBy
```

- Lowercase name enforcement
- Builder-style `withXxx()` methods for immutable updates
- Permission field is optional (null = no restriction)

### Spawn (record)

```
name, world, x, y, z, yaw, pitch,
permission, groupPermission, isDefault, createdAt, createdBy
```

- Per-world spawn support
- Group-based selection via `groupPermission`
- One default spawn enforced by SpawnManager

### TeleportRequest (record)

```
requester (UUID), target (UUID), type (TPA|TPAHERE),
createdAt, expiresAt
```

- `getTeleportingPlayer()` / `getDestinationPlayer()` resolve who moves based on type
- `isExpired()` / `getRemainingTime()` for lifecycle

### PlayerTeleportData (mutable class)

```
uuid, username, tpToggle (bool),
backHistory (List<Location>), lastTpaRequest, lastTeleport
```

- Persisted: TPA toggle state, back location history
- Timestamps for cooldown tracking

### Location (existing)

Already in HyperEssentials — `record Location(String world, double x, double y, double z, float yaw, float pitch)`. No changes needed.

## Storage Interfaces

### WarpStorage

```java
CompletableFuture<Map<String, Warp>> loadWarps();
CompletableFuture<Void> saveWarps(Map<String, Warp> warps);
```

### SpawnStorage

```java
CompletableFuture<Map<String, Spawn>> loadSpawns();
CompletableFuture<Void> saveSpawns(Map<String, Spawn> spawns);
```

### PlayerDataStorage

```java
CompletableFuture<Optional<PlayerTeleportData>> loadPlayerData(UUID uuid);
CompletableFuture<Void> savePlayerData(PlayerTeleportData data);
CompletableFuture<Void> deletePlayerData(UUID uuid);
```

### JsonStorageProvider Updates

- Root directory: `{dataDir}/data/`
- Auto-creates `data/` and `data/players/` on init
- Atomic writes: serialize -> write `.tmp` -> atomic move
- GSON with pretty printing, custom type adapters as needed

## Module Managers

### WarpManager (warps module)

- In-memory: `ConcurrentHashMap<String, Warp>`
- CRUD: `setWarp()`, `getWarp()`, `deleteWarp()`, `getAllWarps()`
- Queries: `getAccessibleWarps(uuid)`, `getWarpsByCategory()`, `getCategories()`
- Access control: checks `warp.permission` via PermissionManager
- Async load/save via WarpStorage

### SpawnManager (spawns module)

- In-memory: `ConcurrentHashMap<String, Spawn>`
- CRUD: `setSpawn()`, `getSpawn()`, `deleteSpawn()`, `getAllSpawns()`
- Special: `getDefaultSpawn()`, `getSpawnForPlayer(uuid)`, `getSpawnForWorld(world)`
- Group-based selection: checks `groupPermission` via PermissionManager
- Per-world spawn support (config-driven)
- Default spawn management (only one at a time)

### TpaManager (teleport module)

- Incoming requests: `Map<UUID, List<TeleportRequest>>`
- Outgoing requests: `Map<UUID, TeleportRequest>`
- Player cache: `ConcurrentHashMap<UUID, PlayerTeleportData>`
- Request lifecycle: create, accept, deny, cancel, expire
- Auto-cleanup of expired requests
- TPA-specific cooldown tracking
- Toggle: `isAcceptingRequests()`, `toggleTpToggle()`
- Config-driven: timeout, cooldown, maxPending from TeleportConfig

### BackManager (teleport module)

- Delegates to TpaManager for player data storage
- History: `addBackLocation()`, `popBackLocation()` (removes on use)
- Triggers: `onTeleport()`, `onDeath()` if configured
- Config-driven: `saveOnTeleport`, `saveOnDeath`, `historySize` from TeleportConfig

### RtpManager (rtp module)

- Random coordinate generation within ring (minRadius to maxRadius)
- Safety checking: solid ground, not void, not dangerous blocks
- Retry logic up to `maxAttempts`
- Integrates with BackManager to save pre-teleport location
- Delegates warmup/cooldown to WarmupManager
- Config-driven: center, radii, attempts, blacklisted worlds

## Commands

### Warps Module (5 commands)

| Command | Permission | Description |
|---------|-----------|-------------|
| `/warp <name>` | `hyperessentials.warp` | Teleport to a warp |
| `/warps` | `hyperessentials.warp.list` | List accessible warps |
| `/setwarp <name> [category]` | `hyperessentials.warp.set` | Create warp at current location |
| `/delwarp <name>` | `hyperessentials.warp.delete` | Delete a warp |
| `/warpinfo <name>` | `hyperessentials.warp.info` | Show warp details |

### Spawns Module (5 commands)

| Command | Permission | Description |
|---------|-----------|-------------|
| `/spawn [name]` | `hyperessentials.spawn` | Teleport to spawn |
| `/spawns` | `hyperessentials.spawn.list` | List all spawns |
| `/setspawn [name]` | `hyperessentials.spawn.set` | Create spawn at current location |
| `/delspawn <name>` | `hyperessentials.spawn.delete` | Delete a spawn |
| `/spawninfo <name>` | `hyperessentials.spawn.info` | Show spawn details |

### Teleport Module (7 commands)

| Command | Permission | Description |
|---------|-----------|-------------|
| `/tpa <player>` | `hyperessentials.tpa` | Request teleport to player |
| `/tpahere <player>` | `hyperessentials.tpahere` | Request player teleport to you |
| `/tpaccept` | `hyperessentials.tpaccept` | Accept incoming TPA |
| `/tpdeny` | `hyperessentials.tpdeny` | Deny incoming TPA |
| `/tpcancel` | `hyperessentials.tpcancel` | Cancel outgoing TPA |
| `/tptoggle` | `hyperessentials.tptoggle` | Toggle TPA acceptance |
| `/back` | `hyperessentials.back` | Return to previous location |

### RTP Module (1 command)

| Command | Permission | Description |
|---------|-----------|-------------|
| `/rtp` | `hyperessentials.rtp` | Teleport to random location |

## Platform Wiring

Updates to `HyperEssentialsPlugin`:

- **Command registration:** Register all 18 commands (5 + 5 + 7 + 1)
- **Movement checking:** Background `ScheduledExecutorService` (100ms poll) for warmup cancellation
- **PlayerConnect:** Load player teleport data async
- **PlayerDisconnect:** Cancel warmups, save/unload player data, cancel TPA requests
- **Death events:** Save back location (if configured), handle respawn teleport to spawn (if configured)

## RTP Config (flesh out existing stub)

```json
{
  "enabled": false,
  "centerX": 0,
  "centerZ": 0,
  "minRadius": 100,
  "maxRadius": 5000,
  "maxAttempts": 10,
  "blacklistedWorlds": []
}
```

## What Gets Stripped from HyperWarps

Everything GUI-related:
- `GuiManager`, `GuiType`, `UIHelper`, `ActivePageTracker`
- All page classes (WarpListPage, WarpDetailPage, AdminMainPage, etc.)
- All GUI data classes (WarpListData, WarpDetailData, etc.)
- All `.ui` resource files
- `AdminCommand` (HyperWarps' GUI-based admin)
- `GuiConfig`
- No GUI page registrations in any module's `onEnable()`

## Commit Strategy

1. **feat: add shared data models for warps, spawns, and teleport**
2. **feat: implement warp and spawn storage interfaces with JSON provider**
3. **feat: implement player data storage interface with JSON provider**
4. **feat: add WarpManager and implement warps module with commands**
5. **feat: add SpawnManager and implement spawns module with commands**
6. **feat: add TpaManager, BackManager and implement teleport module with commands**
7. **feat: implement RTP module with random location finding and safety checks**
8. **feat: wire platform events, movement checking, and command registration**
9. **feat: update API, permissions, and documentation**
