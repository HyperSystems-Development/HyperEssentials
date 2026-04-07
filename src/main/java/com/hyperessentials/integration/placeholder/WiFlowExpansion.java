package com.hyperessentials.integration.placeholder;

import com.hyperessentials.BuildInfo;
import com.hyperessentials.HyperEssentials;
import com.hyperessentials.data.PlayerData;
import com.hyperessentials.module.homes.HomeManager;
import com.hyperessentials.module.homes.HomesModule;
import com.hyperessentials.module.kits.KitManager;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.module.moderation.ModerationManager;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.module.moderation.VanishManager;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.module.moderation.data.PunishmentType;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hyperessentials.module.spawns.SpawnsModule;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.module.utility.UtilityManager;
import com.hyperessentials.module.utility.UtilityModule;
import com.hyperessentials.module.warps.WarpManager;
import com.hyperessentials.module.warps.WarpsModule;
import com.wiflow.placeholderapi.context.PlaceholderContext;
import com.wiflow.placeholderapi.expansion.PlaceholderExpansion;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * WiFlow PlaceholderAPI expansion for HyperEssentials.
 *
 * <p>Exposes essentials data as WiFlow placeholders ({@code {essentials_xxx}}) for use
 * by scoreboards, holograms, chat formatting, and other WiFlow consumers.
 *
 * <p>Homes:
 *   {essentials_home_count}     - Number of player's homes
 *   {essentials_home_limit}     - Player's max homes allowed
 *   {essentials_home_default}   - Name of player's default home
 *   {essentials_has_homes}      - Whether player has any homes (true/false)
 *
 * <p>Kits:
 *   {essentials_kit_count}           - Total number of kits defined
 *   {essentials_kit_available_count} - Number of kits available to the player
 *   {essentials_kit_cooldown_<name>} - Remaining cooldown seconds for a kit
 *
 * <p>Moderation:
 *   {essentials_is_banned}         - Whether player is banned (true/false)
 *   {essentials_is_muted}          - Whether player is muted (true/false)
 *   {essentials_ban_reason}        - Reason for active ban
 *   {essentials_mute_reason}       - Reason for active mute
 *   {essentials_ban_expires}       - Ban expiry date or "permanent"
 *   {essentials_mute_expires}      - Mute expiry date or "permanent"
 *   {essentials_punishment_count}  - Total number of punishments
 *   {essentials_warn_count}        - Number of warnings
 *
 * <p>Utility:
 *   {essentials_is_afk}              - Whether player is AFK (true/false)
 *   {essentials_is_flying}           - Whether player is flying (true/false)
 *   {essentials_is_god}              - Whether player has god mode (true/false)
 *   {essentials_is_infinite_stamina} - Whether player has infinite stamina (true/false)
 *   {essentials_is_vanished}         - Whether player is vanished (true/false)
 *   {essentials_playtime}            - Total playtime in milliseconds
 *   {essentials_playtime_formatted}  - Total playtime formatted (e.g., 3d 2h 15m)
 *   {essentials_session_time}        - Current session time in milliseconds
 *   {essentials_session_time_formatted} - Current session time formatted
 *   {essentials_first_join}             - First join timestamp (ISO)
 *   {essentials_first_join_formatted}   - First join date (yyyy-MM-dd HH:mm)
 *   {essentials_last_join}              - Last join timestamp (ISO)
 *   {essentials_last_join_formatted}    - Last join date (yyyy-MM-dd HH:mm)
 *
 * <p>Warps/Spawns/TPA:
 *   {essentials_warp_count}            - Total number of warps
 *   {essentials_warp_accessible_count} - Number of warps accessible to the player
 *   {essentials_spawn_count}           - Total number of spawns
 *   {essentials_is_accepting_tpa}      - Whether player is accepting TPA requests (true/false)
 *   {essentials_tpa_pending_count}     - Number of incoming TPA requests
 */
public class WiFlowExpansion extends PlaceholderExpansion {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

  private final HyperEssentials plugin;

  /** Creates a new WiFlowExpansion. */
  public WiFlowExpansion(@NotNull HyperEssentials plugin) {
    this.plugin = plugin;
  }

  /** Returns the identifier. */
  @Override
  public String getIdentifier() {
    return "essentials";
  }

  /** Returns the author. */
  @Override
  public String getAuthor() {
    return "HyperSystems-Development";
  }

  /** Returns the version. */
  @Override
  public String getVersion() {
    return BuildInfo.VERSION;
  }

