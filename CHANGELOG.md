# Changelog

All notable changes to HyperEssentials will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

#### Localization (i18n)
- Built-in localization system with 10 languages: English, German, Spanish, French, Italian, Dutch, Polish, Portuguese, Russian, Filipino
- ~500 translation keys covering all commands, GUI labels, admin messages, and error text
- Player language auto-detection from client locale with per-player override
- Player Settings page with language selector accessible from GUI nav bar
- `I18nModule` with HyperFactions-compatible key format (`hyperessentials_` prefix for admin, `hyperessentials.` for player commands)
- Localized `.lang` files in `Server/Languages/<locale>/` following Hytale conventions

#### Sentry Error Tracking
- Sentry SDK integration for automated error reporting
- `ErrorHandler` utility wrapping all async operations with contextual breadcrumbs
- All modules migrated to use `ErrorHandler` for consistent error capture

#### Data Import System
- `/essimport` command with adapter pattern for migrating from other essentials plugins
- Importers for 4 plugins: EliteEssentials, Essentials (nhulston), EssentialsPlus, and HyEssentialsX
- `AbstractEssentialsImporter` base class with home/warp/spawn/kit import lifecycle
- `WorldResolver` utility for mapping world names across plugin formats
- ZIP backup created automatically before import

#### Admin GUI Enhancements
- **Config Editor** — In-game config editing with tabbed sections for all 13 module configs, boolean toggles, numeric steppers, text inputs, and save/discard
- **Player Moderation Page** — Per-player punishment actions with type selection and duration input
- **Updates Page** — Check for plugin updates from within the admin GUI
- **Player Search** — `PlayerResolver` and `PlayerDBService` for online/offline player lookup across admin pages
- **Bypass Toggle** — Admin bypass enable/disable system gating all bypass permissions (except home limits)
- `ConfigSnapshot` tracking for validating config changes before save

#### Player GUI Enhancements
- **Player Settings Page** — Locale selector with auto-detect language toggle, accessible from nav bar
- **Kit Edit Modal** — Edit kit display name, cooldown, one-time, permission, and per-item delete with preview
- **Warp Edit Modal** — Edit warp display name, category, description, and permission
- **Warp/Kit Name Input** — Name input dialog when creating warps and kits from admin GUI

#### Backup System
- Automatic ZIP backup on data import and manual backup via admin GUI
- Backup storage in `backups/` directory with timestamped filenames

#### Chat Delegation
- `ChatDelegation` coordinator for priority-based chat handling across Werchat, HyperPerms, and built-in formatters
- Automatic detection of installed chat plugins with graceful fallback

#### HyperFactions 0.12.0 API Integration
- Direct API methods via `HyperFactionsIntegration.Delegate` for territory queries
- `ESSENTIALS_BACK` zone flag support for `/back` territory restrictions
- `PlayerTerritoryChangeEvent` subscription for territory-aware features
- Bidirectional language preference sync between HyperEssentials and HyperFactions

#### HyperPerms Integration
- `HyperPermsProviderAdapter` for direct HyperPerms API access
- Quick-add warp/kit permissions to HyperPerms roles from admin GUI (`admin_permission_add.ui`)

#### Announcements Overhaul
- `CronScheduler` — 5-field cron expression parser for per-announcement scheduling
- Event announcements — join, leave, and first-join event handlers with placeholder support
- Announcement type toggle (CHAT vs NOTIFICATION) with per-announcement configuration
- Full CRUD for announcements via admin GUI

#### Warn Command
- `/warn <player> [reason]` — issue warnings with staff notification broadcast
- WARN punishment type tracked in punishment history

#### /back Territory Restrictions
- Source tracking for `/back` locations (death, home, warp, spawn, rtp, tpa, factionhome)
- Faction territory checks on `/back` destination with `ESSENTIALS_BACK` zone flag support
- `backFactions` config subsection with per-relationship toggles (mirrors homes config)

