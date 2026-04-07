package com.hyperessentials.api.events.homes;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class HomeUnsharePreEvent implements Cancellable {

  private final UUID ownerUuid;
  private final String homeName;
  private final UUID targetUuid;
  private boolean cancelled;
  private String cancelReason;

  public HomeUnsharePreEvent(@NotNull UUID ownerUuid, @NotNull String homeName,
                             @NotNull UUID targetUuid) {
    this.ownerUuid = ownerUuid;
    this.homeName = homeName;
    this.targetUuid = targetUuid;
  }

  @NotNull public UUID ownerUuid() { return ownerUuid; }
  @NotNull public String homeName() { return homeName; }
  @NotNull public UUID targetUuid() { return targetUuid; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
