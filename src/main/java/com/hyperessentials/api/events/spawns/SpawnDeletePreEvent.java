package com.hyperessentials.api.events.spawns;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SpawnDeletePreEvent implements Cancellable {

  private final String worldUuid;
  private final UUID actorUuid;
  private boolean cancelled;
  private String cancelReason;

  public SpawnDeletePreEvent(@NotNull String worldUuid, @NotNull UUID actorUuid) {
    this.worldUuid = worldUuid;
    this.actorUuid = actorUuid;
  }

  @NotNull public String worldUuid() { return worldUuid; }
  @NotNull public UUID actorUuid() { return actorUuid; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
