# Commands

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

## Warps

| Command | Description | Permission |
|---------|-------------|------------|
| `/warp <name>` | Teleport to a warp | `hyperessentials.warp` |
| `/setwarp <name> [category]` | Create or update a warp | `hyperessentials.warp.set` |
| `/delwarp <name>` | Delete a warp | `hyperessentials.warp.delete` |
| `/warps` | List all warps | `hyperessentials.warp.list` |
| `/warpinfo <name>` | View detailed warp info | `hyperessentials.warp.info` |

Aliases: `/delwarp` = `/deletewarp`, `/rmwarp`, `/removewarp`

## Spawns

| Command | Description | Permission |
|---------|-------------|------------|
| `/spawn [name]` | Teleport to spawn | `hyperessentials.spawn` |
| `/setspawn [name] [--default]` | Set a spawn point | `hyperessentials.spawn.set` |
| `/delspawn <name>` | Delete a spawn | `hyperessentials.spawn.delete` |
| `/spawns` | List all spawns | `hyperessentials.spawn.list` |
| `/spawninfo <name>` | View detailed spawn info | `hyperessentials.spawn.info` |

Aliases: `/delspawn` = `/deletespawn`, `/rmspawn`, `/removespawn`

The `--default` flag on `/setspawn` marks the spawn as the server default.

## Teleport

| Command | Description | Permission |
|---------|-------------|------------|
| `/tpa <player>` | Request teleport to player | `hyperessentials.tpa` |
| `/tpahere <player>` | Request player teleport to you | `hyperessentials.tpahere` |
| `/tpaccept [player]` | Accept teleport request | `hyperessentials.tpaccept` |
| `/tpdeny [player]` | Deny teleport request | `hyperessentials.tpdeny` |
| `/tpcancel` | Cancel outgoing request | `hyperessentials.tpcancel` |
| `/tptoggle` | Toggle TPA requests | `hyperessentials.tptoggle` |
| `/back` | Return to previous location | `hyperessentials.back` |

Aliases: `/tpaccept` = `/tpyes`, `/tpdeny` = `/tpno`

## Random Teleport

| Command | Description | Permission |
|---------|-------------|------------|
| `/rtp` | Teleport to a random location | `hyperessentials.rtp` |

Aliases: `/rtp` = `/randomtp`, `/randomteleport`
