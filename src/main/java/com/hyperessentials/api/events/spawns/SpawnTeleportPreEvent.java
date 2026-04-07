package com.hyperessentials.api.events.spawns;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SpawnTeleportPreEvent implements Cancellable {

  private final UUID playerUuid;
  private final String worldUuid;
  private boolean cancelled;
  private String cancelReason;

  public SpawnTeleportPreEvent(@NotNull UUID playerUuid, @NotNull String worldUuid) {
    this.playerUuid = playerUuid;
    this.worldUuid = worldUuid;
  }

  @NotNull public UUID playerUuid() { return playerUuid; }
  @NotNull public String worldUuid() { return worldUuid; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
