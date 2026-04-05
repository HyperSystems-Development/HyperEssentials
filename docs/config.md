# Configuration

All config files use JSON and are generated with defaults on first run.

## File Structure

```
mods/com.hyperessentials_HyperEssentials/
  config.json              Core settings
  config/
    homes.json             Home module
    warps.json             Warp module
    spawns.json            Spawn module
    teleport.json          TPA and RTP settings
    warmup.json            Warmup/cooldown
    kits.json              Kit module
    moderation.json        Moderation module
    vanish.json            Vanish module
    utility.json           Utility module
    announcements.json     Announcement module
    debug.json             Debug logging categories
```

## Core Config (`config.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `prefixText` | string | `"HyperEssentials"` | Chat prefix text |
| `prefixColor` | string | `"#FFAA00"` | Prefix text color (hex) |
| `prefixBracketColor` | string | `"#AAAAAA"` | Bracket color (hex) |
| `primaryColor` | string | `"#55FFFF"` | Primary accent color |
| `secondaryColor` | string | `"#55FF55"` | Secondary accent color |
| `errorColor` | string | `"#FF5555"` | Error message color |
| `adminRequiresOp` | boolean | `true` | Require OP for admin commands |
| `allowWithoutPermissionMod` | boolean | `true` | Allow commands without a permission plugin |
| `storageType` | string | `"json"` | Storage backend (only `"json"` supported) |
| `updateCheck` | boolean | `true` | Check for updates on startup |
| `configVersion` | int | `1` | Config version for migrations |

## Module Configs

Each module config has an `enabled` field. Set to `false` to disable the module entirely.

### Homes (`config/homes.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `defaultHomeLimit` | int | `3` | Default home limit per player |
| `bedSyncEnabled` | boolean | `true` | Sync bed interaction with a "bed" home |
| `bedHomeName` | string | `"bed"` | Name for the bed-synced home |
| `shareEnabled` | boolean | `true` | Allow players to share homes |
| `maxSharesPerHome` | int | `10` | Maximum shares per home |
| `factions.enabled` | boolean | `true` | Enable faction territory restrictions |
| `factions.allowInOwnTerritory` | boolean | `true` | Allow homes in own faction territory |
| `factions.allowInAllyTerritory` | boolean | `true` | Allow homes in ally territory |
| `factions.allowInNeutralTerritory` | boolean | `true` | Allow homes in neutral territory |
| `factions.allowInEnemyTerritory` | boolean | `false` | Allow homes in enemy territory |
| `factions.allowInWilderness` | boolean | `true` | Allow homes in wilderness |

### Warps (`config/warps.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `defaultCategory` | string | `"general"` | Default category for new warps |

### Spawns (`config/spawns.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `defaultSpawnName` | string | `"spawn"` | Name of the default spawn |
| `teleportOnJoin` | boolean | `false` | Teleport players to spawn on join |
| `teleportOnRespawn` | boolean | `true` | Teleport players to spawn on respawn |
| `perWorldSpawns` | boolean | `false` | Use per-world spawn points |

### Teleport (`config/teleport.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `tpaTimeout` | int | `60` | TPA request expiry in seconds |
| `tpaCooldown` | int | `30` | Cooldown between TPA requests in seconds |
| `maxPendingTpa` | int | `5` | Max pending incoming TPA requests per player |
| `backHistorySize` | int | `5` | Number of back locations to remember |
| `saveBackOnDeath` | boolean | `true` | Save location on death for /back |
| `saveBackOnTeleport` | boolean | `true` | Save location on teleport for /back |
| `rtp.centerX` | int | `0` | RTP center X coordinate (used when `playerRelative` is false) |
| `rtp.centerZ` | int | `0` | RTP center Z coordinate (used when `playerRelative` is false) |
| `rtp.minRadius` | int | `100` | Minimum RTP radius |
| `rtp.maxRadius` | int | `5000` | Maximum RTP radius |
| `rtp.maxAttempts` | int | `10` | Max safe-location search attempts |
| `rtp.playerRelative` | boolean | `true` | Center search ring on player (true) or `centerX/Z` (false) |
| `rtp.blacklistedWorlds` | list | `[]` | Worlds where RTP is disabled |
| `rtp.factionAvoidance.enabled` | boolean | `true` | Skip locations near faction claims |
| `rtp.factionAvoidance.bufferRadius` | int | `2` | Chunk buffer radius around claims (2 = 5x5 check area) |
| `rtp.safety.avoidWater` | boolean | `true` | Skip locations with any fluid at feet/head |
| `rtp.safety.avoidDangerousFluids` | boolean | `true` | Skip locations near lava/tar/poison below ground |
| `rtp.safety.minY` | int | `5` | Minimum Y level (avoid bedrock) |
| `rtp.safety.maxY` | int | `300` | Maximum Y level for heightmap scan |

