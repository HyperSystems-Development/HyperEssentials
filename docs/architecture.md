# Architecture

> **Status:** Scaffold only — details will be filled in as modules are implemented.

## Overview

HyperEssentials is a modular server essentials plugin. Each feature area (homes, warps, spawns, etc.) is encapsulated as a **module** that can be independently enabled or disabled.

## Package Structure

```
com.hyperessentials/
  HyperEssentials.java          Core singleton — lifecycle, subsystem orchestration
  Permissions.java              All permission node constants
  BuildInfo.java                Auto-generated version info

  platform/
    HyperEssentialsPlugin.java  Hytale JavaPlugin entry point

  api/
    HyperEssentialsAPI.java     Public API for cross-plugin access
    events/
      EventBus.java             Simple publish-subscribe event bus

  config/
    ConfigFile.java             Abstract JSON config base class
    ModuleConfig.java           Abstract module config with enabled flag
    ValidationResult.java       Config validation collector
    CoreConfig.java             Root plugin settings
    ConfigManager.java          Singleton config orchestrator
    modules/                    Per-module config classes (11 files)

  module/
    Module.java                 Module interface
    AbstractModule.java         Base implementation
    ModuleRegistry.java         Lifecycle manager
    homes/                      Home management module
    warps/                      Warp management module
    spawns/                     Spawn management module
    teleport/                   TPA and /back module
    warmup/                     Universal warmup/cooldown
    kits/                       Kit system module
    moderation/                 Moderation tools module
    vanish/                     Vanish module
    utility/                    Utility commands module
    announcements/              Broadcast module
    rtp/                        Random teleport module

  gui/
    GuiManager.java             Central GUI hub
    PageRegistry.java           Dynamic page registration
    NavBarHelper.java           Shared navigation bar
    ActivePageTracker.java      Player-to-page tracking
    UIHelper.java               Formatting utilities
    RefreshablePage.java        Push-refresh interface
    PageSupplier.java           Page factory interface
    GuiType.java                PLAYER / ADMIN enum

  storage/
    StorageProvider.java        Top-level storage interface
    HomeStorage.java            Home CRUD interface
    WarpStorage.java            Warp CRUD interface
    SpawnStorage.java           Spawn CRUD interface
    PlayerDataStorage.java      Per-player data interface
    json/
      JsonStorageProvider.java  JSON file implementation

  integration/
    PermissionProvider.java     Permission provider interface
    PermissionManager.java      Chain-of-responsibility resolver
    HyperPermsProviderAdapter.java  Reflection-based HyperPerms bridge
    HyperFactionsIntegration.java   Territory/relation lookups
    EcotaleIntegration.java     Economy hooks (stub)
    WerchatIntegration.java     Chat hooks (stub)

  command/
    AdminCommand.java           /hessentials admin command
    util/
      CommandUtil.java          Messaging and permission helpers

  data/
    Location.java               Immutable location record

  util/
    Logger.java                 Wrapped logger with prefix
    TimeUtil.java               Duration formatting
```

## Module Lifecycle

1. `HyperEssentialsPlugin.setup()` — creates core singleton
2. `HyperEssentialsPlugin.start()` — calls `HyperEssentials.enable()`
3. `HyperEssentials.enable()`:
   - Loads config
   - Initializes integrations
   - Initializes storage
   - Registers all modules in `ModuleRegistry`
   - Enables modules whose config has `enabled = true`
4. `HyperEssentialsPlugin.shutdown()` — calls `HyperEssentials.disable()`
5. `HyperEssentials.disable()`:
   - Disables modules in reverse order
   - Shuts down storage
   - Saves config

## Cross-Module Communication

Modules communicate through:
- **ConfigManager** — shared configuration access
- **EventBus** — publish-subscribe for decoupled events
- **GuiManager** — shared page registry for navigation
- **WarmupManager** — centralized warmup/cooldown tracking
