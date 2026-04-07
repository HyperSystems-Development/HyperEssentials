package com.hyperessentials.api;

import com.hyperessentials.HyperEssentials;
import com.hyperessentials.api.events.EventBus;
import com.hyperessentials.data.Home;
import com.hyperessentials.data.Location;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.data.Warp;
import com.hyperessentials.module.homes.HomeManager;
import com.hyperessentials.module.homes.HomesModule;
import com.hyperessentials.module.kits.KitManager;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.module.kits.data.Kit;
import com.hyperessentials.module.moderation.ModerationManager;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hyperessentials.module.spawns.SpawnsModule;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.module.utility.UtilityManager;
import com.hyperessentials.module.utility.UtilityModule;
import com.hyperessentials.module.warps.WarpManager;
import com.hyperessentials.module.warps.WarpsModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Public API for HyperEssentials.
 * External plugins can use this to query and modify essentials data
 * without depending on internal module implementations.
 *
 * <p>All query methods that return collections return empty collections (never null)
 * when the backing module is unavailable or disabled. Nullable return types indicate
 * that the data may not exist. Mutating methods return {@code boolean} where
 * {@code false} means the module was unavailable or the operation failed.</p>
 *
 * <p>For advanced use cases, the manager accessor methods at the bottom of this class
 * expose the underlying managers directly.</p>
 */
public final class HyperEssentialsAPI {

  private static HyperEssentials instance;

  private HyperEssentialsAPI() {}

  /**
   * Sets the backing HyperEssentials instance. Called internally on plugin enable/disable.
   */
  public static void setInstance(@Nullable HyperEssentials instance) {
    HyperEssentialsAPI.instance = instance;
  }

  /**
   * Returns the backing HyperEssentials instance.
   *
   * @throws IllegalStateException if HyperEssentials is not initialized
   */
  @NotNull
  public static HyperEssentials getInstance() {
    if (instance == null) throw new IllegalStateException("HyperEssentials is not initialized");
    return instance;
  }

  /**
   * Returns whether HyperEssentials is initialized and available.
   */
  public static boolean isAvailable() {
    return instance != null;
  }

  // ========================================================================
  // Home API
  // ========================================================================

  /**
   * Gets all homes for a player.
   *
   * @param uuid the player UUID
   * @return the player's homes, or an empty collection if the module is unavailable
   */
  @NotNull
  public static Collection<Home> getHomes(@NotNull UUID uuid) {
    HomeManager hm = homeManager();
    return hm != null ? hm.getHomes(uuid) : Collections.emptyList();
  }

  /**
   * Gets a specific home by name.
   *
   * @param uuid the player UUID
   * @param name the home name
   * @return the home, or null if not found or module unavailable
   */
  @Nullable
  public static Home getHome(@NotNull UUID uuid, @NotNull String name) {
    HomeManager hm = homeManager();
    return hm != null ? hm.getHome(uuid, name) : null;
  }

  /**
   * Sets (creates or overwrites) a home for a player.
   *
   * @param uuid the player UUID
   * @param home the home to set
   * @return true if the home was set, false if the module is unavailable or the player is at their limit
   */
  public static boolean setHome(@NotNull UUID uuid, @NotNull Home home) {
    HomeManager hm = homeManager();
    return hm != null && hm.setHome(uuid, home);
  }

  /**
   * Deletes a home by name.
   *
   * @param uuid the player UUID
   * @param name the home name
   * @return true if the home was deleted, false if not found or module unavailable
   */
  public static boolean deleteHome(@NotNull UUID uuid, @NotNull String name) {
    HomeManager hm = homeManager();
    return hm != null && hm.deleteHome(uuid, name);
  }

  /**
   * Gets the number of homes a player has.
   *
   * @param uuid the player UUID
   * @return the home count, or 0 if the module is unavailable
   */
  public static int getHomeCount(@NotNull UUID uuid) {
    HomeManager hm = homeManager();
    return hm != null ? hm.getHomeCount(uuid) : 0;
  }

