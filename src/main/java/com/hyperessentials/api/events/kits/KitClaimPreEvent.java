package com.hyperessentials.api.events.kits;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a player claims a kit.
 * Cancel to prevent the kit from being given.
 */
public final class KitClaimPreEvent implements Cancellable {

  private final UUID playerUuid;
  private final String kitName;
  private boolean cancelled;
  private String cancelReason;

  public KitClaimPreEvent(@NotNull UUID playerUuid, @NotNull String kitName) {
    this.playerUuid = playerUuid;
    this.kitName = kitName;
  }

  @NotNull
  public UUID playerUuid() {
    return playerUuid;
  }

  @NotNull
  public String kitName() {
    return kitName;
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
