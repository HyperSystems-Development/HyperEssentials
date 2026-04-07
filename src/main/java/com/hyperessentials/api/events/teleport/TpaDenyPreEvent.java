package com.hyperessentials.api.events.teleport;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a TPA request is denied.
 * Cancel to prevent the denial.
 */
public final class TpaDenyPreEvent implements Cancellable {

  private final UUID denierUuid;
  private final UUID requesterUuid;
  private boolean cancelled;
  private String cancelReason;

  public TpaDenyPreEvent(@NotNull UUID denierUuid, @NotNull UUID requesterUuid) {
    this.denierUuid = denierUuid;
    this.requesterUuid = requesterUuid;
  }

  @NotNull
  public UUID denierUuid() {
    return denierUuid;
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