  /**
   * Gets the home limit for a player (based on permissions and config).
   *
   * @param uuid the player UUID
   * @return the home limit (-1 = unlimited), or 0 if the module is unavailable
   */
  public static int getHomeLimit(@NotNull UUID uuid) {
    HomeManager hm = homeManager();
    return hm != null ? hm.getHomeLimit(uuid) : 0;
  }

  /**
   * Checks whether a player is at their home limit.
   *
   * @param uuid the player UUID
   * @return true if the player cannot create more homes, false if unlimited or module unavailable
   */
  public static boolean isAtHomeLimit(@NotNull UUID uuid) {
    HomeManager hm = homeManager();
    return hm != null && hm.isAtLimit(uuid);
  }

  /**
   * Gets the name of a player's default home.
   *
   * @param uuid the player UUID
   * @return the default home name, or null if none set or module unavailable
   */
  @Nullable
  public static String getDefaultHome(@NotNull UUID uuid) {
    HomeManager hm = homeManager();
    return hm != null ? hm.getDefaultHome(uuid) : null;
  }

  /**
   * Sets a player's default home.
   *
   * @param uuid the player UUID
   * @param name the home name to set as default (null to clear)
   * @return true if set, false if the module is unavailable
   */
  public static boolean setDefaultHome(@NotNull UUID uuid, @Nullable String name) {
    HomeManager hm = homeManager();
    if (hm == null) return false;
    hm.setDefaultHome(uuid, name);
    return true;
  }

  /**
   * Shares a home with another player.
   *
   * @param ownerUuid the home owner's UUID
   * @param homeName  the home name
   * @param targetUuid the player to share with
   * @return true if shared, false if the module is unavailable
   */
  public static boolean shareHome(@NotNull UUID ownerUuid, @NotNull String homeName, @NotNull UUID targetUuid) {
    HomeManager hm = homeManager();
    if (hm == null) return false;
    hm.shareHome(ownerUuid, homeName, targetUuid);
    return true;
  }

  /**
   * Revokes a home share from another player.
   *
   * @param ownerUuid the home owner's UUID
   * @param homeName  the home name
   * @param targetUuid the player to unshare from
   * @return true if unshared, false if the module is unavailable
   */
  public static boolean unshareHome(@NotNull UUID ownerUuid, @NotNull String homeName, @NotNull UUID targetUuid) {
    HomeManager hm = homeManager();
    if (hm == null) return false;
    hm.unshareHome(ownerUuid, homeName, targetUuid);
    return true;
  }

  /**
   * Gets the set of player UUIDs a home is shared with.
   *
   * @param ownerUuid the home owner's UUID
   * @param homeName  the home name
   * @return the set of shared player UUIDs, or an empty set if unavailable
   */
  @NotNull
  public static Set<UUID> getSharedWith(@NotNull UUID ownerUuid, @NotNull String homeName) {
    HomeManager hm = homeManager();
    return hm != null ? hm.getSharedWith(ownerUuid, homeName) : Set.of();
  }

  /**
   * Gets all homes shared TO a player (from other players).
   *
   * @param uuid the viewer's UUID
   * @return list of shared homes, or an empty list if unavailable
   */
  @NotNull
  public static List<HomeManager.SharedHome> getSharedHomes(@NotNull UUID uuid) {
    HomeManager hm = homeManager();
    return hm != null ? hm.getSharedHomes(uuid) : List.of();
  }

  // ========================================================================
  // Warp API
  // ========================================================================

  /**
   * Gets a warp by name.
   *
   * @param name the warp name (case-insensitive)
   * @return the warp, or null if not found or module unavailable
   */
  @Nullable
  public static Warp getWarp(@NotNull String name) {
    WarpManager wm = warpManager();
    return wm != null ? wm.getWarp(name) : null;
  }

  /**
   * Gets all warps.
   *
   * @return all warps, or an empty collection if unavailable
   */
  @NotNull
  public static Collection<Warp> getAllWarps() {
    WarpManager wm = warpManager();
    return wm != null ? wm.getAllWarps() : Collections.emptyList();
  }

