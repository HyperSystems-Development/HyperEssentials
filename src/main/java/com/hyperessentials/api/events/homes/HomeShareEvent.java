package com.hyperessentials.api.events.homes;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record HomeShareEvent(
    @NotNull UUID ownerUuid,
    @NotNull String homeName,
    @NotNull UUID targetUuid
) {}
