package com.hyperessentials.api.events.warps;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record WarpSetEvent(
    @NotNull String warpName,
    @NotNull UUID actorUuid
) {}
