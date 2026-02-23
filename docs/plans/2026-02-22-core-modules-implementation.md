# Core Modules Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the Warps, Spawns, Teleport, and RTP modules by migrating proven logic from HyperWarps, stripping all GUI/CustomUI code, and adapting to HyperEssentials' modular architecture.

**Architecture:** Bottom-up layered build — data models first, then storage, then managers, then commands, then platform wiring. Each module fills its existing stub. All warmup/cooldown delegates to the existing `WarmupManager`. Storage uses a `data/` subdirectory with atomic writes aligned to HyperPerms conventions.

**Tech Stack:** Java 25, Hytale Server API, GSON 2.11.0, HyperEssentials module framework

**Source Reference:** `C:/Users/Nick/Documents/Apps/HyperSystems/HyperWarps/` — port from here, adapt package names `com.hyperwarps` -> `com.hyperessentials`

**Commit Author:** `ZenithDevHQ <ZenithDevHQ@users.noreply.github.com>` — DO NOT mention Claude in commits.

---

## Task 1: Add Shared Data Models

**Files:**
- Create: `src/main/java/com/hyperessentials/data/Warp.java`
- Create: `src/main/java/com/hyperessentials/data/Spawn.java`
- Create: `src/main/java/com/hyperessentials/data/TeleportRequest.java`
- Create: `src/main/java/com/hyperessentials/data/PlayerTeleportData.java`
- Modify: `src/main/java/com/hyperessentials/data/Location.java` (add utility methods)

### Step 1: Create Warp record

Port from `HyperWarps/src/main/java/com/hyperwarps/data/Warp.java`. Change package to `com.hyperessentials.data`. Keep all fields, compact constructor, factory method, `withXxx()` builders, and `requiresPermission()`. No changes to logic.

```java
package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record Warp(
    @NotNull String name,
    @NotNull String displayName,
    @NotNull String category,
    @NotNull String world,
    double x, double y, double z,
    float yaw, float pitch,
    @Nullable String permission,
    @Nullable String description,
    long createdAt,
    @Nullable String createdBy
) {
    public Warp {
        name = name.toLowerCase();
        if (displayName == null || displayName.isEmpty()) displayName = name;
        if (category == null || category.isEmpty()) category = "general";
    }

    public static Warp create(@NotNull String name, @NotNull String world,
                              double x, double y, double z, float yaw, float pitch,
                              @Nullable String createdBy) {
        return new Warp(name.toLowerCase(), name, "general", world, x, y, z, yaw, pitch,
                       null, null, System.currentTimeMillis(), createdBy);
    }

    public Warp withDisplayName(@NotNull String v) { return new Warp(name, v, category, world, x, y, z, yaw, pitch, permission, description, createdAt, createdBy); }
    public Warp withCategory(@NotNull String v) { return new Warp(name, displayName, v, world, x, y, z, yaw, pitch, permission, description, createdAt, createdBy); }
    public Warp withPermission(@Nullable String v) { return new Warp(name, displayName, category, world, x, y, z, yaw, pitch, v, description, createdAt, createdBy); }
    public Warp withDescription(@Nullable String v) { return new Warp(name, displayName, category, world, x, y, z, yaw, pitch, permission, v, createdAt, createdBy); }
    public Warp withLocation(@NotNull String w, double nx, double ny, double nz, float ny2, float np) {
        return new Warp(name, displayName, category, w, nx, ny, nz, ny2, np, permission, description, createdAt, createdBy);
    }
    public boolean requiresPermission() { return permission != null && !permission.isEmpty(); }
}
```

### Step 2: Create Spawn record

Port from `HyperWarps/src/main/java/com/hyperwarps/data/Spawn.java`. Same treatment.

### Step 3: Create TeleportRequest record

Port from `HyperWarps/src/main/java/com/hyperwarps/data/TeleportRequest.java`. Same treatment. Includes `Type` enum (TPA/TPAHERE), factory method, `isExpired()`, `getRemainingTime()`, `getTeleportingPlayer()`, `getDestinationPlayer()`.

### Step 4: Create PlayerTeleportData class

Port from `HyperWarps/src/main/java/com/hyperwarps/data/PlayerTeleportData.java`. Same treatment. Mutable class with UUID, username, tpToggle, backHistory list, timestamps.

### Step 5: Update Location record

Add `fromWarp()`, `fromSpawn()`, and `distanceSquared()` utility methods to the existing `Location.java`:

