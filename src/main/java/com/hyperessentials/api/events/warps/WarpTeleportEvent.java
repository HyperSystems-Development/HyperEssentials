package com.hyperessentials.api.events.warps;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record WarpTeleportEvent(
    @NotNull UUID playerUuid,
    @NotNull String warpName
) {}
