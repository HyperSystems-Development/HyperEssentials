package com.hyperessentials.api.events.moderation;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a player is unmuted.
 * Cancel to prevent the unmute.
 */
public final class PlayerUnmutePreEvent implements Cancellable {

  private final UUID targetUuid;
  private final UUID actorUuid;
  private boolean cancelled;
  private String cancelReason;

  public PlayerUnmutePreEvent(@NotNull UUID targetUuid, @NotNull UUID actorUuid) {
    this.targetUuid = targetUuid;
    this.actorUuid = actorUuid;
  }

  @NotNull
  public UUID targetUuid() {
    return targetUuid;
  }

  @NotNull
  public UUID actorUuid() {
    return actorUuid;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
  }

  @Override
  @Nullable
  public String getCancelReason() {
    return cancelReason;
  }

  @Override
  public void setCancelReason(@Nullable String reason) {
    this.cancelReason = reason;
  }
}