```java
public static Location fromWarp(@NotNull Warp warp) {
    return new Location(warp.world(), warp.x(), warp.y(), warp.z(), warp.yaw(), warp.pitch());
}
public static Location fromSpawn(@NotNull Spawn spawn) {
    return new Location(spawn.world(), spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch());
}
public double distanceSquared(@NotNull Location other) {
    if (!world.equals(other.world)) return Double.MAX_VALUE;
    double dx = x - other.x, dy = y - other.y, dz = z - other.z;
    return dx * dx + dy * dy + dz * dz;
}
```

### Step 6: Commit

```bash
git add src/main/java/com/hyperessentials/data/
git commit --author="ZenithDevHQ <ZenithDevHQ@users.noreply.github.com>" -m "feat: add shared data models for warps, spawns, and teleport"
```

---

## Task 2: Implement Warp and Spawn Storage

**Files:**
- Modify: `src/main/java/com/hyperessentials/storage/WarpStorage.java`
- Modify: `src/main/java/com/hyperessentials/storage/SpawnStorage.java`
- Modify: `src/main/java/com/hyperessentials/storage/json/JsonStorageProvider.java`

### Step 1: Flesh out WarpStorage interface

Add CRUD methods to the existing stub:

```java
package com.hyperessentials.storage;

import com.hyperessentials.data.Warp;
import org.jetbrains.annotations.NotNull;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface WarpStorage {
    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
    CompletableFuture<Map<String, Warp>> loadWarps();
    CompletableFuture<Void> saveWarps(@NotNull Map<String, Warp> warps);
}
```

### Step 2: Flesh out SpawnStorage interface

Same pattern with `loadSpawns()` / `saveSpawns()`.

### Step 3: Create JsonWarpStorage inner implementation

Inside `JsonStorageProvider`, create a proper inner class `JsonWarpStorage implements WarpStorage`. Port serialization/deserialization logic from `HyperWarps/src/main/java/com/hyperwarps/storage/json/JsonStorageProvider.java` (lines 76-166). Key changes:
- File path: `dataDir.resolve("data/warps.json")` (not `dataDir.resolve("warps.json")`)
- Use atomic writes: write to `.tmp` file, then `Files.move()` with `ATOMIC_MOVE` + `REPLACE_EXISTING`
- Import `com.hyperessentials.data.Warp` instead of `com.hyperwarps.data.Warp`

### Step 4: Create JsonSpawnStorage inner implementation

Same pattern. File path: `dataDir.resolve("data/spawns.json")`. Port spawn serialization from HyperWarps (lines 170-259).

### Step 5: Wire implementations in JsonStorageProvider

Update `getWarpStorage()` and `getSpawnStorage()` to return actual implementations. Update `init()` to create `data/` directory. Keep a shared `Gson` instance.

```java
public class JsonStorageProvider implements StorageProvider {
    private final Path dataDir;
    private final Path dataRoot;  // dataDir.resolve("data")
    private final Gson gson;
    private final JsonWarpStorage warpStorage;
    private final JsonSpawnStorage spawnStorage;

    public JsonStorageProvider(@NotNull Path dataDir) {
        this.dataDir = dataDir;
        this.dataRoot = dataDir.resolve("data");
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        this.warpStorage = new JsonWarpStorage();
        this.spawnStorage = new JsonSpawnStorage();
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(dataRoot);
                Logger.info("[Storage] JSON storage initialized at %s", dataRoot);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create storage directories", e);
            }
        });
    }
    // ...
}
```

### Step 6: Commit

```bash
git add src/main/java/com/hyperessentials/storage/
git commit --author="ZenithDevHQ <ZenithDevHQ@users.noreply.github.com>" -m "feat: implement warp and spawn storage interfaces with JSON provider"
```

---

## Task 3: Implement Player Data Storage

**Files:**
- Modify: `src/main/java/com/hyperessentials/storage/PlayerDataStorage.java`
- Modify: `src/main/java/com/hyperessentials/storage/json/JsonStorageProvider.java`

### Step 1: Flesh out PlayerDataStorage interface

```java
package com.hyperessentials.storage;

import com.hyperessentials.data.PlayerTeleportData;
import org.jetbrains.annotations.NotNull;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerDataStorage {
    CompletableFuture<Void> init();
    CompletableFuture<Void> shutdown();
    CompletableFuture<Optional<PlayerTeleportData>> loadPlayerData(@NotNull UUID uuid);
    CompletableFuture<Void> savePlayerData(@NotNull PlayerTeleportData data);
    CompletableFuture<Void> deletePlayerData(@NotNull UUID uuid);
}
```

