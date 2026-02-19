# Commands

> **Status:** Only the admin command is implemented. Module commands will be added as modules are built.

## Admin

| Command | Description | Permission |
|---------|-------------|------------|
| `/hessentials` | Show help | - |
| `/hessentials reload` | Reload configuration | `hyperessentials.admin.reload` |
| `/hessentials version` | Show version | - |

## Homes (Planned)

| Command | Description | Permission |
|---------|-------------|------------|
| `/home [name]` | Teleport to a home | `hyperessentials.home` |
| `/sethome [name]` | Set a home | `hyperessentials.home.set` |
| `/delhome <name>` | Delete a home | `hyperessentials.home.delete` |
| `/homes` | List homes / open GUI | `hyperessentials.home.list` |

## Warps (Planned)

| Command | Description | Permission |
|---------|-------------|------------|
| `/warp <name>` | Teleport to a warp | `hyperessentials.warp` |
| `/setwarp <name>` | Create a warp | `hyperessentials.warp.set` |
| `/delwarp <name>` | Delete a warp | `hyperessentials.warp.delete` |
| `/warps` | List warps / open GUI | `hyperessentials.warp.list` |

## Spawns (Planned)

| Command | Description | Permission |
|---------|-------------|------------|
| `/spawn [name]` | Teleport to spawn | `hyperessentials.spawn` |
| `/setspawn [name]` | Set a spawn point | `hyperessentials.spawn.set` |
| `/delspawn <name>` | Delete a spawn | `hyperessentials.spawn.delete` |
| `/spawns` | List spawns | `hyperessentials.spawn.list` |

## Teleport (Planned)

| Command | Description | Permission |
|---------|-------------|------------|
| `/tpa <player>` | Request teleport to player | `hyperessentials.tpa` |
| `/tpahere <player>` | Request player teleport to you | `hyperessentials.tpahere` |
| `/tpaccept` | Accept teleport request | `hyperessentials.tpaccept` |
| `/tpdeny` | Deny teleport request | `hyperessentials.tpdeny` |
| `/tpcancel` | Cancel outgoing request | `hyperessentials.tpcancel` |
| `/tptoggle` | Toggle TPA requests | `hyperessentials.tptoggle` |
| `/back` | Return to previous location | `hyperessentials.back` |
