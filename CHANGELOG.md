# Changelog

All notable changes to HyperEssentials will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

#### RTP Overhaul
- Chunk-based random location selection with multi-attempt safety verification
- `findSafeY()` heightmap scan with `BlockMaterial` + fluid-aware safety checks (avoidWater, avoidDangerousFluids)
- Faction claim avoidance with configurable chunk buffer radius via `HyperFactionsIntegration`
- `BorderHook` interface for future HyperBorder world border enforcement
- `RtpChunkUtil` utility for 32-block chunk coordinate math
- `RtpResult` sealed interface with `Success(Location)` and `Failure(String)` records
- `playerRelative` config option — center RTP ring on player position (default) or fixed `centerX/Z`
- `rtp.factionAvoidance` config subsection (`enabled`, `bufferRadius`)
- `rtp.safety` config subsection (`avoidWater`, `avoidDangerousFluids`, `minY`, `maxY`)
- `hyperessentials.rtp.bypass.factions` permission node
- "Searching for a safe random location..." feedback message before search begins
- Found coordinates shown in warmup message: "Found location at (X, Y, Z). Teleporting in Ns..."

#### Documentation
- Updated README with complete module table (status, default state), optional dependencies, command quick reference, and docs index
- Updated architecture docs with full package tree including all data records, modules, managers, commands, listeners, and migration framework
- Updated commands docs with all 46 commands across 9 modules (kits, moderation, utility, announcements)
- Updated integrations docs with HyperFactions territory checker, VaultUnlocked economy, LuckPerms, and current Ecotale/Werchat status
- Updated modules docs with correct config file paths, implementation status, and debug configuration section
- Updated storage docs with kits/punishments storage, complete data directory layout, and migration system
- Updated docs index (readme.md) with completion status for all documentation pages

### Changed

#### RTP Overhaul
- RTP search now runs on world thread via `currentWorld.execute()` for chunk/block access
- Player position obtained via `TransformComponent` (matches HyperFactions stuck pattern)
- Replaced `java.util.Random` with `ThreadLocalRandom`
- `maxAttempts` config now actually used (was previously ignored)
- Y coordinate resolved from heightmap scan instead of hardcoded 64

### Removed

#### RTP Overhaul
- `findRandomLocation(String worldName)` method (replaced by `findSafeRandomLocation()`)

#### Documentation
- `docs/plans/2026-02-22-core-modules-design.md` — completed design plan (implemented)
- `docs/plans/2026-02-22-core-modules-implementation.md` — completed implementation plan (implemented)

### Added (prior work)

#### Homes Module
- `Home` immutable record with timestamps, `create()` factory, `withLastUsed()` and `withLocation()` update methods
- `PlayerHomes` collection with case-insensitive lookup (lowercase keys, original casing preserved)
- `HomeManager` with CRUD operations, player cache lifecycle (load on connect, flush on disconnect), permission-based limit resolution (unlimited → limit.N → config default), and bed sync
- `Location.fromHome()` factory method for consistency with existing `fromWarp()` and `fromSpawn()`
- `HomeStorage` interface with `loadPlayerHomes()` and `savePlayerHomes()` methods
- `JsonHomeStorage` implementation with per-player JSON files in `data/players/homes/`, atomic writes
- `/sethome [name]` — set a home with name validation (`[a-zA-Z0-9_-]{1,32}`), faction territory check, and limit enforcement
- `/home [name]` — teleport to a home with faction territory check on destination, warmup/cooldown via WarmupManager
- `/delhome <name>` — delete a home with available homes listed on error
- `/homes` — list all homes with count/limit display (e.g. "Your homes (2/3)")

#### HyperFactions Integration
- HyperFactions added to `manifest.json` SoftDependencies for load ordering
- `FactionTerritoryChecker` with `Result` enum (ALLOWED, BLOCKED_OWN/ALLY/ENEMY/NEUTRAL/WILDERNESS) and `canUseHome()` static method
- `HomesConfig` factions subsection with per-relationship toggles: `enabled` (master toggle), `allowInOwnTerritory`, `allowInAllyTerritory`, `allowInNeutralTerritory`, `allowInEnemyTerritory`, `allowInWilderness`
- Faction bypass permissions: `bypass.factions` (wildcard), `bypass.factions.sethome`, `bypass.factions.home`
- `HOME_TELEPORT` and `HOME_LIMIT_PREFIX` permission constants

#### Infrastructure
- Checkstyle linting (10.26.1) with 2-space indent, 120-char lines, relaxed Javadoc rules
- Category-based debug logging via `Logger.DebugCategory` (12 categories: homes, warps, spawns, teleport, kits, moderation, utility, rtp, announcements, integration, economy, storage)
- `DebugConfig` module config with per-category toggle and `applyToLogger()`
- Migration framework: `Migration` interface, `MigrationType` enum, `MigrationOptions`, `MigrationResult`, `MigrationRegistry`, `MigrationRunner` with ZIP backup and rollback
- VaultUnlocked economy integration via `VaultEconomyProvider` (reflection-based lazy init)
- Standard directory structure: `config/`, `data/`, `data/players/`, `backups/`, `data/.version` marker
- `ConfigFile.hasNewKeys()` auto-detection for missing config keys on load

### Changed