### Step 2: Create JsonPlayerDataStorage inner implementation

Port from `HyperWarps/src/main/java/com/hyperwarps/storage/json/JsonStorageProvider.java` (lines 261-364). Key changes:
- Directory: `dataRoot.resolve("players")` (i.e., `{dataDir}/data/players/`)
- Atomic writes for player files
- Import `com.hyperessentials.data.*`
- Location serialization/deserialization helper methods shared with a utility

### Step 3: Wire in JsonStorageProvider

Update `init()` to also create `data/players/` directory. Update `getPlayerDataStorage()` to return the real implementation.

### Step 4: Commit

```bash
git add src/main/java/com/hyperessentials/storage/
git commit --author="ZenithDevHQ <ZenithDevHQ@users.noreply.github.com>" -m "feat: implement player data storage interface with JSON provider"
```

---

## Task 4: Implement Warps Module

**Files:**
- Create: `src/main/java/com/hyperessentials/module/warps/WarpManager.java`
- Create: `src/main/java/com/hyperessentials/module/warps/command/WarpCommand.java`
- Create: `src/main/java/com/hyperessentials/module/warps/command/WarpsCommand.java`
- Create: `src/main/java/com/hyperessentials/module/warps/command/SetWarpCommand.java`
- Create: `src/main/java/com/hyperessentials/module/warps/command/DelWarpCommand.java`
- Create: `src/main/java/com/hyperessentials/module/warps/command/WarpInfoCommand.java`
- Modify: `src/main/java/com/hyperessentials/module/warps/WarpsModule.java`

### Step 1: Create WarpManager

Port from `HyperWarps/src/main/java/com/hyperwarps/manager/WarpManager.java`. Key adaptations:
- Package: `com.hyperessentials.module.warps`
- Constructor takes `WarpStorage` (not `StorageProvider`)
- Uses `com.hyperessentials.integration.PermissionManager`
- Uses `com.hyperessentials.data.Warp`
- Uses `com.hyperessentials.util.Logger`

All methods port 1:1: `loadWarps()`, `saveWarps()`, `setWarp()`, `getWarp()`, `deleteWarp()`, `getAllWarps()`, `getAccessibleWarps()`, `getWarpsByCategory()`, `getCategories()`, `canAccess()`, `warpExists()`, `getWarpNames()`, `getAccessibleWarpNames()`, `getWarpCount()`.

### Step 2: Create SetWarpCommand

Port from `HyperWarps/src/main/java/com/hyperwarps/command/warp/SetWarpCommand.java`. Key adaptations:
- Package: `com.hyperessentials.module.warps.command`
- Constructor takes `WarpManager` and `WarpsConfig` (not `HyperWarps`)
- Permission: `Permissions.WARP_SET` (not `Permissions.SETWARP`)
- Default category from `WarpsConfig.getDefaultCategory()`
- Uses `com.hyperessentials.command.util.CommandUtil` for messages
- Strip all `UIHelper` references — use `CommandUtil.info()` for secondary messages

### Step 3: Create WarpCommand

Port from `HyperWarps/src/main/java/com/hyperwarps/command/warp/WarpCommand.java`. Key adaptations:
- Constructor takes `WarpManager` and `WarmupManager`
- Permission: `Permissions.WARP`
- Replace `TeleportManager.teleportToLocation()` flow with `WarmupManager.startWarmup()`:
  ```java
  warmupManager.startWarmup(uuid, "warps", "warp", () -> {
      executeTeleport(store, ref, destination);
  });
  ```
- Strip GUI open logic entirely
- When no args: list warps as text (like WarpsCommand fallback)
- Teleport execution: same Hytale API calls (World.execute, Teleport component)

### Step 4: Create WarpsCommand

Port from `HyperWarps/src/main/java/com/hyperwarps/command/warp/WarpsCommand.java`. Strip ALL GUI code (the `GuiManager.openWarpList()` block). Keep only the text list fallback logic. Use `CommandUtil` for all messages instead of `UIHelper`.

### Step 5: Create DelWarpCommand

Port from `HyperWarps/src/main/java/com/hyperwarps/command/warp/DelWarpCommand.java`. Straightforward — permission becomes `Permissions.WARP_DELETE`.

### Step 6: Create WarpInfoCommand

Port from `HyperWarps/src/main/java/com/hyperwarps/command/warp/WarpInfoCommand.java`. Permission becomes `Permissions.WARP_INFO`. Replace `UIHelper.header/secondary/primary` with `CommandUtil.info/success/msg`.

