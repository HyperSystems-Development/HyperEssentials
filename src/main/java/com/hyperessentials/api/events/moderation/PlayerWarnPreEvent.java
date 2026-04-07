package com.hyperessentials.api.events.moderation;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a player is warned.
 * Cancel to prevent the warning.
 */
public final class PlayerWarnPreEvent implements Cancellable {

  private final UUID targetUuid;
  private final UUID actorUuid;
  private final String reason;
  private boolean cancelled;
  private String cancelReason;

  public PlayerWarnPreEvent(@NotNull UUID targetUuid, @NotNull UUID actorUuid,
                            @NotNull String reason) {
    this.targetUuid = targetUuid;
    this.actorUuid = actorUuid;
    this.reason = reason;
  }

  @NotNull
  public UUID targetUuid() {
    return targetUuid;
  }

  @NotNull
  public UUID actorUuid() {
    return actorUuid;
  }

  @NotNull
  public String reason() {
    return reason;
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
