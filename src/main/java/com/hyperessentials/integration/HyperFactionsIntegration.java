package com.hyperessentials.integration;

import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.data.Location;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Soft dependency on HyperFactions.
 *
 * <p>Compiles against HyperFactions (compileOnly) but remains safe when the
 * dependency is absent at runtime. All direct API references are isolated
 * inside the private {@link Delegate} inner class, which is only loaded
 * after {@link #init()} confirms the HyperFactions classes are present.
 */
public final class HyperFactionsIntegration {

  private static boolean available = false;
  private static boolean initialized = false;

  // Zone flag name constants (must match HyperFactions ZoneFlags)
  public static final String FLAG_HOMES = "essentials_homes";
  public static final String FLAG_WARPS = "essentials_warps";
  public static final String FLAG_KITS = "essentials_kits";
  public static final String FLAG_BACK = "essentials_back";

  private HyperFactionsIntegration() {}

  public static void init() {
    if (initialized) return;
    initialized = true;

    try {
      // Probe for the API class — throws ClassNotFoundException if absent
      Class.forName("com.hyperfactions.api.HyperFactionsAPI");

      // Delegate class references HF types directly; loading it here is
      // safe because we already confirmed the API class exists.
      boolean apiReady = Delegate.checkAvailable();
      if (!apiReady) {
        Logger.info("[Integration] HyperFactions detected but not yet available");
        return;
      }

      available = true;
      Logger.info("[Integration] HyperFactions integration initialized successfully");

      // Subscribe to events
      Delegate.subscribeToEvents();
    } catch (ClassNotFoundException e) {
      Logger.info("[Integration] HyperFactions not found - territory features disabled");
    } catch (Exception e) {
      ErrorHandler.report("[Integration] Failed to initialize HyperFactions integration", e);
    }
  }

  public static boolean isAvailable() { return available; }

  @Nullable
  public static String getFactionAtLocation(@NotNull String world, double x, double z) {
    if (!available) return null;
    try {
      return Delegate.getFactionAtLocation(world, x, z);
    } catch (Exception e) {
      return null;
    }
  }

  @Nullable
  public static String getRelationAtLocation(@NotNull UUID playerUuid, @NotNull String world, double x, double z) {
    if (!available) return null;
    try {
      return Delegate.getRelationAtLocation(playerUuid, world, x, z);
    } catch (Exception e) {
      return null;
    }
  }

  @NotNull
  public static String getTerritoryLabel(@NotNull String world, double x, double z) {
    String factionName = getFactionAtLocation(world, x, z);
    return factionName != null ? factionName : "Wilderness";
  }

  // === Faction Home Methods ===

  public static boolean hasFactionHome(@NotNull UUID playerUuid) {
    if (!available) return false;
    try {
      return Delegate.hasFactionHome(playerUuid);
    } catch (Exception e) {
      return false;
    }
  }

  @Nullable
  public static String getFactionHomeWorld(@NotNull UUID playerUuid) {
    if (!available) return null;
    try {
      return Delegate.getFactionHomeWorld(playerUuid);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns faction home coordinates as [x, y, z, yaw, pitch], or null.
   */
  @Nullable
  public static double[] getFactionHomeCoords(@NotNull UUID playerUuid) {
    if (!available) return null;
    try {
      return Delegate.getFactionHomeCoords(playerUuid);
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets the faction home cooldown remaining.
   */
  public static int getFactionHomeCooldownRemaining(@NotNull UUID playerUuid) {
    if (!available) return 0;
    try {
      return Delegate.getFactionHomeCooldownRemaining(playerUuid);
    } catch (Exception e) {
      return 0;
    }
  }

  // === Zone Flag Methods ===

  /**
   * Checks if a zone flag allows an action at the given location.
   * Returns true (fail-open) if HyperFactions is absent or the location is
   * not in a zone.
   *
   * @param world    the world name
   * @param x        the x coordinate
   * @param z        the z coordinate
   * @param flagName the zone flag name (use FLAG_HOMES, FLAG_WARPS, FLAG_KITS, FLAG_BACK)
   * @return true if allowed
   */
  public static boolean isZoneFlagAllowed(@NotNull String world, double x, double z,
                                          @NotNull String flagName) {
    if (!available) return true;
    try {
      return Delegate.isZoneFlagAllowed(world, x, z, flagName);
    } catch (Exception e) {
      return true;
    }
  }

  // === Language Sync ===

  /**
   * Syncs a language preference to HyperFactions.
   * Silently ignored if HyperFactions is absent or the locale is not supported.
   *
   * @param uuid     the player's UUID
   * @param language the language code (e.g. "en-US")
   */
  public static void syncLanguage(@NotNull UUID uuid, @NotNull String language) {
    if (!available) return;
    try {
      Delegate.syncLanguage(uuid, language);
    } catch (Exception e) {
      ErrorHandler.report("[Integration] Failed to sync language to HyperFactions", e);
    }
  }

  /**
   * Gets the language preference stored in HyperFactions for a player.
   * Returns null if HyperFactions is absent or has no preference stored.
   *
   * @param uuid the player's UUID
   * @return the language code, or null
   */
  @Nullable
  public static String getHFLanguage(@NotNull UUID uuid) {
    if (!available) return null;
    try {
      return Delegate.getHFLanguage(uuid);
    } catch (Exception e) {
      return null;
    }
  }

  // =========================================================================
  // Inner delegate — only loaded when HyperFactions classes are on the classpath
  // =========================================================================

  /**
   * Isolated class that directly references HyperFactions API types.
   * This class is never loaded by the JVM unless we have confirmed that
   * HyperFactions is present, preventing {@link NoClassDefFoundError}.
   */
  private static final class Delegate {

    static boolean checkAvailable() {
      return com.hyperfactions.api.HyperFactionsAPI.isAvailable();
    }

    static void subscribeToEvents() {
      // FactionHomeTeleportEvent — save back location when player uses /f home
      try {
        com.hyperfactions.api.events.EventBus.register(
            com.hyperfactions.api.events.FactionHomeTeleportEvent.class, event -> {
              try {
                if (!HyperEssentialsAPI.isAvailable()) return;
                TeleportModule tm = HyperEssentialsAPI.getInstance().getTeleportModule();
                if (tm == null || !tm.isEnabled()) return;
                BackManager bm = tm.getBackManager();
                if (bm == null) return;

                Location backLoc = new Location(event.sourceWorld(), "",
                    event.sourceX(), event.sourceY(), event.sourceZ(), 0, 0);
                bm.onTeleport(event.playerUuid(), backLoc, "factionhome");
                Logger.debugIntegration("Saved back location for %s from /f home teleport",
                    event.playerUuid());
              } catch (Exception e) {
                ErrorHandler.report("[Integration] Failed to save back location from /f home", e);
              }
            });
        Logger.info("[Integration] Subscribed to FactionHomeTeleportEvent for back tracking");
      } catch (Exception e) {
        Logger.debugIntegration("Failed to subscribe to FactionHomeTeleportEvent: %s", e.getMessage());
      }

      // PlayerTerritoryChangeEvent — debug logging for territory transitions
      try {
        com.hyperfactions.api.events.EventBus.register(
            com.hyperfactions.api.events.PlayerTerritoryChangeEvent.class, event -> {
              Logger.debugIntegration("Player %s moved territory: %s -> %s",
                  event.playerUuid(), event.oldFactionId(), event.newFactionId());
            });
        Logger.info("[Integration] Subscribed to PlayerTerritoryChangeEvent");
      } catch (Exception e) {
        Logger.debugIntegration("Failed to subscribe to PlayerTerritoryChangeEvent: %s", e.getMessage());
      }
    }

    @Nullable
    static String getFactionAtLocation(@NotNull String world, double x, double z) {
      UUID factionId = com.hyperfactions.api.HyperFactionsAPI.getClaimManager()
          .getClaimOwnerAt(world, x, z);
      if (factionId == null) return null;
      com.hyperfactions.data.Faction faction =
          com.hyperfactions.api.HyperFactionsAPI.getFaction(factionId);
      return faction != null ? faction.name() : null;
    }

    @Nullable
    static String getRelationAtLocation(@NotNull UUID playerUuid, @NotNull String world,
                                        double x, double z) {
      UUID territoryFactionId = com.hyperfactions.api.HyperFactionsAPI.getClaimManager()
          .getClaimOwnerAt(world, x, z);
      if (territoryFactionId == null) return null;

      com.hyperfactions.data.Faction playerFaction =
          com.hyperfactions.api.HyperFactionsAPI.getPlayerFaction(playerUuid);
      if (playerFaction == null) return "NEUTRAL";

      UUID playerFactionId = playerFaction.id();
      if (playerFactionId.equals(territoryFactionId)) return "OWN";

      com.hyperfactions.data.RelationType relation =
          com.hyperfactions.api.HyperFactionsAPI.getRelation(playerFactionId, territoryFactionId);
      return relation.name();
    }

    static boolean hasFactionHome(@NotNull UUID playerUuid) {
      return com.hyperfactions.api.HyperFactionsAPI.hasFactionHome(playerUuid);
    }

    @Nullable
    static String getFactionHomeWorld(@NotNull UUID playerUuid) {
      return com.hyperfactions.api.HyperFactionsAPI.getFactionHomeWorld(playerUuid);
    }

    @Nullable
    static double[] getFactionHomeCoords(@NotNull UUID playerUuid) {
      return com.hyperfactions.api.HyperFactionsAPI.getFactionHomeCoords(playerUuid);
    }

    static int getFactionHomeCooldownRemaining(@NotNull UUID playerUuid) {
      return com.hyperfactions.api.HyperFactionsAPI.getFactionHomeCooldownRemaining(playerUuid);
    }

    static boolean isZoneFlagAllowed(@NotNull String world, double x, double z,
                                     @NotNull String flagName) {
      return com.hyperfactions.api.HyperFactionsAPI.isZoneFlagAllowed(world, x, z, flagName);
    }

    static void syncLanguage(@NotNull UUID uuid, @NotNull String language) {
      // Only sync if HyperFactions supports this locale
      if (com.hyperfactions.api.HyperFactionsAPI.getSupportedLocales().contains(language)) {
        com.hyperfactions.api.HyperFactionsAPI.setPlayerLanguage(uuid, language);
      }
    }

    @Nullable
    static String getHFLanguage(@NotNull UUID uuid) {
      String lang = com.hyperfactions.api.HyperFactionsAPI.getPlayerLanguage(uuid);
      // getPlayerLanguage returns server default if no preference — we need to
      // distinguish "explicit preference" from "no preference". The API returns
      // the server default when there's no override, so we can't distinguish.
      // Return the value as-is and let the caller decide.
      return lang;
    }
  }
}
