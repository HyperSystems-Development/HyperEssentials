package com.hyperessentials.module.teleport;

import com.hyperessentials.config.modules.TeleportConfig;
import com.hyperessentials.data.Location;
import com.hyperessentials.data.PlayerData;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Manages /back location history for players.
 * Works in conjunction with TpaManager for player data storage.
 */
public class BackManager {

  private final TpaManager tpaManager;
  private final TeleportConfig config;

  public BackManager(@NotNull TpaManager tpaManager, @NotNull TeleportConfig config) {
    this.tpaManager = tpaManager;
    this.config = config;
  }

  public void saveBackLocation(@NotNull UUID uuid, @NotNull Location location) {
    PlayerData data = tpaManager.getPlayerData(uuid);
    if (data == null) {
      Logger.debug("Cannot save back location - player %s not loaded", uuid);
      return;
    }

    int maxSize = config.getBackHistorySize();
    data.addBackLocation(location, maxSize);
    tpaManager.savePlayer(uuid);

    Logger.debug("Saved back location for %s: %s %.0f, %.0f, %.0f",
      uuid, location.world(), location.x(), location.y(), location.z());
  }

  public void onTeleport(@NotNull UUID uuid, @NotNull Location location) {
    if (config.isSaveBackOnTeleport()) {
      saveBackLocation(uuid, location);
    }
  }

  public void onDeath(@NotNull UUID uuid, @NotNull Location location) {
    if (config.isSaveBackOnDeath()) {
      saveBackLocation(uuid, location);
    }
  }

  @Nullable
  public Location getBackLocation(@NotNull UUID uuid) {
    PlayerData data = tpaManager.getPlayerData(uuid);
    if (data == null) {
      return null;
    }
    return data.getLastBackLocation();
  }

  @Nullable
  public Location popBackLocation(@NotNull UUID uuid) {
    PlayerData data = tpaManager.getPlayerData(uuid);
    if (data == null) {
      return null;
    }

    Location location = data.popBackLocation();
    if (location != null) {
      tpaManager.savePlayer(uuid);
      Logger.debug("Popped back location for %s", uuid);
    }
    return location;
  }

  /**
   * Returns the full back history for the player (most recent first).
   */
  @NotNull
  public List<Location> getBackHistory(@NotNull UUID uuid) {
    PlayerData data = tpaManager.getPlayerData(uuid);
    return data != null ? data.getBackHistory() : List.of();
  }

  /**
   * Removes a back entry at the given index and returns it.
   */
  @Nullable
  public Location removeBackEntry(@NotNull UUID uuid, int index) {
    PlayerData data = tpaManager.getPlayerData(uuid);
    if (data == null) return null;
    Location removed = data.removeBackLocation(index);
    if (removed != null) {
      tpaManager.savePlayer(uuid);
    }
    return removed;
  }

  public boolean hasBackHistory(@NotNull UUID uuid) {
    PlayerData data = tpaManager.getPlayerData(uuid);
    return data != null && !data.getBackHistory().isEmpty();
  }

  public void clearHistory(@NotNull UUID uuid) {
    PlayerData data = tpaManager.getPlayerData(uuid);
    if (data != null) {
      data.clearBackHistory();
      tpaManager.savePlayer(uuid);
      Logger.debug("Cleared back history for %s", uuid);
    }
  }

  public int getHistorySize(@NotNull UUID uuid) {
    PlayerData data = tpaManager.getPlayerData(uuid);
    return data != null ? data.getBackHistory().size() : 0;
  }
}
