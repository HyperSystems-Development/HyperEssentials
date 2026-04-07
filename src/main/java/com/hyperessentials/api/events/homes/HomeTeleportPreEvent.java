package com.hyperessentials.api.events.homes;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class HomeTeleportPreEvent implements Cancellable {

  private final UUID playerUuid;
  private final String homeName;
  private final String world;
  private boolean cancelled;
  private String cancelReason;

  public HomeTeleportPreEvent(@NotNull UUID playerUuid, @NotNull String homeName,
                              @NotNull String world) {
    this.playerUuid = playerUuid;
    this.homeName = homeName;
    this.world = world;
  }

  @NotNull public UUID playerUuid() { return playerUuid; }
  @NotNull public String homeName() { return homeName; }
  @NotNull public String world() { return world; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
