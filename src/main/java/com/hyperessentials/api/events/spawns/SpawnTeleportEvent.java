package com.hyperessentials.api.events.spawns;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SpawnTeleportEvent(
    @NotNull UUID playerUuid,
    @NotNull String worldUuid
) {}