### Step 7: Wire WarpsModule

Update `WarpsModule.onEnable()` to:
1. Get `WarpStorage` from `HyperEssentials.getStorageProvider().getWarpStorage()`
2. Create `WarpManager` with the storage
3. Call `warpManager.loadWarps().join()`
4. Store reference to warpManager as field
5. Note: Commands are registered at the platform layer (Task 8), not here

Update `WarpsModule.onDisable()` to:
1. Call `warpManager.saveWarps().join()`

Add getter: `public WarpManager getWarpManager()`

### Step 8: Commit

```bash
git add src/main/java/com/hyperessentials/module/warps/
git commit --author="ZenithDevHQ <ZenithDevHQ@users.noreply.github.com>" -m "feat: add WarpManager and implement warps module with commands"
```

---

## Task 5: Implement Spawns Module

**Files:**
- Create: `src/main/java/com/hyperessentials/module/spawns/SpawnManager.java`
- Create: `src/main/java/com/hyperessentials/module/spawns/command/SpawnCommand.java`
- Create: `src/main/java/com/hyperessentials/module/spawns/command/SpawnsCommand.java`
- Create: `src/main/java/com/hyperessentials/module/spawns/command/SetSpawnCommand.java`
- Create: `src/main/java/com/hyperessentials/module/spawns/command/DelSpawnCommand.java`
- Create: `src/main/java/com/hyperessentials/module/spawns/command/SpawnInfoCommand.java`
- Modify: `src/main/java/com/hyperessentials/module/spawns/SpawnsModule.java`

### Step 1: Create SpawnManager

Port from `HyperWarps/src/main/java/com/hyperwarps/manager/SpawnManager.java`. Key adaptations:
- Package: `com.hyperessentials.module.spawns`
- Constructor takes `SpawnStorage` and `SpawnsConfig`
- Default spawn name from `SpawnsConfig.getDefaultSpawnName()` (not `ConfigManager.get().getDefaultSpawn()`)
- Uses `com.hyperessentials.integration.PermissionManager`

All methods port 1:1: `loadSpawns()`, `saveSpawns()`, `setSpawn()`, `getSpawn()`, `deleteSpawn()`, `getDefaultSpawn()`, `getSpawnForPlayer()`, `getSpawnForWorld()`, `getAllSpawns()`, `getAccessibleSpawns()`, `canAccess()`, `spawnExists()`, `setDefaultSpawn()`, `getSpawnNames()`, `getSpawnCount()`.

### Step 2: Create spawn commands

Same pattern as warps. Port each command from `HyperWarps/src/main/java/com/hyperwarps/command/spawn/`:
- `SetSpawnCommand` — permission `Permissions.SPAWN_SET`, supports `--default` flag
- `SpawnCommand` — permission `Permissions.SPAWN`, uses WarmupManager for teleport
- `SpawnsCommand` — permission `Permissions.SPAWN_LIST`, text list only (no GUI)
- `DelSpawnCommand` — permission `Permissions.SPAWN_DELETE`
- `SpawnInfoCommand` — permission `Permissions.SPAWN_INFO`

### Step 3: Wire SpawnsModule

Same pattern as WarpsModule. `onEnable()` creates SpawnManager with SpawnStorage, loads spawns. `onDisable()` saves. Add getter.

### Step 4: Commit

```bash
git add src/main/java/com/hyperessentials/module/spawns/
git commit --author="ZenithDevHQ <ZenithDevHQ@users.noreply.github.com>" -m "feat: add SpawnManager and implement spawns module with commands"
```

---

## Task 6: Implement Teleport Module (TPA + Back)

**Files:**
- Create: `src/main/java/com/hyperessentials/module/teleport/TpaManager.java`
- Create: `src/main/java/com/hyperessentials/module/teleport/BackManager.java`
- Create: `src/main/java/com/hyperessentials/module/teleport/command/TpaCommand.java`
- Create: `src/main/java/com/hyperessentials/module/teleport/command/TpaHereCommand.java`
- Create: `src/main/java/com/hyperessentials/module/teleport/command/TpAcceptCommand.java`
- Create: `src/main/java/com/hyperessentials/module/teleport/command/TpDenyCommand.java`
- Create: `src/main/java/com/hyperessentials/module/teleport/command/TpCancelCommand.java`
- Create: `src/main/java/com/hyperessentials/module/teleport/command/TpToggleCommand.java`
- Create: `src/main/java/com/hyperessentials/module/teleport/command/BackCommand.java`
- Modify: `src/main/java/com/hyperessentials/module/teleport/TeleportModule.java`