  /**
   * Gets warps accessible to a player (respecting per-warp permissions).
   *
   * @param uuid the player UUID
   * @return accessible warps, or an empty list if unavailable
   */
  @NotNull
  public static List<Warp> getAccessibleWarps(@NotNull UUID uuid) {
    WarpManager wm = warpManager();
    return wm != null ? wm.getAccessibleWarps(uuid) : Collections.emptyList();
  }

  /**
   * Creates or updates a warp.
   *
   * @param warp the warp to set
   * @return true if set, false if the module is unavailable
   */
  public static boolean setWarp(@NotNull Warp warp) {
    WarpManager wm = warpManager();
    if (wm == null) return false;
    wm.setWarp(warp);
    return true;
  }

  /**
   * Deletes a warp by name.
   *
   * @param name the warp name
   * @return true if the warp was deleted, false if not found or module unavailable
   */
  public static boolean deleteWarp(@NotNull String name) {
    WarpManager wm = warpManager();
    return wm != null && wm.deleteWarp(name);
  }

  /**
   * Gets the total number of warps.
   *
   * @return the warp count, or 0 if the module is unavailable
   */
  public static int getWarpCount() {
    WarpManager wm = warpManager();
    return wm != null ? wm.getWarpCount() : 0;
  }

  /**
   * Gets all warp categories.
   *
   * @return category names, or an empty set if unavailable
   */
  @NotNull
  public static Set<String> getWarpCategories() {
    WarpManager wm = warpManager();
    return wm != null ? wm.getCategories() : Set.of();
  }

  // ========================================================================
  // Spawn API
  // ========================================================================

  /**
   * Gets the spawn for a specific world by UUID.
   *
   * @param worldUuid the world UUID string
   * @return the spawn, or null if not found or module unavailable
   */
  @Nullable
  public static Spawn getSpawnForWorld(@NotNull String worldUuid) {
    SpawnManager sm = spawnManager();
    return sm != null ? sm.getSpawnForWorld(worldUuid) : null;
  }

  /**
   * Gets the global spawn point.
   *
   * @return the global spawn, or null if none configured or module unavailable
   */
  @Nullable
  public static Spawn getGlobalSpawn() {
    SpawnManager sm = spawnManager();
    return sm != null ? sm.getGlobalSpawn() : null;
  }

  /**
   * Gets all configured spawns.
   *
   * @return all spawns, or an empty collection if unavailable
   */
  @NotNull
  public static Collection<Spawn> getAllSpawns() {
    SpawnManager sm = spawnManager();
    return sm != null ? sm.getAllSpawns() : Collections.emptyList();
  }

  /**
   * Sets (creates or updates) a spawn for a world.
   * If the spawn is marked as global, any previous global spawn is demoted.
   *
   * @param spawn the spawn to set
   * @return true if set, false if the module is unavailable
   */
  public static boolean setSpawn(@NotNull Spawn spawn) {
    SpawnManager sm = spawnManager();
    if (sm == null) return false;
    sm.setSpawn(spawn);
    return true;
  }

  /**
   * Deletes the spawn for a world.
   *
   * @param worldUuid the world UUID string
   * @return true if deleted, false if not found or module unavailable
   */
  public static boolean deleteSpawn(@NotNull String worldUuid) {
    SpawnManager sm = spawnManager();
    return sm != null && sm.deleteSpawn(worldUuid);
  }

  /**
   * Gets the total number of configured spawns.
   *
   * @return the spawn count, or 0 if the module is unavailable
   */
  public static int getSpawnCount() {
    SpawnManager sm = spawnManager();
    return sm != null ? sm.getSpawnCount() : 0;
  }

  // ========================================================================
  // Kit API
  // ========================================================================

  /**
   * Gets a kit by name.
   *
   * @param name the kit name (case-insensitive)
   * @return the kit, or null if not found or module unavailable
   */
  @Nullable
  public static Kit getKit(@NotNull String name) {
    KitManager km = kitManager();
    return km != null ? km.getKit(name) : null;
  }

  /**
   * Gets all defined kits.
   *
   * @return all kits, or an empty collection if unavailable
   */
  @NotNull
  public static Collection<Kit> getAllKits() {
    KitManager km = kitManager();
    return km != null ? km.getAllKits() : Collections.emptyList();
  }