#### Infrastructure
- Reformatted all Java files to 2-space indentation (aligned with HyperFactions code style)
- Logger upgraded from `java.util.logging.Logger` to HytaleLogger (Flogger) with `logger.at(Level).log()` pattern
- RTP module merged into Teleport module (RtpManager, RtpCommand moved to `module.teleport` package)
- RTP config merged into `TeleportConfig` under `"rtp"` subsection
- `EcotaleIntegration` now uses reflection detection instead of stub
- `manifest.json` soft dependencies expanded: HyperPerms, VaultUnlocked, LuckPerms

### Removed
- Standalone RTP module (`module.rtp` package, `RtpConfig`)

### Added (continued)

#### Infrastructure (from prior work)
- `DurationParser` utility for parsing human-readable durations ("1h30m", "7d") and formatting
- Categorized `Permissions` constants: KIT, MODERATION, UTILITY, ANNOUNCE, BYPASS, NOTIFY with wildcards
- `CommandUtil.findOnlinePlayer()` for case-insensitive online player lookup
- Disconnect handler system in `HyperEssentials` core for session cleanup callbacks

#### Kits Module
- `Kit` and `KitItem` data records for kit definitions
- `KitStorage` for JSON persistence of kit definitions (`data/kits.json`)
- `KitManager` with CRUD operations, cooldown tracking, one-time claim tracking, and inventory capture
- `/kit <name>` — claim a kit with permission and cooldown checks
- `/kits` — list available kits filtered by player permissions
- `/createkit <name>` — capture current inventory as a new kit definition
- `/deletekit <name>` — remove a kit definition
- `KitsConfig` with `defaultCooldownSeconds` and `oneTimeDefault` settings

#### Moderation Module
- `Punishment` record with ban/mute/kick types, expiry, and revocation tracking
- `ModerationStorage` for JSON persistence of punishment history (`data/punishments.json`)
- `ModerationManager` for ban/mute/kick operations with broadcast and staff notifications
- `FreezeManager` with position-locked movement checking via `ScheduledExecutorService`
- `VanishManager` using `HiddenPlayersManager` with fake disconnect/connect messages
- `ModerationListener` for ban enforcement on connect and mute enforcement on chat
- `/ban`, `/tempban`, `/unban` — permanent and temporary bans with reason support
- `/mute`, `/tempmute`, `/unmute` — permanent and temporary mutes
- `/kick` — kick with custom reason, broadcast, and staff notifications
- `/freeze` — toggle position freeze with movement prevention
- `/vanish` — toggle vanish with configurable fake leave/join messages
- `/punishments <player>` — view punishment history
- `ModerationConfig` with default messages, broadcast toggles, freeze interval, history limits
- `VanishConfig` with fake message toggles and vanish messages

#### Utility Module
- `UtilityManager` for session-only fly/god state tracking (cleared on disconnect)
- `/heal [player]` — heal via `EntityStatsModule` stat maximization
- `/fly [player]` — toggle Creative/Adventure mode as flight workaround
- `/god [player]` — toggle `Invulnerable` ECS component
- `/clearchat [player]` — clear chat with configurable line count
- `/clearinventory [player]` (alias `/ci`) — clear inventory via `Player` component
- `/repair` — repair held item durability via `withRestoredDurability()`
- `/near [radius]` — list nearby players with configurable default/max radius
- `UtilityConfig` with per-command enable flags, radius limits, and chat line settings

#### Announcements Module
- `AnnouncementScheduler` with configurable interval, sequential/random rotation
- `/broadcast <message>` — send formatted announcement to all players
- `/announce list|add|remove|reload` — manage announcement rotation
- `AnnouncementsConfig` with interval, randomize, prefix/message colors, and message list

## [0.1.0] - 2026-02-18

### Added
- Initial project scaffold with modular architecture (11 modules)
- Module system: `Module` interface, `AbstractModule` base class, `ModuleRegistry` lifecycle manager
- Module stubs: homes, warps, spawns, teleport, warmup, kits, moderation, vanish, utility, announcements, rtp
- Configuration system with `ConfigFile` base, `CoreConfig`, and per-module config files
- `ConfigManager` singleton with load, reload, save, and validation
- Permission integration with HyperPerms via reflection-based soft dependency
- `PermissionManager` with chain-of-responsibility resolution (HyperPerms -> OP -> config fallback)
- GUI framework: `PageRegistry`, `GuiManager`, `NavBarHelper`, `ActivePageTracker`, `RefreshablePage`
- Shared UI templates: styles, navigation bar, error page
- Universal warmup/cooldown system: `WarmupManager`, `CooldownTracker`, per-module config
- Storage framework: `StorageProvider` interface with `HomeStorage`, `WarpStorage`, `SpawnStorage`, `PlayerDataStorage`
- `JsonStorageProvider` stub implementation
- Admin command `/hessentials` with reload and version subcommands
- Soft dependency stubs for HyperFactions (territory), Ecotale (economy), Werchat (chat)
- Public API via `HyperEssentialsAPI` singleton
- Event bus for cross-module communication
- Shared utilities: `Logger`, `TimeUtil`, `CommandUtil`, `UIHelper`, `Location` record
- Full `Permissions` constants class organized by module
- Documentation: architecture, commands, config, permissions, modules, GUI, storage, integrations, API, warmup, migration
