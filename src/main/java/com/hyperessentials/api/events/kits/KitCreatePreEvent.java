package com.hyperessentials.api.events.kits;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a new kit is created.
 * Cancel to prevent the kit from being created.
 */
public final class KitCreatePreEvent implements Cancellable {

  private final String kitName;
  private final UUID actorUuid;
  private boolean cancelled;
  private String cancelReason;

  public KitCreatePreEvent(@NotNull String kitName, @NotNull UUID actorUuid) {
    this.kitName = kitName;
    this.actorUuid = actorUuid;
  }

  @NotNull
  public String kitName() {
    return kitName;
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
