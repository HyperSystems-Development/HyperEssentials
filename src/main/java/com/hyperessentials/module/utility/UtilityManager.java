package com.hyperessentials.module.utility;

import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages session-only states for utility commands (fly, god).
 * All states are cleared on player disconnect and server shutdown.
 */
public class UtilityManager {

  private final Set<UUID> flyingPlayers = ConcurrentHashMap.newKeySet();
  private final Set<UUID> godPlayers = ConcurrentHashMap.newKeySet();

  // === Fly ===

  public boolean isFlying(@NotNull UUID uuid) {
    return flyingPlayers.contains(uuid);
  }

  public boolean toggleFly(@NotNull UUID uuid) {
    if (flyingPlayers.contains(uuid)) {
      flyingPlayers.remove(uuid);
      return false;
    } else {
      flyingPlayers.add(uuid);
      return true;
    }
  }

  public void setFlying(@NotNull UUID uuid, boolean flying) {
    if (flying) flyingPlayers.add(uuid);
    else flyingPlayers.remove(uuid);
  }

  // === God ===

  public boolean isGod(@NotNull UUID uuid) {
    return godPlayers.contains(uuid);
  }

  public boolean toggleGod(@NotNull UUID uuid) {
    if (godPlayers.contains(uuid)) {
      godPlayers.remove(uuid);
      return false;
    } else {
      godPlayers.add(uuid);
      return true;
    }
  }

  public void setGod(@NotNull UUID uuid, boolean god) {
    if (god) godPlayers.add(uuid);
    else godPlayers.remove(uuid);
  }

  // === Cleanup ===

  public void onPlayerDisconnect(@NotNull UUID uuid) {
    flyingPlayers.remove(uuid);
    godPlayers.remove(uuid);
  }

  public void shutdown() {
    flyingPlayers.clear();
    godPlayers.clear();
  }
}
