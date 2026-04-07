package com.hyperessentials.api.events.homes;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class HomeDefaultSetPreEvent implements Cancellable {

  private final UUID playerUuid;
  private final String homeName;
  private boolean cancelled;
  private String cancelReason;

  public HomeDefaultSetPreEvent(@NotNull UUID playerUuid, @NotNull String homeName) {
    this.playerUuid = playerUuid;
    this.homeName = homeName;
  }

  @NotNull public UUID playerUuid() { return playerUuid; }
  @NotNull public String homeName() { return homeName; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