#### GUI Foundation Infrastructure
- `GuiColors` — centralized semantic color constants (brand gold, text, status, backgrounds, dividers)
- `UIPaths` — centralized UI template path constants for all shared, player, and admin pages
- `PlayerPageData` — BuilderCodec for player page events (Button, NavTarget, Target, Page)
- `AdminPageData` — BuilderCodec for admin page events (Button, NavTarget, Target, Value, Filter, Page)
- `PlayerPageOpener` / `AdminPageOpener` — static utilities to resolve and open pages from registries
- `GuiManager.openPlayerPage()` / `openAdminPage()` — convenience methods for page opening
- `NavBarHelper.setupAdminBar()` — admin nav bar variant with "HE Admin" title
- `NavBarHelper.handleNavEvent()` — overload with `GuiType` for admin/player nav routing
- `UIHelper.formatPlaytime()` — human-readable playtime formatting (Xd Xh Xm)
- `hyperessentials.admin.gui` permission node for admin panel access
- `/he` (no args) now opens player dashboard GUI (falls back to help text if no pages registered)
- `/he admin` opens admin dashboard GUI (requires `admin.gui` permission)
- Shared `.ui` templates: `styles.ui` (HyperFactions-quality TextButtonStyle definitions), `empty_state.ui`, `confirm_modal.ui`, `stat_row.ui`

#### Player GUI Pages — Homes, Warps, Kits
- `HomesPage` — browse homes, teleport (with warmup), delete; shows count/limit header
- `WarpsPage` — browse warps grouped by category with category headers, teleport with warmup
- `KitsPage` — browse available kits, claim with cooldown status display, preview items
- `.ui` templates: `homes_page.ui`, `home_entry.ui`, `warps_page.ui`, `warp_entry.ui`, `warp_category_header.ui`, `kits_page.ui`, `kit_entry.ui`
- `/homes`, `/warps`, `/kits` commands now open GUI pages (text fallback preserved)
- Page registration in `HyperEssentials.registerPages()` with correct display order (Homes=10, Warps=20, Kits=30)

#### Admin GUI Pages — Dashboard, Warps, Spawns, Kits
- `AdminDashboardPage` — server overview with online count, warp/spawn/kit stats, module status grid (enabled/disabled indicators)
- `AdminWarpsPage` — list all warps sorted by category, create at current location, delete
- `AdminSpawnsPage` — list all spawns with default badge, create at current location, delete
- `AdminKitsPage` — list all kits with item count/cooldown/one-time info, create from inventory, delete
- `.ui` templates: `admin_dashboard.ui`, `admin_module_card.ui`, `admin_warps.ui`, `admin_warp_entry.ui`, `admin_spawns.ui`, `admin_spawn_entry.ui`, `admin_kits.ui`, `admin_kit_entry.ui`
- Admin page registration in `HyperEssentials.registerPages()`: Dashboard(0), Warps(20), Spawns(30), Kits(40)
- `/he admin` now navigable to all 4 admin tabs

#### Player GUI Pages — Dashboard, TPA, Stats
- `PlayerDashboardPage` — welcome screen with stat cards (homes, online players, TPA requests), quick action buttons, playtime/join info
- `TpaPage` — incoming TPA requests with per-entry accept/deny, toggle TPA acceptance on/off, time remaining display
- `StatsPage` — player stats (first joined, last login, total playtime, current session) and status indicators (AFK, Fly, God, Infinite Stamina)
- `.ui` templates: `dashboard.ui`, `stats.ui`, `tpa_page.ui`, `tpa_entry.ui`
- `UtilityManager.getSessionStart()` — new public accessor for session start time
- All 6 player GUI tabs now functional: Dashboard(0), Homes(10), Warps(20), Kits(30), TPA(40), Stats(50)

#### Admin GUI Pages — Players, Moderation, Announcements, Settings
- `AdminPlayersPage` — list online players sorted by name with UUID preview
- `AdminModerationPage` — punishment list with active/all filter, revoke functionality; type-colored badges (BAN=red, MUTE=gold, KICK=cyan)
- `AdminAnnouncementsPage` — read-only view of announcement messages, interval, and sequential/random mode
- `AdminSettingsPage` — version info, data directory, config reload button, module status grid with enabled/disabled indicators
- `ModerationManager.getAllPunishments(boolean)` — new method to query all punishments across all players (supports active-only filter)
- `.ui` templates: `admin_players.ui`, `admin_player_entry.ui`, `admin_moderation.ui`, `admin_punishment_entry.ui`, `admin_announcements.ui`, `admin_announcement_entry.ui`, `admin_settings.ui`, `admin_module_toggle.ui`
- Admin page registration: Players(10), Moderation(50), Announcements(60), Settings(70)
- Complete GUI system: 14 pages (6 player + 8 admin) all functional

