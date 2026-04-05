package com.hyperessentials.importer;

import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves world references (by name or UUID) to HyperEssentials world data.
 *
 * <p>Built once per import run, caches results for repeated lookups.
 * Tracks unresolved worlds for validation warnings.
 */
public class WorldResolver {

  /** Resolved world info with both name and UUID. */
  public record ResolvedWorld(@NotNull String worldName, @NotNull String worldUuid) {}

  private final Map<String, ResolvedWorld> nameCache = new HashMap<>();
  private final Map<String, ResolvedWorld> uuidCache = new HashMap<>();
  private final List<String> unresolvedWorlds = new ArrayList<>();
  private boolean initialized = false;

  /**
   * Initializes the resolver by scanning all loaded worlds.
   * Safe to call multiple times (no-op after first call).
   */
  public void init() {
    if (initialized) return;
    initialized = true;

    try {
      Universe universe = Universe.get();
      if (universe == null) {
        Logger.warn("[Import] Universe not available, world resolution will be limited");
        return;
      }

      Map<String, World> worlds = universe.getWorlds();
      if (worlds == null || worlds.isEmpty()) {
        Logger.warn("[Import] No worlds loaded, world resolution will be limited");
        return;
      }

      for (World world : worlds.values()) {
        String name = world.getName();
        UUID uuid = null;
        try {
          uuid = world.getWorldConfig().getUuid();
        } catch (Exception ignored) {}
        if (name != null && uuid != null) {
          ResolvedWorld resolved = new ResolvedWorld(name, uuid.toString());
          nameCache.put(name.toLowerCase(), resolved);
          uuidCache.put(uuid.toString(), resolved);
        }
      }

      Logger.info("[Import] WorldResolver initialized with %d world(s)", nameCache.size());
    } catch (Exception e) {
      Logger.warn("[Import] Failed to initialize WorldResolver: %s", e.getMessage());
    }
  }

  /**
   * Resolves a world by display name (case-insensitive).
   * Handles "default" alias by returning the first available world.
   *
   * @param worldName the world name to resolve
   * @return resolved world info, or null if not found
   */
  @Nullable
  public ResolvedWorld resolveByName(@NotNull String worldName) {
    init();

    String key = worldName.toLowerCase();

    // Direct lookup
    ResolvedWorld resolved = nameCache.get(key);
    if (resolved != null) return resolved;

    // "default" alias — try common Hytale default world names
    if ("default".equals(key) || "world".equals(key)) {
      try {
        Universe universe = Universe.get();
        if (universe != null) {
          World defaultWorld = universe.getDefaultWorld();
          if (defaultWorld != null && defaultWorld.getName() != null) {
            UUID defUuid = null;
            try {
              defUuid = defaultWorld.getWorldConfig().getUuid();
            } catch (Exception ignored) {}
            if (defUuid != null) {
              resolved = new ResolvedWorld(defaultWorld.getName(), defUuid.toString());
              nameCache.put(key, resolved);
              return resolved;
            }
          }
        }
      } catch (Exception ignored) {}
    }

    // Track as unresolved
    if (!unresolvedWorlds.contains(worldName)) {
      unresolvedWorlds.add(worldName);
    }
    return null;
  }

  /**
   * Resolves a world by UUID string.
   *
   * @param worldUuidStr the world UUID string
   * @return resolved world info, or null if not found
   */
  @Nullable
  public ResolvedWorld resolveByUuid(@NotNull String worldUuidStr) {
    init();

    ResolvedWorld resolved = uuidCache.get(worldUuidStr);
    if (resolved != null) return resolved;

    // Try live lookup
    try {
      UUID uuid = UUID.fromString(worldUuidStr);
      Universe universe = Universe.get();
      if (universe != null) {
        World world = universe.getWorld(uuid);
        if (world != null && world.getName() != null) {
          resolved = new ResolvedWorld(world.getName(), worldUuidStr);
          uuidCache.put(worldUuidStr, resolved);
          nameCache.put(world.getName().toLowerCase(), resolved);
          return resolved;
        }
      }
    } catch (IllegalArgumentException ignored) {
      // Invalid UUID format
    } catch (Exception ignored) {}

    // Track as unresolved
    String label = "UUID:" + worldUuidStr;
    if (!unresolvedWorlds.contains(label)) {
      unresolvedWorlds.add(label);
    }
    return null;
  }

  /**
   * Returns a list of world references that could not be resolved.
   */
  @NotNull
  public List<String> getUnresolvedWorlds() {
    return List.copyOf(unresolvedWorlds);
  }

  /**
   * Returns whether any worlds could not be resolved.
   */
  public boolean hasUnresolvedWorlds() {
    return !unresolvedWorlds.isEmpty();
  }

  /**
   * Returns the number of loaded worlds available for resolution.
   */
  public int getLoadedWorldCount() {
    init();
    return nameCache.size();
  }
}
