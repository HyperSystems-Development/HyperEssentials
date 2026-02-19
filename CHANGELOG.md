# Changelog

All notable changes to HyperEssentials will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
