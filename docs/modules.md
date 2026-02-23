# Module System

## Overview

HyperEssentials uses a modular architecture where each feature area is a self-contained module. Modules can be independently enabled or disabled via their config file.

## Module List

| Module | Config File | Default | Status |
|--------|-------------|---------|--------|
| **warmup** | `warmup.json` | Enabled | Implemented |
| **homes** | `homes.json` | Enabled | Stub (TODO) |
| **warps** | `warps.json` | Enabled | Implemented |
| **spawns** | `spawns.json` | Enabled | Implemented |
| **teleport** | `teleport.json` | Enabled | Implemented |
| **kits** | `kits.json` | Disabled | Stub (TODO) |
| **moderation** | `moderation.json` | Disabled | Stub (TODO) |
| **vanish** | `vanish.json` | Disabled | Stub (TODO) |
| **utility** | `utility.json` | Disabled | Stub (TODO) |
| **announcements** | `announcements.json` | Disabled | Stub (TODO) |
| **rtp** | `rtp.json` | Disabled | Implemented |

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
3. **Manager Init** - Modules with storage needs (warps, spawns, teleport) have their managers initialized post-enable
4. **Disable** - On shutdown or config change, `onDisable()` is called (saves data, cleanup)

Modules are enabled in registration order (warmup first) and disabled in reverse order.

## Creating a Module

Each module extends `AbstractModule` and implements:

- `getName()` - unique identifier (e.g., `"homes"`)
- `getDisplayName()` - human-readable name (e.g., `"Homes"`)
- `onEnable()` - setup logic
- `onDisable()` - cleanup logic
- `getModuleConfig()` - returns the module's config class