  /**
   * Gets kits available to a player (based on permission checks).
   *
   * @param uuid the player UUID
   * @return available kits, or an empty list if unavailable
   */
  @NotNull
  public static List<Kit> getAvailableKits(@NotNull UUID uuid) {
    KitManager km = kitManager();
    return km != null ? km.getAvailableKits(uuid) : Collections.emptyList();
  }

  /**
   * Checks whether a player is on cooldown for a kit.
   *
   * @param uuid    the player UUID
   * @param kitName the kit name
   * @return true if on cooldown, false if not or module unavailable
   */
  public static boolean isOnKitCooldown(@NotNull UUID uuid, @NotNull String kitName) {
    KitManager km = kitManager();
    return km != null && km.isOnCooldown(uuid, kitName);
  }

  /**
   * Gets the remaining cooldown for a kit in milliseconds.
   *
   * @param uuid    the player UUID
   * @param kitName the kit name
   * @return remaining cooldown in ms, or 0 if no cooldown or module unavailable
   */
  public static long getRemainingKitCooldown(@NotNull UUID uuid, @NotNull String kitName) {
    KitManager km = kitManager();
    return km != null ? km.getRemainingCooldown(uuid, kitName) : 0L;
  }

  /**
   * Checks whether a player has already claimed a one-time kit.
   *
   * @param uuid    the player UUID
   * @param kitName the kit name
   * @return true if already claimed, false if not or module unavailable
   */
  public static boolean hasClaimedOneTimeKit(@NotNull UUID uuid, @NotNull String kitName) {
    KitManager km = kitManager();
    return km != null && km.hasClaimedOneTimeKit(uuid, kitName);
  }

  /**
   * Deletes a kit by name.
   *
   * @param name the kit name
   * @return true if deleted, false if not found or module unavailable
   */
  public static boolean deleteKit(@NotNull String name) {
    KitManager km = kitManager();
    return km != null && km.deleteKit(name);
  }

  // ========================================================================
  // Moderation API
  // ========================================================================

  /**
   * Checks whether a player is currently banned.
   *
   * @param uuid the player UUID
   * @return true if banned, false if not or module unavailable
   */
  public static boolean isBanned(@NotNull UUID uuid) {
    ModerationManager mm = moderationManager();
    return mm != null && mm.isBanned(uuid);
  }

  /**
   * Checks whether a player is currently muted.
   *
   * @param uuid the player UUID
   * @return true if muted, false if not or module unavailable
   */
  public static boolean isMuted(@NotNull UUID uuid) {
    ModerationManager mm = moderationManager();
    return mm != null && mm.isMuted(uuid);
  }

  /**
   * Gets the active ban for a player.
   *
   * @param uuid the player UUID
   * @return the active ban punishment, or null if not banned or module unavailable
   */
  @Nullable
  public static Punishment getActiveBan(@NotNull UUID uuid) {
    ModerationManager mm = moderationManager();
    return mm != null ? mm.getActiveBan(uuid) : null;
  }

  /**
   * Gets the full punishment history for a player.
   *
   * @param uuid the player UUID
   * @return the punishment history, or an empty list if unavailable
   */
  @NotNull
  public static List<Punishment> getPunishmentHistory(@NotNull UUID uuid) {
    ModerationManager mm = moderationManager();
    return mm != null ? mm.getHistory(uuid) : List.of();
  }

  /**
   * Gets all punishments across all players.
   * This is an expensive operation that scans all player data files.
   *
   * @param activeOnly if true, only returns currently effective punishments
   * @return all punishments, or an empty list if unavailable
   */
  @NotNull
  public static List<Punishment> getAllPunishments(boolean activeOnly) {
    ModerationManager mm = moderationManager();
    return mm != null ? mm.getAllPunishments(activeOnly) : List.of();
  }

  /**
   * Checks whether an IP address is banned.
   *
   * @param ip the IP address
   * @return true if IP-banned, false if not or module unavailable
   */
  public static boolean isIpBanned(@NotNull String ip) {
    ModerationManager mm = moderationManager();
    return mm != null && mm.isIpBanned(ip);
  }

