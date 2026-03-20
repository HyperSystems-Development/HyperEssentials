package com.hyperessentials.integration;

import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.data.Location;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Reflection-based soft dependency on HyperFactions.
 */
public final class HyperFactionsIntegration {

  private static boolean available = false;
  private static boolean initialized = false;

  private static Method isAvailableMethod;
  private static Method getClaimManagerMethod;
  private static Method getFactionManagerMethod;
  private static Method getRelationManagerMethod;
  private static Method getClaimOwnerAtMethod;
  private static Method getFactionMethod;
  private static Method getPlayerFactionIdMethod;
  private static Method getRelationMethod;
  private static Method factionNameMethod;
  private static Method relationTypeNameMethod;

  // API convenience methods (available in newer HyperFactions)
  private static Method apiHasFactionHomeMethod;
  private static Method apiGetFactionHomeWorldMethod;
  private static Method apiGetFactionHomeCoordsMethod;
  private static Method apiGetFactionHomeCooldownMethod;

  // Fallback: Faction class methods (for older HyperFactions without API convenience methods)
  private static Class<?> factionClass;
  private static Method factionHasHomeMethod;
  private static Method factionHomeMethod;

  // FactionHome record accessors (fallback)
  private static Method homeWorldMethod;
  private static Method homeXMethod;
  private static Method homeYMethod;
  private static Method homeZMethod;
  private static Method homeYawMethod;
  private static Method homePitchMethod;

  // Zone flag method
  private static Method isZoneFlagAllowedMethod;

  // EventBus methods
  private static Method eventBusRegisterMethod;
  private static Method eventBusUnregisterMethod;

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
      Class<?> apiClass = Class.forName("com.hyperfactions.api.HyperFactionsAPI");
      isAvailableMethod = apiClass.getMethod("isAvailable");
      boolean apiAvailable = (boolean) isAvailableMethod.invoke(null);

      if (!apiAvailable) {
        Logger.info("[Integration] HyperFactions detected but not yet available");
        return;
      }

      getClaimManagerMethod = apiClass.getMethod("getClaimManager");
      getFactionManagerMethod = apiClass.getMethod("getFactionManager");
      getRelationManagerMethod = apiClass.getMethod("getRelationManager");

      Object claimManager = getClaimManagerMethod.invoke(null);
      if (claimManager != null) {
        getClaimOwnerAtMethod = claimManager.getClass()
            .getMethod("getClaimOwnerAt", String.class, double.class, double.class);
      }

      Object factionManager = getFactionManagerMethod.invoke(null);
      if (factionManager != null) {
        getFactionMethod = factionManager.getClass().getMethod("getFaction", UUID.class);
        getPlayerFactionIdMethod = factionManager.getClass().getMethod("getPlayerFactionId", UUID.class);
      }

      Object relationManager = getRelationManagerMethod.invoke(null);
      if (relationManager != null) {
        getRelationMethod = relationManager.getClass().getMethod("getRelation", UUID.class, UUID.class);
      }

      factionClass = Class.forName("com.hyperfactions.data.Faction");
      factionNameMethod = factionClass.getMethod("name");

      Class<?> relationTypeClass = Class.forName("com.hyperfactions.data.RelationType");
      relationTypeNameMethod = relationTypeClass.getMethod("name");

      available = true;
      Logger.info("[Integration] HyperFactions integration initialized successfully");

      // Try API convenience methods first (newer HyperFactions)
      try {
        apiHasFactionHomeMethod = apiClass.getMethod("hasFactionHome", UUID.class);
        apiGetFactionHomeWorldMethod = apiClass.getMethod("getFactionHomeWorld", UUID.class);
        apiGetFactionHomeCoordsMethod = apiClass.getMethod("getFactionHomeCoords", UUID.class);
        apiGetFactionHomeCooldownMethod = apiClass.getMethod("getFactionHomeCooldownRemaining", UUID.class);
        Logger.info("[Integration] HyperFactions faction home API available");
      } catch (NoSuchMethodException e) {
        Logger.info("[Integration] HyperFactions faction home API not available, using fallback");
        // Fallback: resolve Faction class methods directly
        try {
          factionHasHomeMethod = factionClass.getMethod("hasHome");
          factionHomeMethod = factionClass.getMethod("home");

          Class<?> factionHomeClass = Class.forName("com.hyperfactions.data.Faction$FactionHome");
          homeWorldMethod = factionHomeClass.getMethod("world");
          homeXMethod = factionHomeClass.getMethod("x");
          homeYMethod = factionHomeClass.getMethod("y");
          homeZMethod = factionHomeClass.getMethod("z");
          homeYawMethod = factionHomeClass.getMethod("yaw");
          homePitchMethod = factionHomeClass.getMethod("pitch");
          Logger.info("[Integration] HyperFactions faction home access available (via Faction class)");
        } catch (Exception ex) {
          Logger.info("[Integration] HyperFactions faction home access not available: %s", ex.getMessage());
        }
      }

