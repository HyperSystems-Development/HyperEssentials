# Commands

HyperEssentials provides 46 commands across 9 modules. All commands require their respective module to be enabled.

## Admin

| Command | Description | Permission |
|---------|-------------|------------|
| `/hessentials` | Show help | - |
| `/hessentials reload` | Reload configuration | `hyperessentials.admin.reload` |
| `/hessentials version` | Show version | - |

## Homes

| Command | Description | Permission |
|---------|-------------|------------|
| `/sethome [name]` | Set a home at current location | `hyperessentials.home.set` |
| `/home [name]` | Teleport to a home | `hyperessentials.home.teleport` |
| `/delhome <name>` | Delete a home | `hyperessentials.home.delete` |
| `/homes` | List all homes with count/limit | `hyperessentials.home.list` |

Home names are validated against `[a-zA-Z0-9_-]{1,32}`. Faction territory restrictions apply if HyperFactions is installed and configured.

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
| `/rtp` | Teleport to a random location | `hyperessentials.rtp` |

Aliases: `/tpaccept` = `/tpyes`, `/tpdeny` = `/tpno`, `/rtp` = `/randomtp`, `/randomteleport`

RTP generates a random location within a configurable ring radius from a center point.

## Kits

| Command | Description | Permission |
|---------|-------------|------------|
| `/kit <name>` | Claim a kit | `hyperessentials.kit.use.<name>` |
| `/kits` | List available kits | `hyperessentials.kit.list` |
| `/createkit <name>` | Create a kit from current inventory | `hyperessentials.kit.create` |
| `/deletekit <name>` | Delete a kit definition | `hyperessentials.kit.delete` |

Kits support per-kit cooldowns, one-time claims, and custom permission overrides.

## Moderation

| Command | Description | Permission |
|---------|-------------|------------|
| `/ban <player> [reason]` | Permanently ban a player | `hyperessentials.moderation.ban` |
| `/tempban <player> <duration> [reason]` | Temporarily ban a player | `hyperessentials.moderation.ban` |
| `/unban <player>` | Unban a player | `hyperessentials.moderation.ban` |
| `/mute <player> [reason]` | Permanently mute a player | `hyperessentials.moderation.mute` |
| `/tempmute <player> <duration> [reason]` | Temporarily mute a player | `hyperessentials.moderation.mute` |
| `/unmute <player>` | Unmute a player | `hyperessentials.moderation.mute` |
| `/kick <player> [reason]` | Kick a player | `hyperessentials.moderation.kick` |
| `/freeze <player>` | Toggle position freeze | `hyperessentials.moderation.freeze` |
| `/vanish` | Toggle vanish (fake leave/join) | `hyperessentials.moderation.vanish` |
| `/punishments <player>` | View punishment history | `hyperessentials.moderation.history` |

Duration format: `1h`, `30m`, `7d`, `1h30m` (parsed by `DurationParser`).

## Utility

| Command | Description | Permission |
|---------|-------------|------------|
| `/heal [player]` | Heal self or another player | `hyperessentials.utility.heal` / `.heal.others` |
| `/fly [player]` | Toggle flight for self or another | `hyperessentials.utility.fly` / `.fly.others` |
| `/god [player]` | Toggle god mode (invulnerability) | `hyperessentials.utility.god` / `.god.others` |
| `/clearchat` | Clear chat history | `hyperessentials.utility.clearchat` / `.clearchat.others` |
| `/clearinventory [player]` | Clear inventory | `hyperessentials.utility.clearinventory` / `.clearinventory.others` |
| `/repair` | Repair held item | `hyperessentials.utility.repair` |
| `/near [radius]` | List nearby players | `hyperessentials.utility.near` |

Aliases: `/clearinventory` = `/ci`

Each utility command can be individually enabled/disabled in `config/utility.json`.

## Announcements

| Command | Description | Permission |
|---------|-------------|------------|
| `/broadcast <message>` | Send a formatted broadcast | `hyperessentials.announce.broadcast` |
| `/announce list\|add\|remove\|reload` | Manage announcement rotation | `hyperessentials.announce.manage` |

Announcements can also run automatically on a configurable interval with sequential or random rotation.