### Step 1: Create TpaManager

Port from `HyperWarps/src/main/java/com/hyperwarps/manager/TpaManager.java`. Key adaptations:
- Package: `com.hyperessentials.module.teleport`
- Constructor takes `PlayerDataStorage` and `TeleportConfig`
- Config values come from `TeleportConfig` (timeout, cooldown, maxPending) instead of `ConfigManager.get()`
- Permissions use `com.hyperessentials.Permissions` constants
- Uses `com.hyperessentials.data.*` records

All methods port 1:1: `loadPlayer()`, `savePlayer()`, `unloadPlayer()`, `getPlayerData()`, `getOrCreatePlayerData()`, `isAcceptingRequests()`, `toggleTpToggle()`, `createRequest()`, `getIncomingRequests()`, `getIncomingRequest()`, `getMostRecentIncomingRequest()`, `getOutgoingRequest()`, `acceptRequest()`, `denyRequest()`, `cancelOutgoingRequest()`, `getRemainingTpaCooldown()`, `hasPendingIncoming()`, `saveAll()`.

### Step 2: Create BackManager

Port from `HyperWarps/src/main/java/com/hyperwarps/manager/BackManager.java`. Key adaptations:
- Package: `com.hyperessentials.module.teleport`
- Constructor takes `TpaManager` and `TeleportConfig`
- Config values from `TeleportConfig` (historySize, saveOnDeath, saveOnTeleport) instead of `ConfigManager.get()`

All methods port 1:1: `saveBackLocation()`, `onTeleport()`, `onDeath()`, `getBackLocation()`, `popBackLocation()`, `hasBackHistory()`, `clearHistory()`, `getHistorySize()`.

### Step 3: Create TPA commands

Port each from `HyperWarps/src/main/java/com/hyperwarps/command/tpa/`:

- **TpaCommand** — `/tpa <player>`. Takes `TpaManager`. Resolves target player from tracked players. Creates TPA request. Sends messages to both players.
- **TpaHereCommand** — `/tpahere <player>`. Same but `Type.TPAHERE`.
- **TpAcceptCommand** — `/tpaccept [player]`. Gets most recent incoming request (or from specific player). Calls `tpaManager.acceptRequest()`. Triggers teleport via WarmupManager.
- **TpDenyCommand** — `/tpdeny [player]`. Denies request, notifies requester.
- **TpCancelCommand** — `/tpcancel`. Cancels outgoing request.
- **TpToggleCommand** — `/tptoggle`. Toggles acceptance state.

Key adaptation for all TPA commands: when a request is accepted, the actual teleport uses `WarmupManager.startWarmup()` instead of `TeleportManager.teleportToLocation()`.

### Step 4: Create BackCommand

Port from `HyperWarps/src/main/java/com/hyperwarps/command/BackCommand.java`:
- Permission: `Permissions.BACK`
- Gets `backManager.popBackLocation(uuid)`
- If null: "No back location available"
- If found: use WarmupManager for warmup, then execute teleport
- If teleport fails: re-add location to history

### Step 5: Wire TeleportModule

`onEnable()`:
1. Get PlayerDataStorage from StorageProvider
2. Get TeleportConfig from ConfigManager
3. Create TpaManager with storage and config
4. Create BackManager with TpaManager and config
5. Store references

`onDisable()`:
1. Save all player data via `tpaManager.saveAll().join()`

Add getters: `getTpaManager()`, `getBackManager()`

### Step 6: Commit

```bash
git add src/main/java/com/hyperessentials/module/teleport/
git commit --author="ZenithDevHQ <ZenithDevHQ@users.noreply.github.com>" -m "feat: add TpaManager, BackManager and implement teleport module with commands"
```

---

## Task 7: Implement RTP Module

**Files:**
- Create: `src/main/java/com/hyperessentials/module/rtp/RtpManager.java`
- Create: `src/main/java/com/hyperessentials/module/rtp/command/RtpCommand.java`
- Modify: `src/main/java/com/hyperessentials/module/rtp/RtpModule.java`
- Modify: `src/main/java/com/hyperessentials/config/modules/RtpConfig.java`

### Step 1: Flesh out RtpConfig

Add config fields to the existing stub:

