# HyperEssentials

Modular server essentials for Hytale. Part of the [HyperSystems](https://github.com/HyperSystemsDev) plugin suite.

## Features

HyperEssentials consolidates essential server functionality into a single modular plugin with 10 modules, 46 commands, and 50+ permission nodes. Each module can be independently enabled or disabled.

| Module | Features | Default | Status |
|--------|----------|---------|--------|
| **Warmup** | Universal warmup/cooldown system for all teleport actions | Enabled | Implemented |
| **Homes** | Home CRUD, sharing, bed sync, faction territory restrictions | Enabled | Implemented |
| **Warps** | Warp CRUD, categories, custom permissions per warp | Enabled | Implemented |
| **Spawns** | Spawn CRUD, per-world spawns, group routing, respawn teleport | Enabled | Implemented |
| **Teleport** | TPA requests, /back history, random teleport (RTP) | Enabled | Implemented |
| **Kits** | Kit creation from inventory, cooldowns, one-time claims, per-kit permissions | Disabled | Implemented |
| **Moderation** | Ban, tempban, mute, tempmute, kick, freeze, vanish, punishment history | Disabled | Implemented |
| **Utility** | Heal, fly, god mode, clear chat, clear inventory, repair, near | Disabled | Implemented |
| **Announcements** | Scheduled broadcasts, announcement rotation, manual broadcast | Disabled | Implemented |
| **Vanish** | Standalone vanish module (vanish is also available via Moderation) | Enabled | Stub |

## Requirements

- Hytale Server (latest release)
- Java 25+

## Optional Dependencies

- [HyperPerms](https://github.com/HyperSystemsDev/HyperPerms) — Advanced permission management (chain-of-responsibility resolution)
- [HyperFactions](https://github.com/HyperSystemsDev/HyperFactions) — Territory-aware home restrictions
- [VaultUnlocked](https://github.com/TheNewEconomy/VaultUnlockedAPI) — Economy integration
- LuckPerms — Permission provider fallback

## Installation

1. Download `HyperEssentials-<version>.jar` from [Releases](https://github.com/HyperSystemsDev/HyperEssentials/releases)
2. Place in your server's `mods/` directory
3. Restart the server
4. Configuration files will be generated in `mods/com.hyperessentials_HyperEssentials/`

## Configuration

HyperEssentials uses a split configuration system with per-module config files:

- `config.json` — Core settings (prefix, colors, admin, storage)
- `config/homes.json` — Home module (limits, bed sync, faction restrictions)
- `config/warps.json` — Warp module (default category)
- `config/spawns.json` — Spawn module (default spawn, join/respawn teleport)
- `config/teleport.json` — TPA and RTP settings (timeouts, back history, RTP radius)
- `config/warmup.json` — Per-module warmup/cooldown timers
- `config/kits.json` — Kit module (default cooldown, one-time claims)
- `config/moderation.json` — Moderation (default reasons, broadcast toggles, freeze interval)
- `config/vanish.json` — Vanish (fake join/leave messages)
- `config/utility.json` — Utility commands (per-command toggles, radius limits)
- `config/announcements.json` — Announcement rotation (interval, randomize, messages)
- `config/debug.json` — Debug logging (per-category toggles)

## Commands

See [docs/commands.md](docs/commands.md) for the complete command reference with all 46 commands, permissions, and aliases.

### Quick Reference

| Module | Commands |
|--------|----------|
| Admin | `/hessentials reload\|version` |
| Homes | `/sethome`, `/home`, `/delhome`, `/homes` |
| Warps | `/warp`, `/setwarp`, `/delwarp`, `/warps`, `/warpinfo` |
| Spawns | `/spawn`, `/setspawn`, `/delspawn`, `/spawns`, `/spawninfo` |
| Teleport | `/tpa`, `/tpahere`, `/tpaccept`, `/tpdeny`, `/tpcancel`, `/tptoggle`, `/back`, `/rtp` |
| Kits | `/kit`, `/kits`, `/createkit`, `/deletekit` |
| Moderation | `/ban`, `/tempban`, `/unban`, `/mute`, `/tempmute`, `/unmute`, `/kick`, `/freeze`, `/vanish`, `/punishments` |
| Utility | `/heal`, `/fly`, `/god`, `/clearchat`, `/clearinventory`, `/repair`, `/near` |
| Announcements | `/broadcast`, `/announce` |

## Permissions

See [docs/permissions.md](docs/permissions.md) for the complete permission reference. All nodes use the `hyperessentials` prefix.

## Documentation

Detailed documentation is available in the [docs/](docs/) directory:

| Document | Description |
|----------|-------------|
| [Architecture](docs/architecture.md) | Package structure, module lifecycle, cross-module communication |
| [Commands](docs/commands.md) | All 46 commands organized by module |
| [Config](docs/config.md) | Configuration reference for core and all modules |
| [Permissions](docs/permissions.md) | Permission nodes organized by module |
| [Modules](docs/modules.md) | Module system overview, enable/disable, lifecycle |
| [Storage](docs/storage.md) | Storage providers, JSON format, data directory layout |
| [Integrations](docs/integrations.md) | HyperPerms, HyperFactions, VaultUnlocked integration |

## Building

```bash
./gradlew :HyperEssentials:shadowJar
```

Output: `HyperEssentials/build/libs/HyperEssentials-<version>.jar`

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
