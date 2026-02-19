# Warmup / Cooldown System

> **Status:** Framework complete, not yet wired to modules.

## Overview

HyperEssentials provides a universal warmup/cooldown system that all modules share. This avoids duplicate timer logic across homes, warps, spawns, and TPA.

## How It Works

### Warmup
When a player initiates a teleport action:
1. Module calls `warmupManager.startWarmup(uuid, "homes", "teleport", callback)`
2. `WarmupManager` looks up the warmup duration from `WarmupConfig`'s per-module settings
3. A countdown starts — player sees a warmup message
4. If the player moves or takes damage (and cancellation is enabled), the warmup is cancelled
5. On completion, the callback executes the actual teleport

### Cooldown
After a successful action:
1. Module calls `cooldownTracker.startCooldown(uuid, "homes", "teleport")`
2. Before the next action, module checks `cooldownTracker.isOnCooldown(uuid, "homes", "teleport")`
3. If on cooldown, the action is blocked with a remaining-time message

## Configuration (`warmup.json`)

```json
{
  "enabled": true,
  "cancelOnMove": true,
  "cancelOnDamage": true,
  "safeTeleport": true,
  "safeRadius": 3,
  "modules": {
    "homes": { "warmup": 3, "cooldown": 5 },
    "warps": { "warmup": 3, "cooldown": 5 },
    "spawns": { "warmup": 3, "cooldown": 5 },
    "teleport": { "warmup": 5, "cooldown": 10 },
    "rtp": { "warmup": 5, "cooldown": 30 }
  }
}
```

## Bypass Permissions

| Permission | Effect |
|------------|--------|
| `hyperessentials.bypass.warmup` | Skip warmup timers |
| `hyperessentials.bypass.cooldown` | Skip cooldowns |