  // ========================================================================
  // Utility API
  // ========================================================================

  /**
   * Checks whether a player has fly mode enabled.
   *
   * @param uuid the player UUID
   * @return true if flying, false if not or module unavailable
   */
  public static boolean isFlying(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null && um.isFlying(uuid);
  }

  /**
   * Checks whether a player has god mode enabled.
   *
   * @param uuid the player UUID
   * @return true if in god mode, false if not or module unavailable
   */
  public static boolean isGod(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null && um.isGod(uuid);
  }

  /**
   * Checks whether a player is AFK.
   *
   * @param uuid the player UUID
   * @return true if AFK, false if not or module unavailable
   */
  public static boolean isAfk(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null && um.isAfk(uuid);
  }

  /**
   * Checks whether a player has infinite stamina enabled.
   *
   * @param uuid the player UUID
   * @return true if infinite stamina is active, false if not or module unavailable
   */
  public static boolean isInfiniteStamina(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null && um.isInfiniteStamina(uuid);
  }

  /**
   * Gets the first join time for a player.
   *
   * @param uuid the player UUID
   * @return the first join instant, or null if unknown or module unavailable
   */
  @Nullable
  public static Instant getFirstJoin(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null ? um.getFirstJoin(uuid) : null;
  }

  /**
   * Gets the total playtime for a player, including the current session.
   *
   * @param uuid the player UUID
   * @return total playtime in milliseconds, or 0 if module unavailable
   */
  public static long getTotalPlaytimeMs(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null ? um.getTotalPlaytimeMs(uuid) : 0L;
  }

  /**
   * Gets the current session start time for a player.
   *
   * @param uuid the player UUID
   * @return the session start instant, or null if not online or module unavailable
   */
  @Nullable
  public static Instant getSessionStart(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null ? um.getSessionStart(uuid) : null;
  }

  // ========================================================================
  // TPA / Back API
  // ========================================================================

  /**
   * Checks whether a player is accepting TPA requests.
   *
   * @param uuid the player UUID
   * @return true if accepting (or if the module is unavailable — fail-open)
   */
  public static boolean isAcceptingTpa(@NotNull UUID uuid) {
    TpaManager tm = tpaManager();
    return tm == null || tm.isAcceptingRequests(uuid);
  }

  /**
   * Saves a back location for a player (used by external teleport systems).
   *
   * @param uuid     the player UUID
   * @param location the location to save
   * @param source   a source identifier (e.g., "warp", "home", "tpa")
   */
  public static void saveBackLocation(@NotNull UUID uuid, @NotNull Location location, @NotNull String source) {
    BackManager bm = backManager();
    if (bm != null) {
      bm.saveBackLocation(uuid, location, source);
    }
  }

  /**
   * Checks whether a player has any back history entries.
   *
   * @param uuid the player UUID
   * @return true if back history exists, false if empty or module unavailable
   */
  public static boolean hasBackHistory(@NotNull UUID uuid) {
    BackManager bm = backManager();
    return bm != null && bm.hasBackHistory(uuid);
  }

  /**
   * Gets the number of entries in a player's back history.
   *
   * @param uuid the player UUID
   * @return the back history size, or 0 if the module is unavailable
   */
  public static int getBackHistorySize(@NotNull UUID uuid) {
    BackManager bm = backManager();
    return bm != null ? bm.getHistorySize(uuid) : 0;
  }

  /**
   * Clears a player's entire back history.
   *
   * @param uuid the player UUID
   */
  public static void clearBackHistory(@NotNull UUID uuid) {
    BackManager bm = backManager();
    if (bm != null) {
      bm.clearHistory(uuid);
    }
  }

  // ========================================================================
  // Event System
  // ========================================================================

  /**
   * Registers an event listener for HyperEssentials events.
   *
   * @param eventClass the event class to listen for
   * @param listener   the listener callback
   * @param <T>        the event type
   */
  public static <T> void registerEventListener(@NotNull Class<T> eventClass, @NotNull Consumer<T> listener) {
    EventBus.register(eventClass, listener);
  }

