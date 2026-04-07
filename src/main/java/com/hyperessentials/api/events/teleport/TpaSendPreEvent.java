package com.hyperessentials.api.events.teleport;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a TPA or TPAHere request is sent.
 * Cancel to prevent the request from being sent.
 */
public final class TpaSendPreEvent implements Cancellable {

  private final UUID senderUuid;
  private final UUID targetUuid;
  private final boolean here;
  private boolean cancelled;
  private String cancelReason;

  public TpaSendPreEvent(@NotNull UUID senderUuid, @NotNull UUID targetUuid, boolean here) {
    this.senderUuid = senderUuid;
    this.targetUuid = targetUuid;
    this.here = here;
  }

  @NotNull
  public UUID senderUuid() {
    return senderUuid;
  }

  @NotNull
  public UUID targetUuid() {
    return targetUuid;
  }

  /** Whether this is a TPAHere request (true) or a regular TPA request (false). */
  public boolean here() {
    return here;
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