#### New Utility Commands
- `/motd` — display configurable message of the day
- `/rules` — display server rules
- `/discord` — display Discord invite link
- `/list` (aliases: `/online`, `/players`) — show sorted online player list with count
- `/playtime` (alias: `/pt`) — show total playtime including current session
- `/joindate` (alias: `/firstjoin`) — show first join timestamp
- `/afk` (alias: `/away`) — toggle AFK status with server-wide broadcast
- `/stamina` (alias: `/stam`) — toggle infinite stamina with periodic enforcement
- `/maxstack` (alias: `/stack`) — set held item quantity to max stack size
- `/sleeppercentage` (aliases: `/sleeppct`, `/spc`) — view/set sleep skip percentage threshold
- `/invsee <player>` — stub for inventory viewing (GUI coming later)
- `/trash` — stub for disposal inventory (GUI coming later)
- `/repairmax` (alias: `/fixmax`) — fully restore held item including max durability reset
- `/durability` (alias: `/dura`) — set or reset max durability on held items
- `/previewkit` (aliases: `/vkit`, `/viewkit`) — preview kit contents without claiming

#### IP Ban System
- `/ipban <player> [duration] [reason]` — ban player's IP with smart duration detection
- `/ipunban <ip>` — unban an IP address
- `IpBan` record with expiration support (permanent and temporary)
- IP tracking on player connect via `ModerationManager`
- Connect-time IP ban enforcement in `ModerationListener`
- IP ban persistence in `ModerationStorage` (`"ipBans"` JSON section)
- Same-IP kick on ban (disconnects all players on the banned IP)

#### Player Stats System
- `PlayerStats` record (uuid, username, firstJoin, totalPlaytimeMs, lastJoin)
- `PlayerStatsStorage` with JSON persistence at `data/playerstats.json`
- Session tracking in `UtilityManager` with playtime accumulation across sessions
- Connect handler infrastructure in `HyperEssentials` core (mirrors disconnect handler)

#### AFK System
- Manual AFK toggle via `/afk` command with server-wide broadcast
- Auto-AFK detection via configurable idle timeout (`afkTimeoutSeconds`)
- Auto-unset AFK on player activity (chat, interact, mouse motion, movement)
- Activity tracking via `PlayerChatEvent`, `PlayerInteractEvent`, and `PlayerMouseMotionEvent` listeners
- Position-based movement detection via periodic polling (Hytale has no PlayerMoveEvent)

#### Infinite Stamina System
- Toggle-based infinite stamina via `UtilityManager` state tracking
- Periodic enforcement via `ScheduledExecutorService` (every 1 second)
- Uses `EntityStatsModule` component with `DefaultEntityStatTypes.getStamina()` for targeted stat maximization
- Support for targeting other players with `stamina.others` permission

#### Spawn Auto-Detection
- Auto-import spawn points from Hytale `WorldConfig` `ISpawnProvider` on fresh install
- `SpawnManager.importWorldSpawns()` for manual import via `/he importspawns`

#### Expanded Help
- `/he help` subcommand with full command listing grouped by module
- Each module section only shown if the module is enabled

#### Utility Config Expansion
- 12 new command enable toggles (motd, rules, discord, list, playtime, joindate, afk, invsee, stamina, trash, maxstack, sleepPercentage)
- Content fields: `motdLines`, `ruleLines`, `discordUrl`
- AFK config: `afkTimeoutSeconds` (default 300, 0 = disabled)
- Sleep config: `sleepPercentage` with per-world overrides

#### New Permission Nodes
- `utility.motd`, `utility.playtime`, `utility.joindate`, `utility.afk`
- `utility.invsee`, `utility.stamina`, `utility.stamina.others`
- `utility.trash`, `utility.maxstack`, `utility.sleeppercentage`
- `moderation.ipban`

