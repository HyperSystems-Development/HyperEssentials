package com.hyperessentials.api.events.teleport;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Fired before a player teleports back to their previous location.
 * Cancel to prevent the back-teleport.
 */
public final class BackTeleportPreEvent implements Cancellable {

  private final UUID playerUuid;
  private final String world;
  private final double x;
  private final double y;
  private final double z;
  private final String source;
  private boolean cancelled;
  private String cancelReason;

  public BackTeleportPreEvent(@NotNull UUID playerUuid, @NotNull String world,
                              double x, double y, double z, @NotNull String source) {
    this.playerUuid = playerUuid;
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
    this.source = source;
  }

  @NotNull
  public UUID playerUuid() {
    return playerUuid;
  }

  @NotNull
  public String world() {
    return world;
  }

  public double x() {
    return x;
  }

  public double y() {
    return y;
  }

  public double z() {
    return z;
  }

  /** The source that recorded this back location (e.g. "tpa", "warp", "home", "death"). */
  @NotNull
  public String source() {
    return source;
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
