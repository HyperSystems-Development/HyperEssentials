# HyperEssentials

Modular server essentials for Hytale. Part of the [HyperSystems](https://github.com/HyperSystems-Development) plugin suite.

## Features

HyperEssentials consolidates essential server functionality into a single modular plugin:

| Module | Features | Status |
|--------|----------|--------|
| **Homes** | Home CRUD, sharing, bed sync | Planned |
| **Warps** | Warp CRUD, categories | Planned |
| **Spawns** | Spawn CRUD, per-world, respawn | Planned |
| **Teleport** | TPA, /back | Planned |
| **Warmup** | Universal warmup/cooldown system | Scaffold |
| **Kits** | Kit system | Planned |
| **Moderation** | Mute, temp ban, IP ban, freeze | Planned |
| **Vanish** | Vanish system | Planned |
| **Utility** | Clear chat, clear inventory, repair, near | Planned |
| **Announcements** | Broadcast/announcement system | Planned |
| **RTP** | Random teleport | Planned |

Each module can be independently enabled or disabled via configuration.

## Requirements

- Hytale Server (latest release)
- Java 25+

## Optional Dependencies

- [HyperPerms](https://github.com/HyperSystems-Development/HyperPerms) - Advanced permission management

## Installation

1. Download `HyperEssentials-<version>.jar` from [Releases](https://github.com/HyperSystems-Development/HyperEssentials/releases)
2. Place in your server's `mods/` directory
3. Restart the server
4. Configuration files will be generated in `mods/com.hyperessentials_HyperEssentials/`

## Configuration

HyperEssentials uses a split configuration system:

- `config.json` - Core settings (prefix, colors, admin)
- `config/homes.json` - Home module settings
- `config/warps.json` - Warp module settings
- `config/spawns.json` - Spawn module settings
- `config/teleport.json` - TPA module settings
- `config/warmup.json` - Warmup/cooldown settings
- `config/kits.json` - Kit module settings
- `config/moderation.json` - Moderation settings
- `config/vanish.json` - Vanish settings
- `config/utility.json` - Utility settings
- `config/announcements.json` - Announcement settings
- `config/rtp.json` - Random teleport settings

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/hessentials reload` | Reload configuration | `hyperessentials.admin.reload` |
| `/hessentials version` | Show version | - |

## Building

```bash
./gradlew :HyperEssentials:shadowJar
```

Output: `HyperEssentials/build/libs/HyperEssentials-<version>.jar`

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.
