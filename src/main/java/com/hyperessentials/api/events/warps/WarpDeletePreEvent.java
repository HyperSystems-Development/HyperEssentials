package com.hyperessentials.api.events.warps;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class WarpDeletePreEvent implements Cancellable {

  private final String warpName;
  private final UUID actorUuid;
  private boolean cancelled;
  private String cancelReason;

  public WarpDeletePreEvent(@NotNull String warpName, @NotNull UUID actorUuid) {
    this.warpName = warpName;
    this.actorUuid = actorUuid;
  }

  @NotNull public String warpName() { return warpName; }
  @NotNull public UUID actorUuid() { return actorUuid; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
