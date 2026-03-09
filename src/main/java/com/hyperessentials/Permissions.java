package com.hyperessentials;

/**
 * Centralized permission node definitions for HyperEssentials.
 * Follows namespace.category.action hierarchy with wildcards at every level.
 */
public final class Permissions {

  private Permissions() {}

  public static final String ROOT = "hyperessentials";
  public static final String WILDCARD = ROOT + ".*";

  // === Homes ===
  public static final String HOME = ROOT + ".home";
  public static final String HOME_SET = HOME + ".set";
  public static final String HOME_DELETE = HOME + ".delete";
  public static final String HOME_LIST = HOME + ".list";
  public static final String HOME_GUI = HOME + ".gui";
  public static final String HOME_SHARE = HOME + ".share";
  public static final String HOME_UNLIMITED = HOME + ".unlimited";
  public static final String HOME_TELEPORT = HOME + ".teleport";
  public static final String HOME_LIMIT_PREFIX = HOME + ".limit.";

  // === Warps ===
  public static final String WARP = ROOT + ".warp";
  public static final String WARP_SET = WARP + ".set";
  public static final String WARP_DELETE = WARP + ".delete";
  public static final String WARP_LIST = WARP + ".list";
  public static final String WARP_INFO = WARP + ".info";

  // === Spawns ===
  public static final String SPAWN = ROOT + ".spawn";
  public static final String SPAWN_SET = SPAWN + ".set";
  public static final String SPAWN_DELETE = SPAWN + ".delete";
  public static final String SPAWN_LIST = SPAWN + ".list";
  public static final String SPAWN_INFO = SPAWN + ".info";

  // === Teleport ===
  public static final String TPA = ROOT + ".tpa";
  public static final String TPAHERE = ROOT + ".tpahere";
  public static final String TPACCEPT = ROOT + ".tpaccept";
  public static final String TPDENY = ROOT + ".tpdeny";
  public static final String TPCANCEL = ROOT + ".tpcancel";
  public static final String TPTOGGLE = ROOT + ".tptoggle";
  public static final String BACK = ROOT + ".back";

  // === Kits ===
  public static final String KIT_WILDCARD = ROOT + ".kit.*";
  public static final String KIT_USE = ROOT + ".kit.use";
  public static final String KIT_USE_PREFIX = ROOT + ".kit.use.";
  public static final String KIT_LIST = ROOT + ".kit.list";
  public static final String KIT_CREATE = ROOT + ".kit.create";
  public static final String KIT_DELETE = ROOT + ".kit.delete";

  // === Moderation ===
  public static final String MODERATION_WILDCARD = ROOT + ".moderation.*";
  public static final String MODERATION_BAN = ROOT + ".moderation.ban";
  public static final String MODERATION_MUTE = ROOT + ".moderation.mute";
  public static final String MODERATION_KICK = ROOT + ".moderation.kick";
  public static final String MODERATION_FREEZE = ROOT + ".moderation.freeze";
  public static final String MODERATION_VANISH = ROOT + ".moderation.vanish";
  public static final String MODERATION_HISTORY = ROOT + ".moderation.history";
  public static final String MODERATION_IPBAN = ROOT + ".moderation.ipban";

