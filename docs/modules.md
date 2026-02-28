# Module System

## Overview

HyperEssentials uses a modular architecture where each feature area is a self-contained module. Modules can be independently enabled or disabled via their config file.

## Module List

| Module | Config File | Default | Status |
|--------|-------------|---------|--------|
| **warmup** | `config/warmup.json` | Enabled | Implemented |
| **homes** | `config/homes.json` | Enabled | Implemented |
| **warps** | `config/warps.json` | Enabled | Implemented |
| **spawns** | `config/spawns.json` | Enabled | Implemented |
| **teleport** | `config/teleport.json` | Enabled | Implemented |
| **kits** | `config/kits.json` | Disabled | Implemented |
| **moderation** | `config/moderation.json` | Disabled | Implemented |
| **vanish** | `config/vanish.json` | Enabled | Stub |
| **utility** | `config/utility.json` | Disabled | Implemented |
| **announcements** | `config/announcements.json` | Disabled | Implemented |

**Note:** RTP (random teleport) is part of the **teleport** module, not a standalone module. The vanish module is a standalone stub — vanish functionality is fully implemented within the **moderation** module via `VanishManager` and `/vanish`.

## Debug Configuration

A separate `config/debug.json` controls per-category debug logging with 12 categories: homes, warps, spawns, teleport, kits, moderation, utility, rtp, announcements, integration, economy, storage.

## Enabling/Disabling

Each module's JSON config has an `enabled` field:

```json
{
  "enabled": true
}
```

Set to `false` and reload (`/hessentials reload`) to disable a module. Its commands, listeners, and GUI pages will be unregistered.

## Module Lifecycle

1. **Registration** - Module instances are created and registered in `ModuleRegistry` during startup
2. **Enable** - If `enabled = true` in config, `onEnable()` is called (registers commands, listeners, GUI pages)
3. **Manager Init** - Modules with storage needs (warps, spawns, teleport, homes, kits, moderation) have their managers initialized post-enable
4. **Disable** - On shutdown or config change, `onDisable()` is called (saves data, cleanup)

Modules are enabled in registration order (warmup first) and disabled in reverse order.

## Creating a Module

Each module extends `AbstractModule` and implements:

- `getName()` - unique identifier (e.g., `"homes"`)
- `getDisplayName()` - human-readable name (e.g., `"Homes"`)
- `onEnable()` - setup logic
- `onDisable()` - cleanup logic
- `getModuleConfig()` - returns the module's config class
