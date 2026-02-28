package com.hyperessentials.integration;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.HomesConfig;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Centralized faction territory check for home operations.
 * Reads HomesConfig faction toggles and delegates to HyperFactionsIntegration.
 */
public final class FactionTerritoryChecker {

  public enum Result {
    ALLOWED,
    BLOCKED_OWN_TERRITORY,
    BLOCKED_ALLY_TERRITORY,
    BLOCKED_ENEMY_TERRITORY,
    BLOCKED_NEUTRAL_TERRITORY,
    BLOCKED_WILDERNESS;

    /**
     * Returns a player-facing denial message for this result.
     */
    @NotNull
    public String getDenialMessage() {
      return switch (this) {
        case BLOCKED_OWN_TERRITORY -> "You cannot use homes in your faction's territory.";
        case BLOCKED_ALLY_TERRITORY -> "You cannot use homes in allied territory.";
        case BLOCKED_ENEMY_TERRITORY -> "You cannot use homes in enemy territory.";
        case BLOCKED_NEUTRAL_TERRITORY -> "You cannot use homes in neutral territory.";
        case BLOCKED_WILDERNESS -> "You cannot use homes in the wilderness.";
        case ALLOWED -> "";
      };
    }
  }

  private FactionTerritoryChecker() {}

  /**
   * Checks whether a player can use a home at the given location.
   * Returns ALLOWED if HyperFactions is absent or faction checks are disabled.
   *
   * @param playerUuid the player's UUID
   * @param world      the world name
   * @param x          the x coordinate
   * @param z          the z coordinate
   * @return the check result
   */
  @NotNull
  public static Result canUseHome(@NotNull UUID playerUuid, @NotNull String world,
                                  double x, double z) {
    // Soft dependency absent — allow everything
    if (!HyperFactionsIntegration.isAvailable()) {
      return Result.ALLOWED;
    }

    // Master toggle off — allow everything
    HomesConfig config = ConfigManager.get().homes();
    if (!config.isFactionsEnabled()) {
      return Result.ALLOWED;
    }

    // Check if location is in a faction claim
    String factionName = HyperFactionsIntegration.getFactionAtLocation(world, x, z);
    if (factionName == null) {
      // Wilderness
      if (config.isAllowInWilderness()) {
        return Result.ALLOWED;
      }
      Logger.debug("[Factions] Blocked home in wilderness for %s", playerUuid);
      return Result.BLOCKED_WILDERNESS;
    }

    // Get relation between player and territory owner
    String relation = HyperFactionsIntegration.getRelationAtLocation(playerUuid, world, x, z);
    if (relation == null) {
      // Player not in a faction, treat as neutral
      relation = "NEUTRAL";
    }

    return switch (relation) {
      case "OWN" -> config.isAllowInOwnTerritory()
          ? Result.ALLOWED : Result.BLOCKED_OWN_TERRITORY;
      case "ALLY" -> config.isAllowInAllyTerritory()
          ? Result.ALLOWED : Result.BLOCKED_ALLY_TERRITORY;
      case "ENEMY" -> config.isAllowInEnemyTerritory()
          ? Result.ALLOWED : Result.BLOCKED_ENEMY_TERRITORY;
      default -> config.isAllowInNeutralTerritory()
          ? Result.ALLOWED : Result.BLOCKED_NEUTRAL_TERRITORY;
    };
  }
}