  /**
   * Unregisters an event listener.
   *
   * @param eventClass the event class
   * @param listener   the listener callback to remove
   * @param <T>        the event type
   */
  public static <T> void unregisterEventListener(@NotNull Class<T> eventClass, @NotNull Consumer<T> listener) {
    EventBus.unregister(eventClass, listener);
  }

  // ========================================================================
  // Manager Accessors (for advanced use)
  // ========================================================================

  /**
   * Gets the HomeManager for direct access to home operations.
   *
   * @return the HomeManager, or null if the homes module is unavailable
   */
  @Nullable
  public static HomeManager getHomeManager() {
    return homeManager();
  }

  /**
   * Gets the WarpManager for direct access to warp operations.
   *
   * @return the WarpManager, or null if the warps module is unavailable
   */
  @Nullable
  public static WarpManager getWarpManager() {
    return warpManager();
  }

  /**
   * Gets the SpawnManager for direct access to spawn operations.
   *
   * @return the SpawnManager, or null if the spawns module is unavailable
   */
  @Nullable
  public static SpawnManager getSpawnManager() {
    return spawnManager();
  }

  /**
   * Gets the KitManager for direct access to kit operations.
   *
   * @return the KitManager, or null if the kits module is unavailable
   */
  @Nullable
  public static KitManager getKitManager() {
    return kitManager();
  }

  /**
   * Gets the ModerationManager for direct access to moderation operations.
   *
   * @return the ModerationManager, or null if the moderation module is unavailable
   */
  @Nullable
  public static ModerationManager getModerationManager() {
    return moderationManager();
  }

  /**
   * Gets the UtilityManager for direct access to utility state operations.
   *
   * @return the UtilityManager, or null if the utility module is unavailable
   */
  @Nullable
  public static UtilityManager getUtilityManager() {
    return utilityManager();
  }

  /**
   * Gets the TpaManager for direct access to TPA request operations.
   *
   * @return the TpaManager, or null if the teleport module is unavailable
   */
  @Nullable
  public static TpaManager getTpaManager() {
    return tpaManager();
  }

  /**
   * Gets the BackManager for direct access to back history operations.
   *
   * @return the BackManager, or null if the teleport module is unavailable
   */
  @Nullable
  public static BackManager getBackManager() {
    return backManager();
  }

  // ========================================================================
  // Private Helpers
  // ========================================================================

  @Nullable
  private static HomeManager homeManager() {
    if (!isAvailable()) return null;
    HomesModule m = instance.getHomesModule();
    return (m != null && m.isEnabled()) ? m.getHomeManager() : null;
  }

  @Nullable
  private static WarpManager warpManager() {
    if (!isAvailable()) return null;
    WarpsModule m = instance.getWarpsModule();
    return (m != null && m.isEnabled()) ? m.getWarpManager() : null;
  }

  @Nullable
  private static SpawnManager spawnManager() {
    if (!isAvailable()) return null;
    SpawnsModule m = instance.getSpawnsModule();
    return (m != null && m.isEnabled()) ? m.getSpawnManager() : null;
  }

  @Nullable
  private static KitManager kitManager() {
    if (!isAvailable()) return null;
    KitsModule m = instance.getKitsModule();
    return (m != null && m.isEnabled()) ? m.getKitManager() : null;
  }

  @Nullable
  private static ModerationManager moderationManager() {
    if (!isAvailable()) return null;
    ModerationModule m = instance.getModerationModule();
    return (m != null && m.isEnabled()) ? m.getModerationManager() : null;
  }

  @Nullable
  private static UtilityManager utilityManager() {
    if (!isAvailable()) return null;
    UtilityModule m = instance.getUtilityModule();
    return (m != null && m.isEnabled()) ? m.getUtilityManager() : null;
  }

  @Nullable
  private static TpaManager tpaManager() {
    if (!isAvailable()) return null;
    TeleportModule m = instance.getTeleportModule();
    return (m != null && m.isEnabled()) ? m.getTpaManager() : null;
  }

  @Nullable
  private static BackManager backManager() {
    if (!isAvailable()) return null;
    TeleportModule m = instance.getTeleportModule();
    return (m != null && m.isEnabled()) ? m.getBackManager() : null;
  }
}
