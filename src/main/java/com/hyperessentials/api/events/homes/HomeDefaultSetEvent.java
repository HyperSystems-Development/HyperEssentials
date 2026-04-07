package com.hyperessentials.api.events.homes;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record HomeDefaultSetEvent(
    @NotNull UUID playerUuid,
    @NotNull String homeName
) {}
