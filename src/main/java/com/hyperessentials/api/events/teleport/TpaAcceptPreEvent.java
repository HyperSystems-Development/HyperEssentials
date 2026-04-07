package com.hyperessentials.api.events.teleport;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a TPA request is accepted.
 * Cancel to prevent the acceptance.
 */
public final class TpaAcceptPreEvent implements Cancellable {

  private final UUID accepterUuid;
  private final UUID requesterUuid;
  private boolean cancelled;
  private String cancelReason;

  public TpaAcceptPreEvent(@NotNull UUID accepterUuid, @NotNull UUID requesterUuid) {
    this.accepterUuid = accepterUuid;
    this.requesterUuid = requesterUuid;
  }

  @NotNull
  public UUID accepterUuid() {
    return accepterUuid;
  }

  @NotNull
  public UUID requesterUuid() {
    return requesterUuid;
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
