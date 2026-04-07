package com.hyperessentials.api.events.homes;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class HomeSetPreEvent implements Cancellable {

  private final UUID playerUuid;
  private final String homeName;
  private final String world;
  private final double x;
  private final double y;
  private final double z;
  private boolean cancelled;
  private String cancelReason;

  public HomeSetPreEvent(@NotNull UUID playerUuid, @NotNull String homeName,
                         @NotNull String world, double x, double y, double z) {
    this.playerUuid = playerUuid;
    this.homeName = homeName;
    this.world = world;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @NotNull public UUID playerUuid() { return playerUuid; }
  @NotNull public String homeName() { return homeName; }
  @NotNull public String world() { return world; }
  public double x() { return x; }
  public double y() { return y; }
  public double z() { return z; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
