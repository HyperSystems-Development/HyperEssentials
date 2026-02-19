# Integrations

> **Status:** Permission integration complete, others are stubs.

## HyperPerms (Soft Dependency)

HyperEssentials uses HyperPerms for permission checks when available. Detection is reflection-based — no hard compile-time dependency at runtime.

**Resolution chain:**
1. Try HyperPerms via `HyperPermsProviderAdapter`
2. Fall back to OP check
3. If `allowWithoutPermissionMod = true`, allow by default

**Features used:**
- `hasPermission(uuid, node)` — standard permission checks
- `getPermissionValue(uuid, prefix, default)` — numeric limits (e.g., home count)
- Wildcard resolution (e.g., `hyperessentials.*`)

## HyperFactions (Soft Dependency)

Reflection-based integration for territory awareness:
- `getFactionAtLocation(world, x, z)` — faction name at coordinates
- `getFactionIdAtLocation(world, x, z)` — faction UUID at coordinates
- `getRelationAtLocation(playerUuid, world, x, z)` — relation type (OWN, ALLY, NEUTRAL, ENEMY)
- `canSetHomeAtLocation(playerUuid, world, x, z)` — territory restriction check

Used by the homes module to restrict home placement in enemy territory.

## Ecotale (Planned)

Stub for future economy integration:
- Teleport costs
- Kit purchase prices
- Warp creation fees

## Werchat (Planned)

Stub for future chat integration:
- Announcement delivery through chat channels
- Module-specific chat formatting
