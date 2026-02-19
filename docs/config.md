# Configuration

> **Status:** Scaffold — config classes exist and will generate default files on first run.

## File Structure

```
mods/com.hyperessentials_HyperEssentials/
  config.json              Core settings
  config/
    homes.json             Home module
    warps.json             Warp module
    spawns.json            Spawn module
    teleport.json          TPA module
    warmup.json            Warmup/cooldown
    kits.json              Kit module
    moderation.json        Moderation module
    vanish.json            Vanish module
    utility.json           Utility module
    announcements.json     Announcement module
    rtp.json               Random teleport module
```

## Core Config (`config.json`)

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `prefixText` | string | `"HyperEssentials"` | Chat prefix text |
| `prefixColor` | string | `"#FFAA00"` | Prefix text color (hex) |
| `prefixBracketColor` | string | `"#AAAAAA"` | Bracket color (hex) |
| `primaryColor` | string | `"#55FFFF"` | Primary accent color |
| `secondaryColor` | string | `"#FFAA00"` | Secondary accent color |
| `errorColor` | string | `"#FF5555"` | Error message color |
| `adminRequiresOp` | boolean | `true` | Require OP for admin commands |
| `allowWithoutPermissionMod` | boolean | `true` | Allow commands without HyperPerms |
| `storageType` | string | `"json"` | Storage backend |
| `updateCheckEnabled` | boolean | `true` | Check for updates |

## Module Configs

Each module config has an `enabled` field. Set to `false` to disable the module entirely.

See individual module documentation for config details (to be added as modules are implemented).
