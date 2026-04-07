package com.hyperessentials.api.events.homes;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record HomeDeleteEvent(
    @NotNull UUID playerUuid,
    @NotNull String homeName
) {}
