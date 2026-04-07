package com.hyperessentials.api.events.moderation;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a player is muted.
 * Cancel to prevent the mute.
 */
public final class PlayerMutePreEvent implements Cancellable {

  private final UUID targetUuid;
  private final UUID actorUuid;
  private final String reason;
  private final Long durationMs;
  private boolean cancelled;
  private String cancelReason;

  public PlayerMutePreEvent(@NotNull UUID targetUuid, @NotNull UUID actorUuid,
                            @NotNull String reason, @Nullable Long durationMs) {
    this.targetUuid = targetUuid;
    this.actorUuid = actorUuid;
    this.reason = reason;
    this.durationMs = durationMs;
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

  /**
   * Duration in milliseconds, or {@code null} for a permanent mute.
   */
  @Nullable
  public Long durationMs() {
    return durationMs;
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