#### Command Aliases
- Homes: `deletehome`/`rmhome`/`removehome`, `listhomes`/`homelist`, `createhome`
- Teleport: `tpy`, `tpc`, `tpn`, `tpt`, `tpr`
- Kits: `ckit`, `dkit`
- Utility: `cc`, `fix`
- Warps: `createwarp`
- Announcements: `bc`
- Moderation: `pun`

### Changed

#### Admin GUI Template Overhaul
- Unified admin and player GUI styling to match HyperFactions quality standards
- All config module classes now have setters for live in-game editing support
- Kit data model expanded with `withDisplayName`, `withCooldownSeconds`, `withOneTime`, `withPermission`, `withItems` copy methods

#### i18n Migration
- All commands, GUI pages, and integration messages migrated from hardcoded strings to i18n keys via `ErrorHandler`
- Homes, warps, spawns, teleport, kits, moderation, utility, announcements, and GUI pages all localized

#### Ban/Mute Consolidation
- `/ban <player> [duration] [reason]` — unified syntax with smart duration detection (replaces `/ban` + `/tempban`)
- `/mute <player> [duration] [reason]` — unified syntax with smart duration detection (replaces `/mute` + `/tempmute`)
- `tempban` alias added to `/ban`, `tempmute`/`tmute` aliases added to `/mute`

#### Kits Overhaul
- Kit items now support 4 inventory sections (hotbar, storage, armor, utility) instead of flat slot numbering
- Inventory space pre-check with `INSUFFICIENT_SPACE` result before claiming
- Displaced armor/utility items automatically moved to hotbar/storage
- Backward-compatible deserialization from old flat-slot format

#### Fly Command
- Replaced Creative/Adventure gamemode hack with proper `MovementManager.canFly` toggle
- Uses `UpdateMovementSettings` packet for client-side flight mode
- Cross-player fly toggle disabled pending entity ref resolution

#### Repair Command
- Added "already full durability" check before repairing
- Uses `withDurability(maxDurability)` instead of `withRestoredDurability()`

#### Warmup/Cooldown System
- Auto-completion via `ScheduledExecutorService` replaces manual polling
- Added `bypass.warmup` and `bypass.cooldown` permission checks
- Cancels existing warmup before starting a new one
- Clean shutdown lifecycle for plugin disable

#### Teleport Commands
- All teleport commands use `executeTeleport` callback pattern with `onComplete` runnable
- Added `ref.isValid()` safety check before teleporting
- Switched to `Teleport.createForPlayer()` for proper API usage
- Store retrieval deferred to world thread for thread safety

#### RTP Cave Avoidance
- Added `rtpSafetyAirAboveHead` config (default 10) to prevent underground RTP placement
- RtpManager scans for solid blocks above landing position

### Removed

#### Ban/Mute Consolidation
- `TempBanCommand.java` — merged into `BanCommand`
- `TempMuteCommand.java` — merged into `MuteCommand`

### Removed
- `AdminSettingsPage` — replaced by in-game Config Editor with full per-module editing

### Fixed
- **Server API 2026.03.26 compatibility** — updated `disconnect()` calls from `disconnect(String)` to `disconnect(Message.raw(String))` in ModerationManager and ModerationListener (API removed String overload)
- Spawn detection now shows all worlds and ensures global spawn always exists
- Admin GUI crash from i18n key mismatch (`hyperessentials_admin` vs `hyperessentials.admin` format)
- Warp and kit creation UX — proper name input dialogs instead of auto-generated names
- Infinite stamina enforcement now dispatches to world thread via `world.execute()` (fixes `PlayerRef.getComponent() called async` error spam)
- AFK status now properly clears on player movement (added position polling and mouse motion listener)
- `/heal` only maximizes health stat instead of all stats (avoids unnecessary regen visual effects)
- `/stamina` maximize uses `DefaultEntityStatTypes.getStamina()` instead of iterating all stats
- Manifest `IncludesAssetPack` set to false (plugin has no bundled assets)

---

### Added (prior releases below)

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
