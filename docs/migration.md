# Migration Guide

> **Status:** Planned — migration tooling will be built alongside module implementations.

## Overview

HyperEssentials replaces both **HyperHomes** and **HyperWarp**. This guide will cover migrating data and configuration from those plugins.

## From HyperHomes

### Data Migration
- Home data (JSON format) will be importable via a migration command
- Player homes, sharing data, and bed sync state will be preserved

### Config Migration
- HyperHomes config settings map to `homes.json` and `warmup.json`
- Teleport warmup/cooldown settings move to the universal warmup system

### Permission Migration
| HyperHomes | HyperEssentials |
|------------|-----------------|
| `hyperhomes.home` | `hyperessentials.home` |
| `hyperhomes.home.set` | `hyperessentials.home.set` |
| `hyperhomes.home.delete` | `hyperessentials.home.delete` |
| `hyperhomes.home.list` | `hyperessentials.home.list` |
| `hyperhomes.home.unlimited` | `hyperessentials.home.unlimited` |
| `hyperhomes.bypass.*` | `hyperessentials.bypass.*` |

## From HyperWarp

### Data Migration
- Warp, spawn, and player data (back history, TPA toggle) will be importable
- All location data will be preserved

### Config Migration
- HyperWarp warp settings map to `warps.json`
- HyperWarp spawn settings map to `spawns.json`
- HyperWarp TPA settings map to `teleport.json`
- Warmup/cooldown settings move to the universal warmup system

### Permission Migration
| HyperWarp | HyperEssentials |
|-----------|-----------------|
| `hyperwarp.warp` | `hyperessentials.warp` |
| `hyperwarp.spawn` | `hyperessentials.spawn` |
| `hyperwarp.tpa` | `hyperessentials.tpa` |
| `hyperwarp.back` | `hyperessentials.back` |

## Running Migration

Migration commands will be available in the admin command:
```
/hessentials migrate homes    — Import HyperHomes data
/hessentials migrate warps    — Import HyperWarp data
```

Both plugins can run alongside HyperEssentials during the transition period.