      // Zone flag methods
      try {
        isZoneFlagAllowedMethod = apiClass.getMethod("isZoneFlagAllowed",
            String.class, double.class, double.class, String.class);
        Logger.info("[Integration] HyperFactions zone flag API available");
      } catch (NoSuchMethodException e) {
        Logger.info("[Integration] HyperFactions zone flag API not available (older version)");
      }

      // EventBus — subscribe to events for integration
      try {
        Class<?> eventBusClass = Class.forName("com.hyperfactions.api.events.EventBus");
        eventBusRegisterMethod = eventBusClass.getMethod("register", Class.class, Consumer.class);
        eventBusUnregisterMethod = eventBusClass.getMethod("unregister", Class.class, Consumer.class);
        subscribeToEvents();
      } catch (Exception e) {
        Logger.info("[Integration] HyperFactions EventBus not available: %s", e.getMessage());
      }

    } catch (ClassNotFoundException e) {
      Logger.info("[Integration] HyperFactions not found - territory features disabled");
    } catch (Exception e) {
      ErrorHandler.report("[Integration] Failed to initialize HyperFactions integration", e);
    }
  }

  /**
   * Subscribes to HyperFactions events for integration features.
   */
  private static void subscribeToEvents() {
    // FactionHomeTeleportEvent — save back location when player uses /f home
    try {
      Class<?> eventClass = Class.forName("com.hyperfactions.api.events.FactionHomeTeleportEvent");

      // Event record accessors
      Method playerUuidMethod = eventClass.getMethod("playerUuid");
      Method sourceWorldMethod = eventClass.getMethod("sourceWorld");
      Method sourceXMethod = eventClass.getMethod("sourceX");
      Method sourceYMethod = eventClass.getMethod("sourceY");
      Method sourceZMethod = eventClass.getMethod("sourceZ");

      Consumer<Object> listener = event -> {
        try {
          if (!HyperEssentialsAPI.isAvailable()) return;
          TeleportModule tm = HyperEssentialsAPI.getInstance().getTeleportModule();
          if (tm == null || !tm.isEnabled()) return;
          BackManager bm = tm.getBackManager();
          if (bm == null) return;

          UUID uuid = (UUID) playerUuidMethod.invoke(event);
          String srcWorld = (String) sourceWorldMethod.invoke(event);
          double srcX = (double) sourceXMethod.invoke(event);
          double srcY = (double) sourceYMethod.invoke(event);
          double srcZ = (double) sourceZMethod.invoke(event);

          Location backLoc = new Location(srcWorld, "", srcX, srcY, srcZ, 0, 0);
          bm.onTeleport(uuid, backLoc, "factionhome");
          Logger.debug("[Integration] Saved back location for %s from /f home teleport", uuid);
        } catch (Exception e) {
          ErrorHandler.report("[Integration] Failed to save back location from /f home", e);
        }
      };

      eventBusRegisterMethod.invoke(null, eventClass, listener);
      Logger.info("[Integration] Subscribed to FactionHomeTeleportEvent for back tracking");
    } catch (ClassNotFoundException e) {
      Logger.info("[Integration] FactionHomeTeleportEvent not available (older HyperFactions)");
    } catch (Exception e) {
      Logger.debug("[Integration] Failed to subscribe to FactionHomeTeleportEvent: %s", e.getMessage());
    }
  }

  public static boolean isAvailable() { return available; }

  @Nullable
  public static String getFactionAtLocation(@NotNull String world, double x, double z) {
    if (!available) return null;
    try {
      Object claimManager = getClaimManagerMethod.invoke(null);
      if (claimManager == null) return null;
      UUID factionId = (UUID) getClaimOwnerAtMethod.invoke(claimManager, world, x, z);
      if (factionId == null) return null;
      Object factionManager = getFactionManagerMethod.invoke(null);
      if (factionManager == null) return null;
      Object faction = getFactionMethod.invoke(factionManager, factionId);
      if (faction == null) return null;
      return (String) factionNameMethod.invoke(faction);
    } catch (Exception e) {
      return null;
    }
  }

  @Nullable
  public static String getRelationAtLocation(@NotNull UUID playerUuid, @NotNull String world, double x, double z) {
    if (!available) return null;
    try {
      Object claimManager = getClaimManagerMethod.invoke(null);
      if (claimManager == null) return null;
      UUID territoryFactionId = (UUID) getClaimOwnerAtMethod.invoke(claimManager, world, x, z);
      if (territoryFactionId == null) return null;
      Object factionManager = getFactionManagerMethod.invoke(null);
      if (factionManager == null) return null;
      UUID playerFactionId = (UUID) getPlayerFactionIdMethod.invoke(factionManager, playerUuid);
      if (playerFactionId == null) return null;
      if (playerFactionId.equals(territoryFactionId)) return "OWN";
      Object relationManager = getRelationManagerMethod.invoke(null);
      if (relationManager == null) return null;
      Object relationType = getRelationMethod.invoke(relationManager, playerFactionId, territoryFactionId);
      if (relationType == null) return "NEUTRAL";
      return (String) relationTypeNameMethod.invoke(relationType);
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

  /**
   * Gets the player's faction object via reflection, or null if not in a faction.
   */
  @Nullable
  private static Object getPlayerFaction(@NotNull UUID playerUuid) {
    if (!available) return null;
    try {
      Object factionManager = getFactionManagerMethod.invoke(null);
      if (factionManager == null) return null;
      UUID factionId = (UUID) getPlayerFactionIdMethod.invoke(factionManager, playerUuid);
      if (factionId == null) return null;
      return getFactionMethod.invoke(factionManager, factionId);
    } catch (Exception e) {
      return null;
    }
  }

  public static boolean hasFactionHome(@NotNull UUID playerUuid) {
    if (!available) return false;
    // Prefer API method
    if (apiHasFactionHomeMethod != null) {
      try {
        return (boolean) apiHasFactionHomeMethod.invoke(null, playerUuid);
      } catch (Exception e) { /* fall through */ }
    }
    // Fallback to Faction class reflection
    if (factionHasHomeMethod == null) return false;
    try {
      Object faction = getPlayerFaction(playerUuid);
      if (faction == null) return false;
      return (boolean) factionHasHomeMethod.invoke(faction);
    } catch (Exception e) {
      return false;
    }
  }

  @Nullable
  public static String getFactionHomeWorld(@NotNull UUID playerUuid) {
    if (!available) return null;
    // Prefer API method
    if (apiGetFactionHomeWorldMethod != null) {
      try {
        return (String) apiGetFactionHomeWorldMethod.invoke(null, playerUuid);
      } catch (Exception e) { /* fall through */ }
    }
    // Fallback to Faction class reflection
    if (factionHomeMethod == null || homeWorldMethod == null) return null;
    try {
      Object faction = getPlayerFaction(playerUuid);
      if (faction == null) return null;
      Object home = factionHomeMethod.invoke(faction);
      if (home == null) return null;
      return (String) homeWorldMethod.invoke(home);
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
    // Prefer API method
    if (apiGetFactionHomeCoordsMethod != null) {
      try {
        return (double[]) apiGetFactionHomeCoordsMethod.invoke(null, playerUuid);
      } catch (Exception e) { /* fall through */ }
    }
    // Fallback to Faction class reflection
    if (factionHomeMethod == null) return null;
    try {
      Object faction = getPlayerFaction(playerUuid);
      if (faction == null) return null;
      Object home = factionHomeMethod.invoke(faction);
      if (home == null) return null;
      double x = (double) homeXMethod.invoke(home);
      double y = (double) homeYMethod.invoke(home);
      double z = (double) homeZMethod.invoke(home);
      float yaw = (float) homeYawMethod.invoke(home);
      float pitch = (float) homePitchMethod.invoke(home);
      return new double[]{x, y, z, yaw, pitch};
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Gets the faction home cooldown remaining.
   * Uses HyperFactions API if available, otherwise returns 0.
   */
  public static int getFactionHomeCooldownRemaining(@NotNull UUID playerUuid) {
    if (!available) return 0;
    if (apiGetFactionHomeCooldownMethod != null) {
      try {
        return (int) apiGetFactionHomeCooldownMethod.invoke(null, playerUuid);
      } catch (Exception e) { /* fall through */ }
    }
    return 0;
  }

  // === Zone Flag Methods ===

  /**
   * Checks if a zone flag allows an action at the given location.
   * Returns true (fail-open) if HyperFactions is absent, the zone flag API
   * is unavailable (older version), or the location is not in a zone.
   *
   * @param world    the world name
   * @param x        the x coordinate
   * @param z        the z coordinate
   * @param flagName the zone flag name (use FLAG_HOMES, FLAG_WARPS, FLAG_KITS)
   * @return true if allowed
   */
  public static boolean isZoneFlagAllowed(@NotNull String world, double x, double z,
                                          @NotNull String flagName) {
    if (!available || isZoneFlagAllowedMethod == null) return true;
    try {
      return (boolean) isZoneFlagAllowedMethod.invoke(null, world, x, z, flagName);
    } catch (Exception e) {
      return true;
    }
  }
}