  // === Utility ===
  public static final String UTILITY_WILDCARD = ROOT + ".utility.*";
  public static final String UTILITY_HEAL = ROOT + ".utility.heal";
  public static final String UTILITY_HEAL_OTHERS = ROOT + ".utility.heal.others";
  public static final String UTILITY_FLY = ROOT + ".utility.fly";
  public static final String UTILITY_FLY_OTHERS = ROOT + ".utility.fly.others";
  public static final String UTILITY_GOD = ROOT + ".utility.god";
  public static final String UTILITY_GOD_OTHERS = ROOT + ".utility.god.others";
  public static final String UTILITY_CLEARCHAT = ROOT + ".utility.clearchat";
  public static final String UTILITY_CLEARCHAT_OTHERS = ROOT + ".utility.clearchat.others";
  public static final String UTILITY_CLEARINVENTORY = ROOT + ".utility.clearinventory";
  public static final String UTILITY_CLEARINVENTORY_OTHERS = ROOT + ".utility.clearinventory.others";
  public static final String UTILITY_REPAIR = ROOT + ".utility.repair";
  public static final String UTILITY_DURABILITY = ROOT + ".utility.durability";
  public static final String UTILITY_NEAR = ROOT + ".utility.near";
  public static final String UTILITY_MOTD = ROOT + ".utility.motd";
  public static final String UTILITY_PLAYTIME = ROOT + ".utility.playtime";
  public static final String UTILITY_JOINDATE = ROOT + ".utility.joindate";
  public static final String UTILITY_AFK = ROOT + ".utility.afk";
  public static final String UTILITY_INVSEE = ROOT + ".utility.invsee";
  public static final String UTILITY_STAMINA = ROOT + ".utility.stamina";
  public static final String UTILITY_STAMINA_OTHERS = ROOT + ".utility.stamina.others";
  public static final String UTILITY_TRASH = ROOT + ".utility.trash";
  public static final String UTILITY_MAXSTACK = ROOT + ".utility.maxstack";
  public static final String UTILITY_SLEEPPERCENTAGE = ROOT + ".utility.sleeppercentage";

  // === Announcements ===
  public static final String ANNOUNCE_WILDCARD = ROOT + ".announce.*";
  public static final String ANNOUNCE_BROADCAST = ROOT + ".announce.broadcast";
  public static final String ANNOUNCE_MANAGE = ROOT + ".announce.manage";

  // === Bypass ===
  public static final String BYPASS = ROOT + ".bypass";
  public static final String BYPASS_WILDCARD = BYPASS + ".*";
  public static final String BYPASS_WARMUP = BYPASS + ".warmup";
  public static final String BYPASS_COOLDOWN = BYPASS + ".cooldown";
  public static final String BYPASS_LIMIT = BYPASS + ".limit";
  public static final String BYPASS_TOGGLE = BYPASS + ".toggle";
  public static final String BYPASS_BAN = BYPASS + ".ban";
  public static final String BYPASS_MUTE = BYPASS + ".mute";
  public static final String BYPASS_FREEZE = BYPASS + ".freeze";
  public static final String BYPASS_KIT_COOLDOWN = BYPASS + ".kit.cooldown";
  public static final String BYPASS_FACTIONS = BYPASS + ".factions";
  public static final String BYPASS_FACTIONS_SETHOME = BYPASS_FACTIONS + ".sethome";
  public static final String BYPASS_FACTIONS_HOME = BYPASS_FACTIONS + ".home";
  public static final String BYPASS_FACTIONS_WARP = BYPASS_FACTIONS + ".warp";
  public static final String BYPASS_FACTIONS_KIT = BYPASS_FACTIONS + ".kit";
  public static final String BYPASS_FACTIONS_BACK = BYPASS_FACTIONS + ".back";

  // === Notify ===
  public static final String NOTIFY_WILDCARD = ROOT + ".notify.*";
  public static final String NOTIFY_BAN = ROOT + ".notify.ban";
  public static final String NOTIFY_MUTE = ROOT + ".notify.mute";
  public static final String NOTIFY_KICK = ROOT + ".notify.kick";

  // === Admin ===
  public static final String ADMIN = ROOT + ".admin";
  public static final String ADMIN_WILDCARD = ADMIN + ".*";
  public static final String ADMIN_RELOAD = ADMIN + ".reload";
  public static final String ADMIN_SETTINGS = ADMIN + ".settings";
  public static final String ADMIN_GUI = ADMIN + ".gui";

  // === RTP ===
  public static final String RTP = ROOT + ".rtp";
  public static final String RTP_BYPASS_FACTIONS = RTP + ".bypass.factions";
}
