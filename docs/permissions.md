# Permissions

All permission nodes use the `hyperessentials` root prefix. Wildcard resolution is supported (e.g., `hyperessentials.*` grants all nodes).

## Homes

| Permission | Description |
|------------|-------------|
| `hyperessentials.home.teleport` | Use /home to teleport |
| `hyperessentials.home.set` | Set homes |
| `hyperessentials.home.delete` | Delete homes |
| `hyperessentials.home.list` | List homes |
| `hyperessentials.home.gui` | Open homes GUI |
| `hyperessentials.home.share` | Share homes with other players |
| `hyperessentials.home.unlimited` | Bypass home count limit |
| `hyperessentials.home.limit.<N>` | Set per-player home limit (e.g., `.limit.10`) |

Home limit priority: `home.unlimited` > `bypass.limit` > `home.limit.<N>` > config default (3).

## Warps

| Permission | Description |
|------------|-------------|
| `hyperessentials.warp` | Use /warp |
| `hyperessentials.warp.set` | Create warps |
| `hyperessentials.warp.delete` | Delete warps |
| `hyperessentials.warp.list` | List warps |
| `hyperessentials.warp.info` | View warp info |

Individual warps can also have custom permission nodes set via `/setwarp`.

## Spawns

| Permission | Description |
|------------|-------------|
| `hyperessentials.spawn` | Use /spawn |
| `hyperessentials.spawn.set` | Set spawns |
| `hyperessentials.spawn.delete` | Delete spawns |
| `hyperessentials.spawn.list` | List spawns |
| `hyperessentials.spawn.info` | View spawn info |

## Teleport

| Permission | Description |
|------------|-------------|
| `hyperessentials.tpa` | Send TPA requests |
| `hyperessentials.tpahere` | Send TPA-here requests |
| `hyperessentials.tpaccept` | Accept TPA requests |
| `hyperessentials.tpdeny` | Deny TPA requests |
| `hyperessentials.tpcancel` | Cancel TPA requests |
| `hyperessentials.tptoggle` | Toggle TPA on/off |
| `hyperessentials.back` | Use /back |
| `hyperessentials.rtp` | Use /rtp (random teleport) |
| `hyperessentials.rtp.bypass.factions` | Skip faction claim avoidance during RTP |

## Kits

| Permission | Description |
|------------|-------------|
| `hyperessentials.kit.*` | All kit permissions |
| `hyperessentials.kit.use` | Use kits (base) |
| `hyperessentials.kit.use.<name>` | Use a specific kit (e.g., `.kit.use.starter`) |
| `hyperessentials.kit.list` | List available kits |
| `hyperessentials.kit.create` | Create kit definitions |
| `hyperessentials.kit.delete` | Delete kit definitions |

## Moderation

| Permission | Description |
|------------|-------------|
| `hyperessentials.moderation.*` | All moderation permissions |
| `hyperessentials.moderation.ban` | Ban, tempban, and unban players |
| `hyperessentials.moderation.mute` | Mute, tempmute, and unmute players |
| `hyperessentials.moderation.kick` | Kick players |
| `hyperessentials.moderation.freeze` | Freeze/unfreeze players |
| `hyperessentials.moderation.vanish` | Toggle vanish |
| `hyperessentials.moderation.history` | View punishment history |

## Utility

| Permission | Description |
|------------|-------------|
| `hyperessentials.utility.*` | All utility permissions |
| `hyperessentials.utility.heal` | Heal self |
| `hyperessentials.utility.heal.others` | Heal other players |
| `hyperessentials.utility.fly` | Toggle flight for self |
| `hyperessentials.utility.fly.others` | Toggle flight for others |
| `hyperessentials.utility.god` | Toggle god mode for self |
| `hyperessentials.utility.god.others` | Toggle god mode for others |
| `hyperessentials.utility.clearchat` | Clear own chat |
| `hyperessentials.utility.clearchat.others` | Clear chat for all players |
| `hyperessentials.utility.clearinventory` | Clear own inventory |
| `hyperessentials.utility.clearinventory.others` | Clear other players' inventory |
| `hyperessentials.utility.repair` | Repair held item |
| `hyperessentials.utility.near` | List nearby players |

## Announcements

| Permission | Description |
|------------|-------------|
| `hyperessentials.announce.*` | All announcement permissions |
| `hyperessentials.announce.broadcast` | Send manual broadcasts |
| `hyperessentials.announce.manage` | Manage announcement rotation |

## Bypass

| Permission | Description |
|------------|-------------|
| `hyperessentials.bypass.*` | All bypass permissions |
| `hyperessentials.bypass.warmup` | Skip warmup timers |
| `hyperessentials.bypass.cooldown` | Skip cooldowns |
| `hyperessentials.bypass.limit` | Bypass home limits |
| `hyperessentials.bypass.toggle` | Bypass TPA toggle (send requests to players with TPA disabled) |
| `hyperessentials.bypass.ban` | Bypass ban enforcement |
| `hyperessentials.bypass.mute` | Bypass mute enforcement |
| `hyperessentials.bypass.freeze` | Bypass freeze |
| `hyperessentials.bypass.kit.cooldown` | Bypass kit cooldowns |
| `hyperessentials.bypass.factions` | Bypass all faction territory restrictions |
| `hyperessentials.bypass.factions.sethome` | Bypass faction restrictions for /sethome |
| `hyperessentials.bypass.factions.home` | Bypass faction restrictions for /home |

## Notify (Staff Alerts)

| Permission | Description |
|------------|-------------|
| `hyperessentials.notify.*` | Receive all staff notifications |
| `hyperessentials.notify.ban` | Receive ban notifications |
| `hyperessentials.notify.mute` | Receive mute notifications |
| `hyperessentials.notify.kick` | Receive kick notifications |

## Admin

| Permission | Description |
|------------|-------------|
| `hyperessentials.admin` | Admin root |
| `hyperessentials.admin.*` | All admin permissions |
| `hyperessentials.admin.reload` | Reload configuration |
| `hyperessentials.admin.settings` | Modify settings |
