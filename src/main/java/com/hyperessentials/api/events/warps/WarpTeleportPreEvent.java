package com.hyperessentials.api.events.warps;

import com.hyperessentials.api.events.Cancellable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class WarpTeleportPreEvent implements Cancellable {

  private final UUID playerUuid;
  private final String warpName;
  private boolean cancelled;
  private String cancelReason;

  public WarpTeleportPreEvent(@NotNull UUID playerUuid, @NotNull String warpName) {
    this.playerUuid = playerUuid;
    this.warpName = warpName;
  }

  @NotNull public UUID playerUuid() { return playerUuid; }
  @NotNull public String warpName() { return warpName; }

  @Override public boolean isCancelled() { return cancelled; }
  @Override public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
  @Override @Nullable public String getCancelReason() { return cancelReason; }
  @Override public void setCancelReason(@Nullable String reason) { this.cancelReason = reason; }
}