```java
public class RtpConfig extends ModuleConfig {
    private int centerX = 0;
    private int centerZ = 0;
    private int minRadius = 100;
    private int maxRadius = 5000;
    private int maxAttempts = 10;
    private List<String> blacklistedWorlds = new ArrayList<>();

    // ... loadModuleSettings reads from JSON, writeModuleSettings writes to JSON
    // Getters for all fields
}
```

### Step 2: Create RtpManager

New class — not ported from HyperWarps.

```java
package com.hyperessentials.module.rtp;

public class RtpManager {
    private final RtpConfig config;
    private final Random random = new Random();

    public RtpManager(@NotNull RtpConfig config) {
        this.config = config;
    }

    // Generate random location within configured ring
    public @Nullable Location findRandomLocation(@NotNull String worldName) {
        if (config.getBlacklistedWorlds().contains(worldName.toLowerCase())) return null;

        int centerX = config.getCenterX();
        int centerZ = config.getCenterZ();
        int minR = config.getMinRadius();
        int maxR = config.getMaxRadius();

        // Random point in ring: angle + radius
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = minR + random.nextDouble() * (maxR - minR);
        double x = centerX + radius * Math.cos(angle);
        double z = centerZ + radius * Math.sin(angle);

        return new Location(worldName, x, 64, z, 0, 0);  // y=64 placeholder, adjusted at teleport time
    }

    public boolean isWorldBlacklisted(@NotNull String worldName) {
        return config.getBlacklistedWorlds().contains(worldName.toLowerCase());
    }

    public int getMaxAttempts() { return config.getMaxAttempts(); }
}
```

Note: Actual Y-coordinate resolution (finding safe ground) happens at the platform level when we have world access. The manager generates X/Z candidates.

### Step 3: Create RtpCommand

```java
package com.hyperessentials.module.rtp.command;

// /rtp - Teleport to a random location
public class RtpCommand extends AbstractPlayerCommand {
    private final RtpManager rtpManager;
    private final WarmupManager warmupManager;

    // execute():
    // 1. Permission check: Permissions.RTP
    // 2. Check world not blacklisted
    // 3. Generate random location via rtpManager.findRandomLocation()
    // 4. Save current location as back location (via BackManager if teleport module enabled)
    // 5. Start warmup via warmupManager.startWarmup(uuid, "rtp", "rtp", callback)
    // 6. In callback: resolve safe Y coordinate, execute teleport
}
```

### Step 4: Wire RtpModule

`onEnable()`: Create RtpManager with RtpConfig.
`onDisable()`: No state to save.
Add getter: `getRtpManager()`

### Step 5: Commit

```bash
git add src/main/java/com/hyperessentials/module/rtp/ src/main/java/com/hyperessentials/config/modules/RtpConfig.java
git commit --author="ZenithDevHQ <ZenithDevHQ@users.noreply.github.com>" -m "feat: implement RTP module with random location finding and safety checks"
```

---

## Task 8: Wire Platform Events and Command Registration

**Files:**
- Modify: `src/main/java/com/hyperessentials/platform/HyperEssentialsPlugin.java`
- Modify: `src/main/java/com/hyperessentials/HyperEssentials.java`
- Create: `src/main/java/com/hyperessentials/listener/DeathListener.java`

### Step 1: Update HyperEssentials core

Add convenience getters for accessing module managers:

```java
// In HyperEssentials.java, add after existing getters:
@Nullable
public WarpsModule getWarpsModule() { return moduleRegistry.getModule(WarpsModule.class); }
@Nullable
public SpawnsModule getSpawnsModule() { return moduleRegistry.getModule(SpawnsModule.class); }
@Nullable
public TeleportModule getTeleportModule() { return moduleRegistry.getModule(TeleportModule.class); }
@Nullable
public RtpModule getRtpModule() { return moduleRegistry.getModule(RtpModule.class); }
```

### Step 2: Create DeathListener

Port from `HyperWarps/src/main/java/com/hyperwarps/listener/DeathListener.java`. Adapted for HE:

```java
package com.hyperessentials.listener;

public class DeathListener {
    // onPlayerDeath(UUID uuid, Location deathLocation):
    //   - Get BackManager from TeleportModule (if enabled)
    //   - Call backManager.onDeath(uuid, deathLocation)

    // onPlayerRespawn(UUID uuid):
    //   - Get SpawnsModule (if enabled) and SpawnsConfig
    //   - If teleportOnRespawn: get spawn for player, teleport there
}
```

### Step 3: Update HyperEssentialsPlugin — register commands

In `registerCommands()`, conditionally register commands based on module state:

