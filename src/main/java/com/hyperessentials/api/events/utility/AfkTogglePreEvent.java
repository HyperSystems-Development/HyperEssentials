package com.hyperessentials.api.events.utility;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a player's AFK state is toggled.
 * Cancel to prevent the toggle.
 */
public final class AfkTogglePreEvent implements Cancellable {

  private final UUID playerUuid;
  private final boolean newState;
  private boolean cancelled;
  private String cancelReason;

  public AfkTogglePreEvent(@NotNull UUID playerUuid, boolean newState) {
    this.playerUuid = playerUuid;
    this.newState = newState;
  }

  @NotNull
  public UUID playerUuid() {
    return playerUuid;
  }

  /** The AFK state that will be set (true = going AFK, false = returning from AFK). */
  public boolean newState() {
    return newState;
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
