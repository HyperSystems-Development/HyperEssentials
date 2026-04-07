package com.hyperessentials.api.events.kits;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a kit is deleted.
 * Cancel to prevent the kit from being deleted.
 */
public final class KitDeletePreEvent implements Cancellable {

  private final String kitName;
  private final UUID actorUuid;
  private boolean cancelled;
  private String cancelReason;

  public KitDeletePreEvent(@NotNull String kitName, @NotNull UUID actorUuid) {
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
