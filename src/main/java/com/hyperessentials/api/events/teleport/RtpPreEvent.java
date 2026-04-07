package com.hyperessentials.api.events.teleport;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a random teleport is initiated.
 * Cancel to prevent the random teleport.
 */
public final class RtpPreEvent implements Cancellable {

  private final UUID playerUuid;
  private final String world;
  private boolean cancelled;
  private String cancelReason;

  public RtpPreEvent(@NotNull UUID playerUuid, @NotNull String world) {
    this.playerUuid = playerUuid;
    this.world = world;
  }

  @NotNull
  public UUID playerUuid() {
    return playerUuid;
  }

  @NotNull
  public String world() {
    return world;
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
