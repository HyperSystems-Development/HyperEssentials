package com.hyperessentials.api.events.spawns;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SpawnSetEvent(
    @NotNull String worldUuid,
    @NotNull UUID actorUuid
) {}