  /** Returns the name. */
  @Override
  public String getName() {
    return "HyperEssentials";
  }

  @Override
  public String getDescription() {
    return "HyperEssentials data placeholders: homes, kits, moderation, utility, warps, spawns, TPA";
  }

  /** Persist across reloads. */
  @Override
  public boolean persist() {
    return true;
  }

  /** Returns the placeholders. */
  @Override
  public List<String> getPlaceholders() {
    return List.of(
        "home_count", "home_limit", "home_default", "has_homes",
        "kit_count", "kit_available_count",
        "is_banned", "is_muted", "ban_reason", "mute_reason",
        "ban_expires", "mute_expires", "punishment_count", "warn_count",
        "is_afk", "is_flying", "is_god", "is_infinite_stamina", "is_vanished",
        "playtime", "playtime_formatted", "session_time", "session_time_formatted",
        "first_join", "first_join_formatted", "last_join", "last_join_formatted",
        "warp_count", "warp_accessible_count", "spawn_count",
        "is_accepting_tpa", "tpa_pending_count"
    );
  }

  /** Called when a placeholder is requested. */
  @Override
  @Nullable
  public String onPlaceholderRequest(@NotNull PlaceholderContext context, @NotNull String params) {
    UUID uuid = context.getPlayerUuid();
    if (uuid == null) {
      return null;
    }

    // Handle dynamic kit cooldown placeholders before the switch
    String lower = params.toLowerCase();
    if (lower.startsWith("kit_cooldown_")) {
      String kitName = lower.substring("kit_cooldown_".length());
      KitManager km = kitManager();
      if (km == null) return "";
      long remainingMs = km.getRemainingCooldown(uuid, kitName);
      return String.valueOf(remainingMs / 1000);
    }

    return switch (lower) {
      // Homes
      case "home_count"    -> homeCount(uuid);
      case "home_limit"    -> homeLimit(uuid);
      case "home_default"  -> homeDefault(uuid);
      case "has_homes"     -> hasHomes(uuid);

      // Kits
      case "kit_count"           -> kitCount();
      case "kit_available_count" -> kitAvailableCount(uuid);

      // Moderation
      case "is_banned"         -> isBanned(uuid);
      case "is_muted"          -> isMuted(uuid);
      case "ban_reason"        -> banReason(uuid);
      case "mute_reason"       -> muteReason(uuid);
      case "ban_expires"       -> banExpires(uuid);
      case "mute_expires"      -> muteExpires(uuid);
      case "punishment_count"  -> punishmentCount(uuid);
      case "warn_count"        -> warnCount(uuid);

      // Utility
      case "is_afk"              -> isAfk(uuid);
      case "is_flying"           -> isFlying(uuid);
      case "is_god"              -> isGod(uuid);
      case "is_infinite_stamina" -> isInfiniteStamina(uuid);
      case "is_vanished"         -> isVanished(uuid);
      case "playtime"            -> playtime(uuid);
      case "playtime_formatted"  -> playtimeFormatted(uuid);
      case "session_time"        -> sessionTime(uuid);
      case "session_time_formatted" -> sessionTimeFormatted(uuid);
      case "first_join"             -> firstJoin(uuid);
      case "first_join_formatted"   -> firstJoinFormatted(uuid);
      case "last_join"              -> lastJoin(uuid);
      case "last_join_formatted"    -> lastJoinFormatted(uuid);

      // Warps / Spawns / TPA
      case "warp_count"            -> warpCount();
      case "warp_accessible_count" -> warpAccessibleCount(uuid);
      case "spawn_count"           -> spawnCount();
      case "is_accepting_tpa"      -> isAcceptingTpa(uuid);
      case "tpa_pending_count"     -> tpaPendingCount(uuid);

      default -> null; // Unknown placeholder - preserve original text
    };
  }

  // ========== Homes ==========

  private String homeCount(@NotNull UUID uuid) {
    HomeManager hm = homeManager();
    return hm != null ? String.valueOf(hm.getHomeCount(uuid)) : "0";
  }

  private String homeLimit(@NotNull UUID uuid) {
    HomeManager hm = homeManager();
    return hm != null ? String.valueOf(hm.getHomeLimit(uuid)) : "0";
  }

  private String homeDefault(@NotNull UUID uuid) {
    HomeManager hm = homeManager();
    if (hm == null) return "";
    String def = hm.getDefaultHome(uuid);
    return def != null ? def : "";
  }

