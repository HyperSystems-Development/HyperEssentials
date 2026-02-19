# Module System

> **Status:** Framework complete, all modules are stubs.

## Overview

HyperEssentials uses a modular architecture where each feature area is a self-contained module. Modules can be independently enabled or disabled via their config file.

## Module List

| Module | Config File | Default | Description |
|--------|-------------|---------|-------------|
| **warmup** | `warmup.json` | Enabled | Universal warmup/cooldown system |
| **homes** | `homes.json` | Enabled | Home management and teleportation |
| **warps** | `warps.json` | Enabled | Server warp points |
| **spawns** | `spawns.json` | Enabled | Spawn point management |
| **teleport** | `teleport.json` | Enabled | TPA requests and /back |
| **kits** | `kits.json` | Disabled | Kit system |
| **moderation** | `moderation.json` | Disabled | Mute, ban, freeze |
| **vanish** | `vanish.json` | Disabled | Vanish system |
| **utility** | `utility.json` | Disabled | Clear chat, repair, near |
| **announcements** | `announcements.json` | Disabled | Broadcast system |
| **rtp** | `rtp.json` | Disabled | Random teleport |

## Enabling/Disabling

Each module's JSON config has an `enabled` field:

```json
{
  "enabled": true
}
```

Set to `false` and reload (`/hessentials reload`) to disable a module. Its commands, listeners, and GUI pages will be unregistered.

## Module Lifecycle

1. **Registration** — Module instances are created and registered in `ModuleRegistry` during startup
2. **Enable** — If `enabled = true` in config, `onEnable()` is called (registers commands, listeners, GUI pages)
3. **Disable** — On shutdown or config change, `onDisable()` is called (cleanup)

Modules are enabled in registration order (warmup first) and disabled in reverse order.

## Creating a Module

Each module extends `AbstractModule` and implements:

- `getName()` — unique identifier (e.g., `"homes"`)
- `getDisplayName()` — human-readable name (e.g., `"Homes"`)
- `onEnable()` — setup logic
- `onDisable()` — cleanup logic
- `getModuleConfig()` — returns the module's config class
