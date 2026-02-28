package com.hyperessentials.integration;

import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.UUID;

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

      Class<?> factionClass = Class.forName("com.hyperfactions.data.Faction");
      factionNameMethod = factionClass.getMethod("name");

      Class<?> relationTypeClass = Class.forName("com.hyperfactions.data.RelationType");
      relationTypeNameMethod = relationTypeClass.getMethod("name");

      available = true;
      Logger.info("[Integration] HyperFactions integration initialized successfully");

    } catch (ClassNotFoundException e) {
      Logger.info("[Integration] HyperFactions not found - territory features disabled");
    } catch (Exception e) {
      Logger.warn("[Integration] Failed to initialize HyperFactions integration: %s", e.getMessage());
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
}