```java
private void registerCommands() {
    HyperEssentials he = hyperEssentials;

    // Admin
    getCommandRegistry().registerCommand(new AdminCommand());

    // Warps (if module exists and enabled)
    WarpsModule warps = he.getWarpsModule();
    if (warps != null && warps.isEnabled()) {
        WarpManager wm = warps.getWarpManager();
        getCommandRegistry().registerCommand(new WarpCommand(wm, he.getWarmupManager(), /* ... */));
        getCommandRegistry().registerCommand(new WarpsCommand(wm));
        getCommandRegistry().registerCommand(new SetWarpCommand(wm, ConfigManager.get().warps()));
        getCommandRegistry().registerCommand(new DelWarpCommand(wm));
        getCommandRegistry().registerCommand(new WarpInfoCommand(wm));
    }

    // Spawns (if module exists and enabled)
    // ... same pattern

    // Teleport (if module exists and enabled)
    // ... TPA commands + BackCommand

    // RTP (if module exists and enabled)
    // ... RtpCommand
}
```

### Step 4: Update event listeners

In `registerEventListeners()`, add:
- **PlayerConnect**: Load player TPA data if teleport module enabled
- **PlayerDisconnect**: Unload player data, cancel TPA requests
- **Death events**: Hook into death for back location + respawn teleport

```java
private void onPlayerConnect(PlayerConnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    trackedPlayers.put(playerRef.getUuid(), playerRef);

    // Load teleport data
    TeleportModule tm = hyperEssentials.getTeleportModule();
    if (tm != null && tm.isEnabled()) {
        tm.getTpaManager().loadPlayer(playerRef.getUuid(), playerRef.getUsername());
    }
}

private void onPlayerDisconnect(PlayerDisconnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    trackedPlayers.remove(playerRef.getUuid());

    hyperEssentials.getWarmupManager().cancelWarmup(playerRef.getUuid());
    hyperEssentials.getGuiManager().getPageTracker().unregister(playerRef.getUuid());

    // Unload teleport data
    TeleportModule tm = hyperEssentials.getTeleportModule();
    if (tm != null && tm.isEnabled()) {
        tm.getTpaManager().unloadPlayer(playerRef.getUuid());
    }
}
```

### Step 5: Add movement checking for warmup cancellation

Add a `ScheduledExecutorService` that polls every 100ms for players with active warmups:

```java
private ScheduledExecutorService movementChecker;

// In start():
movementChecker = Executors.newSingleThreadScheduledExecutor();
movementChecker.scheduleAtFixedRate(this::checkMovement, 100, 100, TimeUnit.MILLISECONDS);

// In shutdown():
if (movementChecker != null) movementChecker.shutdownNow();

private void checkMovement() {
    WarmupManager wm = hyperEssentials.getWarmupManager();
    if (!ConfigManager.get().warmup().isCancelOnMove()) return;

    for (Map.Entry<UUID, PlayerRef> entry : trackedPlayers.entrySet()) {
        UUID uuid = entry.getKey();
        if (!wm.hasActiveWarmup(uuid)) continue;
        // Check if player has moved from warmup start position
        // If moved > threshold: wm.cancelWarmup(uuid) + send message
    }
}
```

Note: Movement checking requires storing the start position when a warmup begins. This needs a small extension — a `Map<UUID, Location>` in the plugin that records position at warmup start.

### Step 6: Commit

```bash
git add src/main/java/com/hyperessentials/platform/ src/main/java/com/hyperessentials/HyperEssentials.java src/main/java/com/hyperessentials/listener/
git commit --author="ZenithDevHQ <ZenithDevHQ@users.noreply.github.com>" -m "feat: wire platform events, movement checking, and command registration"
```

---

## Task 9: Update API, Permissions, and Documentation

**Files:**
- Modify: `src/main/java/com/hyperessentials/api/HyperEssentialsAPI.java`
- Modify: `src/main/java/com/hyperessentials/Permissions.java`
- Modify: `docs/commands.md`
- Modify: `docs/permissions.md`
- Modify: `docs/modules.md`
- Modify: `docs/storage.md`

### Step 1: Extend HyperEssentialsAPI

Add convenience methods for external plugin access:

