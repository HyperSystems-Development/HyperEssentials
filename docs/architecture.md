# Architecture

## Overview

HyperEssentials is a modular server essentials plugin. Each feature area (homes, warps, spawns, etc.) is encapsulated as a **module** that can be independently enabled or disabled.

## Package Structure

```
com.hyperessentials/
  HyperEssentials.java          Core singleton — lifecycle, subsystem orchestration
  Permissions.java              All permission node constants (organized by module)
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
    modules/                    Per-module config classes (12 files)
      AnnouncementsConfig.java
      DebugConfig.java
      HomesConfig.java
      KitsConfig.java
      ModerationConfig.java
      SpawnsConfig.java
      TeleportConfig.java       (includes RTP subsection)
      UtilityConfig.java
      VanishConfig.java
      WarmupConfig.java
      WarpsConfig.java

  data/
    Home.java                   Immutable home record (name, world, xyz, yaw, pitch, timestamps)
    Location.java               Immutable location record with factory methods
    PlayerHomes.java            Per-player home collection (case-insensitive lookup)
    PlayerTeleportData.java     TPA toggle, back history, last TPA timestamp
    Spawn.java                  Immutable spawn record (name, world, xyz, permission, group, default)
    TeleportRequest.java        TPA/TPAHere request record (expiry tracking)
    Warp.java                   Immutable warp record (name, category, permission, description)

  module/
    Module.java                 Module interface
    AbstractModule.java         Base implementation
    ModuleRegistry.java         Lifecycle manager
    homes/
      HomesModule.java          Module entry point
      HomeManager.java          CRUD, cache, limit resolution, bed sync
      command/                  SetHomeCommand, HomeCommand, DelHomeCommand, HomesCommand
    warps/
      WarpsModule.java
      WarpManager.java          CRUD, access control (custom warp permissions)
      command/                  SetWarpCommand, WarpCommand, DelWarpCommand, WarpsCommand, WarpInfoCommand
    spawns/
      SpawnsModule.java
      SpawnManager.java         CRUD, group routing, default spawn resolution
      command/                  SetSpawnCommand, SpawnCommand, DelSpawnCommand, SpawnsCommand, SpawnInfoCommand
    teleport/
      TeleportModule.java
      TpaManager.java           TPA request lifecycle, expiry, pending limits
      BackManager.java          Back location history (per-player ring buffer)
      RtpManager.java           Random location generation (ring radius)
      command/                  TpaCommand, TpaHereCommand, TpAcceptCommand, TpDenyCommand,
                                TpCancelCommand, TpToggleCommand, BackCommand, RtpCommand
    warmup/
      WarmupModule.java
      WarmupManager.java        Per-player warmup tasks
      WarmupTask.java           Individual warmup tracking
      CooldownTracker.java      Post-teleport cooldown tracking
    kits/
      KitsModule.java
      KitManager.java           CRUD, cooldown/one-time tracking, inventory capture
      data/
        Kit.java                Kit definition record
        KitItem.java            Kit item data
      storage/
        KitStorage.java         JSON persistence (data/kits.json)
      command/                  KitCommand, KitsCommand, CreateKitCommand, DeleteKitCommand
    moderation/
      ModerationModule.java
      ModerationManager.java    Ban/mute/kick operations, broadcasts, notifications
      FreezeManager.java        Position-lock via 100ms check loop + ECS teleport
      VanishManager.java        HiddenPlayersManager API + fake join/leave messages
      data/
        Punishment.java         Punishment record (type, expiry, revocation)
        PunishmentType.java     BAN, MUTE, KICK enum
      listener/
        ModerationListener.java Ban enforcement on connect, mute enforcement on chat
      storage/
        ModerationStorage.java  JSON persistence (data/punishments.json)
      command/                  BanCommand, TempBanCommand, UnbanCommand, MuteCommand,
                                TempMuteCommand, UnmuteCommand, KickCommand, FreezeCommand,
                                VanishCommand, PunishmentsCommand
    utility/
      UtilityModule.java
      UtilityManager.java       Session-only fly/god state tracking
      command/                  HealCommand, FlyCommand, GodCommand, ClearChatCommand,
                                ClearInventoryCommand, RepairCommand, NearCommand
    announcements/
      AnnouncementsModule.java
      AnnouncementScheduler.java Configurable interval, sequential/random rotation
      command/                  AnnounceCommand, BroadcastCommand
    vanish/
      VanishModule.java         Standalone vanish module (stub, not yet implemented)

  gui/
    GuiManager.java             Central GUI hub (openPlayerPage, openAdminPage)
    GuiColors.java              Semantic color constants (brand, text, status, backgrounds)
    UIPaths.java                Centralized UI template path constants
    PageRegistry.java           Dynamic page registration
    NavBarHelper.java           Shared navigation bar (player + admin variants)
    PlayerPageOpener.java       Static utility to open player pages
    AdminPageOpener.java        Static utility to open admin pages
    ActivePageTracker.java      Player-to-page tracking
    UIHelper.java               Formatting utilities (coords, duration, playtime)
    RefreshablePage.java        Push-refresh interface
    PageSupplier.java           Page factory interface
    GuiType.java                PLAYER / ADMIN enum
    data/
      PlayerPageData.java       BuilderCodec for player page events
      AdminPageData.java        BuilderCodec for admin page events
    player/
      HomesPage.java            Browse homes, teleport (warmup), delete
      WarpsPage.java            Browse warps by category, teleport (warmup)
      KitsPage.java             Browse kits, claim with cooldown, preview
    admin/                      Admin page implementations (Phase 4-5)

  storage/
    StorageProvider.java        Top-level storage interface
    HomeStorage.java            Home CRUD interface
    WarpStorage.java            Warp CRUD interface
    SpawnStorage.java           Spawn CRUD interface
    PlayerDataStorage.java      Per-player data interface
    json/
      JsonStorageProvider.java  JSON file implementation (atomic writes)

  integration/
    PermissionProvider.java     Permission provider interface
    PermissionManager.java      Chain-of-responsibility resolver
    HyperPermsProviderAdapter.java  Reflection-based HyperPerms bridge
    HyperFactionsIntegration.java   Territory/relation lookups (reflection)
    FactionTerritoryChecker.java    Home territory restriction logic
    EcotaleIntegration.java     Economy detection (reflection)
    WerchatIntegration.java     Chat hooks (stub)
    economy/
      VaultEconomyProvider.java VaultUnlocked 2 reflection integration

  listener/
    DeathListener.java          Saves back-location on death

  migration/
    Migration.java              Migration interface
    MigrationType.java          CONFIG, DATA, SCHEMA enum
    MigrationOptions.java       Migration parameters
    MigrationResult.java        Success/failure tracking
    MigrationRegistry.java      Version chain ordering
    MigrationRunner.java        Backup (ZIP) + execute + rollback on failure

  command/
    AdminCommand.java           /hessentials admin command
    util/
      CommandUtil.java          Messaging, permission helpers, player lookup

  util/
    DurationParser.java         Human-readable duration parsing ("1h30m", "7d")
    Logger.java                 HytaleLogger wrapper with prefix and debug categories
    TimeUtil.java               Duration formatting
```

## Module Lifecycle

1. `HyperEssentialsPlugin.setup()` — creates core singleton
2. `HyperEssentialsPlugin.start()` — calls `HyperEssentials.enable()`
3. `HyperEssentials.enable()`:
   - Runs pending migrations (backup + execute)
   - Loads config
   - Initializes integrations (HyperPerms, HyperFactions, VaultUnlocked)
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
- **PermissionManager** — unified permission resolution across providers