  private String hasHomes(@NotNull UUID uuid) {
    HomeManager hm = homeManager();
    return hm != null ? String.valueOf(hm.getHomeCount(uuid) > 0) : "false";
  }

  // ========== Kits ==========

  private String kitCount() {
    KitManager km = kitManager();
    return km != null ? String.valueOf(km.getAllKits().size()) : "0";
  }

  private String kitAvailableCount(@NotNull UUID uuid) {
    KitManager km = kitManager();
    return km != null ? String.valueOf(km.getAvailableKits(uuid).size()) : "0";
  }

  // ========== Moderation ==========

  private String isBanned(@NotNull UUID uuid) {
    ModerationManager mm = moderationManager();
    return mm != null ? String.valueOf(mm.isBanned(uuid)) : "false";
  }

  private String isMuted(@NotNull UUID uuid) {
    ModerationManager mm = moderationManager();
    return mm != null ? String.valueOf(mm.isMuted(uuid)) : "false";
  }

  private String banReason(@NotNull UUID uuid) {
    ModerationManager mm = moderationManager();
    if (mm == null) return "";
    Punishment ban = mm.getActiveBan(uuid);
    if (ban == null) return "";
    return ban.reason() != null ? ban.reason() : "";
  }

  private String muteReason(@NotNull UUID uuid) {
    TpaManager tm = tpaManager();
    if (tm == null) return "";
    PlayerData data = tm.getPlayerData(uuid);
    if (data == null) return "";
    Punishment mute = data.getActiveMute();
    if (mute == null) return "";
    return mute.reason() != null ? mute.reason() : "";
  }

  private String banExpires(@NotNull UUID uuid) {
    ModerationManager mm = moderationManager();
    if (mm == null) return "";
    Punishment ban = mm.getActiveBan(uuid);
    if (ban == null) return "";
    if (ban.isPermanent()) return "permanent";
    return formatInstant(ban.expiresAt());
  }

  private String muteExpires(@NotNull UUID uuid) {
    TpaManager tm = tpaManager();
    if (tm == null) return "";
    PlayerData data = tm.getPlayerData(uuid);
    if (data == null) return "";
    Punishment mute = data.getActiveMute();
    if (mute == null) return "";
    if (mute.isPermanent()) return "permanent";
    return formatInstant(mute.expiresAt());
  }

  private String punishmentCount(@NotNull UUID uuid) {
    ModerationManager mm = moderationManager();
    if (mm == null) return "0";
    return String.valueOf(mm.getHistory(uuid).size());
  }

  private String warnCount(@NotNull UUID uuid) {
    ModerationManager mm = moderationManager();
    if (mm == null) return "0";
    List<Punishment> history = mm.getHistory(uuid);
    long count = history.stream()
        .filter(p -> p.type() == PunishmentType.WARN)
        .count();
    return String.valueOf(count);
  }

  // ========== Utility ==========

  private String isAfk(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null ? String.valueOf(um.isAfk(uuid)) : "false";
  }

  private String isFlying(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null ? String.valueOf(um.isFlying(uuid)) : "false";
  }

  private String isGod(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null ? String.valueOf(um.isGod(uuid)) : "false";
  }

  private String isInfiniteStamina(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null ? String.valueOf(um.isInfiniteStamina(uuid)) : "false";
  }

  private String isVanished(@NotNull UUID uuid) {
    VanishManager vm = vanishManager();
    return vm != null ? String.valueOf(vm.isVanished(uuid)) : "false";
  }

  private String playtime(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    return um != null ? String.valueOf(um.getTotalPlaytimeMs(uuid)) : "0";
  }

  private String playtimeFormatted(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    if (um == null) return "0m";
    return formatDuration(um.getTotalPlaytimeMs(uuid));
  }

  private String sessionTime(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    if (um == null) return "0";
    Instant sessionStart = um.getSessionStart(uuid);
    if (sessionStart == null) return "0";
    return String.valueOf(Duration.between(sessionStart, Instant.now()).toMillis());
  }

  private String sessionTimeFormatted(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    if (um == null) return "0m";
    Instant sessionStart = um.getSessionStart(uuid);
    if (sessionStart == null) return "0m";
    return formatDuration(Duration.between(sessionStart, Instant.now()).toMillis());
  }

  private String firstJoin(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    if (um == null) return "";
    Instant instant = um.getFirstJoin(uuid);
    return instant != null ? instant.toString() : "";
  }