### Warmup (`config/warmup.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `cancelOnMove` | boolean | `true` | Cancel warmup on player movement |
| `cancelOnDamage` | boolean | `true` | Cancel warmup on damage |
| `safeTeleport` | boolean | `true` | Find safe location on teleport |
| `safeRadius` | int | `3` | Safe teleport search radius |
| `modules.homes` | object | `{warmup: 3, cooldown: 5}` | Homes warmup/cooldown (seconds) |
| `modules.warps` | object | `{warmup: 3, cooldown: 5}` | Warps warmup/cooldown |
| `modules.spawns` | object | `{warmup: 3, cooldown: 5}` | Spawns warmup/cooldown |
| `modules.teleport` | object | `{warmup: 3, cooldown: 5}` | TPA warmup/cooldown |
| `modules.rtp` | object | `{warmup: 5, cooldown: 30}` | RTP warmup/cooldown |

### Kits (`config/kits.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `defaultCooldownSeconds` | int | `300` | Default kit cooldown (5 minutes) |
| `oneTimeDefault` | boolean | `false` | Default one-time claim setting for new kits |

### Moderation (`config/moderation.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `defaultBanReason` | string | (default) | Default ban reason text |
| `defaultMuteReason` | string | (default) | Default mute reason text |
| `defaultKickReason` | string | (default) | Default kick reason text |
| `mutedChatMessage` | string | (default) | Message shown to muted players |
| `freezeMessage` | string | (default) | Message shown to frozen players |
| `freezeCheckIntervalMs` | int | `100` | Freeze position-check interval (ms) |
| `broadcastBans` | boolean | `true` | Broadcast ban messages server-wide |
| `broadcastKicks` | boolean | `true` | Broadcast kick messages server-wide |
| `broadcastMutes` | boolean | `false` | Broadcast mute messages server-wide |
| `maxHistoryPerPlayer` | int | `50` | Maximum punishment records per player |

### Vanish (`config/vanish.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `fakeLeaveMessage` | boolean | `true` | Send fake leave message on vanish |
| `fakeJoinMessage` | boolean | `true` | Send fake join message on unvanish |
| `vanishEnableMessage` | string | (default) | Message shown when vanishing |
| `vanishDisableMessage` | string | (default) | Message shown when unvanishing |
| `silentJoin` | boolean | `false` | Auto-vanish on join |

### Utility (`config/utility.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `healEnabled` | boolean | `true` | Enable /heal |
| `flyEnabled` | boolean | `true` | Enable /fly |
| `godEnabled` | boolean | `true` | Enable /god |
| `clearChatEnabled` | boolean | `true` | Enable /clearchat |
| `clearInventoryEnabled` | boolean | `true` | Enable /clearinventory |
| `repairEnabled` | boolean | `true` | Enable /repair |
| `nearEnabled` | boolean | `true` | Enable /near |
| `defaultNearRadius` | int | `200` | Default /near radius |
| `maxNearRadius` | int | `1000` | Maximum /near radius |
| `clearChatLines` | int | `100` | Number of blank lines for /clearchat |

### Announcements (`config/announcements.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `intervalSeconds` | int | `300` | Auto-broadcast interval (5 minutes) |
| `randomize` | boolean | `false` | Randomize message order |
| `prefixText` | string | `"Announcement"` | Announcement prefix text |
| `prefixColor` | string | `"#FFAA00"` | Announcement prefix color |
| `messageColor` | string | `"#FFFFFF"` | Announcement message color |
| `messages` | list | `[]` | Announcement message rotation list |

### Debug (`config/debug.json`)

Per-category debug logging toggles. Categories: `homes`, `warps`, `spawns`, `teleport`, `kits`, `moderation`, `utility`, `rtp`, `announcements`, `integration`, `economy`, `storage`.
