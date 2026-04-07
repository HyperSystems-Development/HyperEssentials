package com.hyperessentials.api.events.spawns;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SpawnDeleteEvent(
    @NotNull String worldUuid,
    @NotNull UUID actorUuid
) {}