  private String firstJoinFormatted(@NotNull UUID uuid) {
    UtilityManager um = utilityManager();
    if (um == null) return "";
    Instant instant = um.getFirstJoin(uuid);
    return instant != null ? formatInstant(instant) : "";
  }

  private String lastJoin(@NotNull UUID uuid) {
    TpaManager tm = tpaManager();
    if (tm == null) return "";
    PlayerData data = tm.getPlayerData(uuid);
    if (data == null) return "";
    return data.getLastJoin().toString();
  }

  private String lastJoinFormatted(@NotNull UUID uuid) {
    TpaManager tm = tpaManager();
    if (tm == null) return "";
    PlayerData data = tm.getPlayerData(uuid);
    if (data == null) return "";
    return formatInstant(data.getLastJoin());
  }

  // ========== Warps / Spawns / TPA ==========

  private String warpCount() {
    WarpManager wm = warpManager();
    return wm != null ? String.valueOf(wm.getWarpCount()) : "0";
  }

  private String warpAccessibleCount(@NotNull UUID uuid) {
    WarpManager wm = warpManager();
    return wm != null ? String.valueOf(wm.getAccessibleWarps(uuid).size()) : "0";
  }

  private String spawnCount() {
    SpawnManager sm = spawnManager();
    return sm != null ? String.valueOf(sm.getSpawnCount()) : "0";
  }

  private String isAcceptingTpa(@NotNull UUID uuid) {
    TpaManager tm = tpaManager();
    return tm != null ? String.valueOf(tm.isAcceptingRequests(uuid)) : "true";
  }

  private String tpaPendingCount(@NotNull UUID uuid) {
    TpaManager tm = tpaManager();
    return tm != null ? String.valueOf(tm.getIncomingRequests(uuid).size()) : "0";
  }

  // ========== Manager Accessors (null-safe) ==========

  @Nullable
  private HomeManager homeManager() {
    HomesModule m = plugin.getHomesModule();
    return (m != null && m.isEnabled()) ? m.getHomeManager() : null;
  }

  @Nullable
  private KitManager kitManager() {
    KitsModule m = plugin.getKitsModule();
    return (m != null && m.isEnabled()) ? m.getKitManager() : null;
  }

  @Nullable
  private ModerationManager moderationManager() {
    ModerationModule m = plugin.getModerationModule();
    return (m != null && m.isEnabled()) ? m.getModerationManager() : null;
  }

  @Nullable
  private VanishManager vanishManager() {
    ModerationModule m = plugin.getModerationModule();
    return (m != null && m.isEnabled()) ? m.getVanishManager() : null;
  }

  @Nullable
  private UtilityManager utilityManager() {
    UtilityModule m = plugin.getUtilityModule();
    return (m != null && m.isEnabled()) ? m.getUtilityManager() : null;
  }

  @Nullable
  private WarpManager warpManager() {
    WarpsModule m = plugin.getWarpsModule();
    return (m != null && m.isEnabled()) ? m.getWarpManager() : null;
  }

  @Nullable
  private SpawnManager spawnManager() {
    SpawnsModule m = plugin.getSpawnsModule();
    return (m != null && m.isEnabled()) ? m.getSpawnManager() : null;
  }

  @Nullable
  private TpaManager tpaManager() {
    TeleportModule m = plugin.getTeleportModule();
    return (m != null && m.isEnabled()) ? m.getTpaManager() : null;
  }

  // ========== Helpers ==========

  /**
   * Formats a duration in milliseconds to a human-readable string.
   * Examples: "3d 2h 15m", "5h 30m", "12m", "0m"
   */
  @NotNull
  private static String formatDuration(long ms) {
    if (ms <= 0) return "0m";

    long totalMinutes = ms / 60_000;
    long days = totalMinutes / 1440;
    long hours = (totalMinutes % 1440) / 60;
    long minutes = totalMinutes % 60;

    StringBuilder sb = new StringBuilder();
    if (days > 0) sb.append(days).append("d ");
    if (hours > 0) sb.append(hours).append("h ");
    sb.append(minutes).append("m");
    return sb.toString().trim();
  }

  /**
   * Formats an Instant to a human-readable date string (yyyy-MM-dd HH:mm).
   */
  @NotNull
  private static String formatInstant(@Nullable Instant instant) {
    if (instant == null) return "";
    return DATE_FORMAT.format(instant);
  }
}
