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
    BLOCKED_WILDERNESS,
    BLOCKED_ZONE;

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
        case BLOCKED_ZONE -> "This action is not allowed in this zone.";
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
  /**
   * Checks if a zone flag allows the action at the given location.
   * This checks ONLY zone flags — not relation-based territory settings.
   * Returns ALLOWED if HyperFactions is absent or zone flag allows.
   *
   * @param world    the world name
   * @param x        the x coordinate
   * @param z        the z coordinate
   * @param flagName the zone flag name
   * @return the check result
   */
  @NotNull
  public static Result checkZoneFlag(@NotNull String world, double x, double z,
                                     @NotNull String flagName) {
    if (!HyperFactionsIntegration.isAvailable()) return Result.ALLOWED;
    boolean allowed = HyperFactionsIntegration.isZoneFlagAllowed(world, x, z, flagName);
    return allowed ? Result.ALLOWED : Result.BLOCKED_ZONE;
  }

  @NotNull
  public static Result canUseHome(@NotNull UUID playerUuid, @NotNull String world,
                                  double x, double z) {
    // Soft dependency absent — allow everything
    if (!HyperFactionsIntegration.isAvailable()) {
      return Result.ALLOWED;
    }

    // Zone flag check — takes priority over relation-based checks
    Result zoneResult = checkZoneFlag(world, x, z, HyperFactionsIntegration.FLAG_HOMES);
    if (zoneResult != Result.ALLOWED) return zoneResult;

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