```java
// Warp API
public static @Nullable Warp getWarp(String name) { ... }
public static Collection<Warp> getAllWarps() { ... }
public static List<Warp> getAccessibleWarps(UUID uuid) { ... }

// Spawn API
public static @Nullable Spawn getSpawn(String name) { ... }
public static @Nullable Spawn getDefaultSpawn() { ... }
public static @Nullable Spawn getSpawnForPlayer(UUID uuid) { ... }

// Back API
public static void saveBackLocation(UUID uuid, Location location) { ... }
public static boolean hasBackHistory(UUID uuid) { ... }

// TPA API
public static boolean isAcceptingTpa(UUID uuid) { ... }
```

Each method checks `isAvailable()` and module enabled state before delegating.

### Step 2: Verify Permissions.java completeness

Current `Permissions.java` already has all needed constants. Verify these exist and match command usage:
- `WARP`, `WARP_SET`, `WARP_DELETE`, `WARP_LIST`, `WARP_INFO`
- `SPAWN`, `SPAWN_SET`, `SPAWN_DELETE`, `SPAWN_LIST`, `SPAWN_INFO`
- `TPA`, `TPAHERE`, `TPACCEPT`, `TPDENY`, `TPCANCEL`, `TPTOGGLE`, `BACK`
- `RTP`
- `BYPASS_WARMUP`, `BYPASS_COOLDOWN`, `BYPASS_LIMIT`

Add if missing: `BYPASS_TOGGLE` for TPA toggle bypass.

### Step 3: Update documentation

Update module docs to reflect implemented state:
- `docs/modules.md` — change warps/spawns/teleport/rtp status from "Stub (TODO)" to "Implemented"
- `docs/commands.md` — document all 18 commands with usage and permissions
- `docs/permissions.md` — full permission tree
- `docs/storage.md` — document `data/` directory layout and JSON formats

### Step 4: Commit

```bash
git add src/main/java/com/hyperessentials/api/ src/main/java/com/hyperessentials/Permissions.java docs/
git commit --author="ZenithDevHQ <ZenithDevHQ@users.noreply.github.com>" -m "feat: update API, permissions, and documentation"
```

---

## Key Adaptation Notes

### WarmupManager Integration Pattern

Every teleport command follows this pattern:

```java
// Check cooldown first
WarmupManager warmup = hyperEssentials.getWarmupManager();
if (warmup.isOnCooldown(uuid, "warps", "warp")) {
    int remaining = warmup.getRemainingCooldown(uuid, "warps", "warp");
    ctx.sendMessage(CommandUtil.error("On cooldown. " + remaining + "s remaining."));
    return;
}

// Start warmup (callback runs on completion)
WarmupTask task = warmup.startWarmup(uuid, "warps", "warp", () -> {
    // Actual teleport execution
    executeTeleport(store, ref, destination);
    ctx.sendMessage(CommandUtil.success("Teleported to warp '" + name + "'!"));
});

if (task != null) {
    ctx.sendMessage(CommandUtil.info("Teleporting in " + task.warmupSeconds() + "s... Don't move!"));
}
```

### Teleport Execution Pattern

All modules share this same teleport execution:

```java
private void executeTeleport(Store<EntityStore> store, Ref<EntityStore> ref, Location dest) {
    World targetWorld = Universe.get().getWorld(dest.world());
    if (targetWorld == null) {
        // Handle world not found
        return;
    }
    targetWorld.execute(() -> {
        Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
        Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
        Teleport teleport = new Teleport(targetWorld, position, rotation);
        store.addComponent(ref, Teleport.getComponentType(), teleport);
    });
}
```

### Message Pattern

Strip all `UIHelper` usage. Use only `CommandUtil`:
- `CommandUtil.error("message")` — red error
- `CommandUtil.success("message")` — green success
- `CommandUtil.info("message")` — secondary info
- `CommandUtil.prefix()` — branded prefix `[HyperEssentials]`

### File Summary

**New files (26):**
- 4 data models
- 1 listener
- 1 warp manager + 5 warp commands
- 1 spawn manager + 5 spawn commands
- 2 teleport managers + 7 teleport commands
- 1 rtp manager + 1 rtp command (+ config update)

**Modified files (8):**
- Location.java (add utility methods)
- WarpStorage.java, SpawnStorage.java, PlayerDataStorage.java (flesh out interfaces)
- JsonStorageProvider.java (implement storage)
- WarpsModule.java, SpawnsModule.java, TeleportModule.java, RtpModule.java (fill stubs)
- HyperEssentialsPlugin.java (commands + events)
- HyperEssentials.java (module getters)
- HyperEssentialsAPI.java (public API)
- Permissions.java (add BYPASS_TOGGLE if missing)
- RtpConfig.java (add fields)
