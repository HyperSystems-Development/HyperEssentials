# Integrations

HyperEssentials integrates with several plugins via reflection-based soft dependencies. All integrations fail gracefully if the target plugin is not installed.

## HyperPerms (Soft Dependency)

HyperEssentials uses HyperPerms for permission checks when available. Detection is reflection-based via `HyperPermsProviderAdapter`.

**Resolution chain (PermissionManager):**
1. Try HyperPerms via `HyperPermsProviderAdapter`
2. Fall back to OP check (via reflection)
3. For bypass permissions, default to `false`
4. For user permissions, respect `allowWithoutPermissionMod` config flag

**Features used:**
- `hasPermission(uuid, node)` — standard permission checks
- `getPermissionValue(uuid, prefix, default)` — numeric limits (e.g., home count via `home.limit.N`)
- `getPrimaryGroup(uuid)` — group lookup
- Wildcard resolution: exact node → category wildcard (`hyperessentials.home.*`) → root wildcard (`hyperessentials.*`)

## HyperFactions (Soft Dependency)

Reflection-based integration via `HyperFactionsIntegration` for territory awareness:
- `getFactionAtLocation(world, x, z)` — faction name at coordinates
- `getRelationAtLocation(playerUuid, world, x, z)` — relation type (OWN, ALLY, NEUTRAL, ENEMY)
- `getTerritoryLabel(world, x, z)` — territory display label

**FactionTerritoryChecker** uses this integration for home placement/teleport restrictions:
- `canUseHome(uuid, world, x, z)` — returns `Result.ALLOWED` or a denial with territory type
- `Result` enum: `ALLOWED`, `BLOCKED_OWN`, `BLOCKED_ALLY`, `BLOCKED_ENEMY`, `BLOCKED_NEUTRAL`, `BLOCKED_WILDERNESS`
- Territory restrictions are configurable per-relationship in `config/homes.json`
- Bypass permissions: `bypass.factions`, `bypass.factions.sethome`, `bypass.factions.home`

## VaultUnlocked (Soft Dependency)

Economy integration via `VaultEconomyProvider` using reflection on VaultUnlocked 2:
- `getBalance(uuid)` — get player balance
- `has(uuid, amount)` — check if player has sufficient funds
- `withdraw(uuid, amount)` — withdraw from player account
- `deposit(uuid, amount)` — deposit to player account
- Uses plugin name `"HyperEssentials"` as the account namespace

Lazy initialization — only connects to VaultUnlocked on first use.

## LuckPerms (Soft Dependency)

Listed as a soft dependency in `manifest.json` for load ordering. Permission resolution falls through the `PermissionManager` chain if LuckPerms is present alongside or instead of HyperPerms.

## Ecotale (Detection Only)

Class detection via `Class.forName("com.ecotale.Ecotale")`. No active integration logic — reserved for future economy features (teleport costs, kit prices, warp fees).

## Werchat (Stub)

Stub class with no implementation. Reserved for future chat integration (announcement delivery through channels, module-specific formatting).
