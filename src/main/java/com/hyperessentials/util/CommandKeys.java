package com.hyperessentials.util;

/**
 * Static constants for HyperEssentials player command i18n message keys.
 *
 * <p>
 * Organized by module/command with nested static classes.
 * Admin GUI keys are in {@link AdminKeys}, player GUI keys in {@link GuiKeys}.
 *
 * <p>
 * Key format: {@code hyperessentials.cmd.{command}.{action}}
 */
public final class CommandKeys {

  private CommandKeys() {}

  /** Shared messages used across multiple commands. */
  public static final class Common {
    private static final String P = "hyperessentials.common.";
    public static final String NO_PERMISSION = P + "no_permission";
    public static final String PLAYER_NOT_FOUND = P + "player_not_found";
    public static final String PLAYER_NOT_ONLINE = P + "player_not_online";
    public static final String PLAYER_NOT_IN_WORLD = P + "player_not_in_world";
    public static final String CANNOT_GET_POSITION = P + "cannot_get_position";
    public static final String CANNOT_ACCESS_PLAYER = P + "cannot_access_player";
    public static final String ON_COOLDOWN = P + "on_cooldown";
    public static final String WARMUP_STARTING = P + "warmup_starting";
    public static final String NOT_HOLDING_ITEM = P + "not_holding_item";
    public static final String CANNOT_SELF = P + "cannot_self";

    private Common() {}
  }

  /** /home, /sethome, /delhome, /homes command messages. */
  public static final class Home {
    private static final String P = "hyperessentials.cmd.home.";
    // /home
    public static final String NO_PERMISSION = P + "no_permission";
    public static final String NO_HOMES = P + "no_homes";
    public static final String NO_DEFAULT = P + "no_default";
    public static final String YOUR_HOMES = P + "your_homes";
    public static final String NOT_FOUND = P + "not_found";
    public static final String TELEPORTED = P + "teleported";
    public static final String FACTION_NO_HOME = P + "faction_no_home";
    public static final String FACTION_RESOLVE_FAILED = P + "faction_resolve_failed";
    public static final String FACTION_ZONE_RESTRICTED = P + "faction_zone_restricted";
    public static final String FACTION_WORLD_NOT_FOUND = P + "faction_world_not_found";
    public static final String FACTION_TELEPORTED = P + "faction_teleported";
    // /sethome
    public static final String SET_NO_PERMISSION = P + "set_no_permission";
    public static final String SET_INVALID_NAME = P + "set_invalid_name";
    public static final String SET_LIMIT_REACHED = P + "set_limit_reached";
    public static final String SET_SUCCESS = P + "set_success";
    public static final String SET_UPDATED = P + "set_updated";
    public static final String SET_LOCATION = P + "set_location";
    // /delhome
    public static final String DEL_NO_PERMISSION = P + "del_no_permission";
    public static final String DEL_USAGE = P + "del_usage";
    public static final String DEL_SUCCESS = P + "del_success";
    public static final String DEL_NOT_FOUND = P + "del_not_found";
    // /homes
    public static final String LIST_NO_PERMISSION = P + "list_no_permission";
    public static final String LIST_EMPTY = P + "list_empty";
    public static final String LIST_HEADER = P + "list_header";
    public static final String LIST_FACTION_HOME = P + "list_faction_home";

    private Home() {}
  }

  /** /warp, /setwarp, /delwarp, /warps, /warpinfo command messages. */
  public static final class Warp {
    private static final String P = "hyperessentials.cmd.warp.";
    // /warp
    public static final String NO_PERMISSION = P + "no_permission";
    public static final String NOT_FOUND = P + "not_found";
    public static final String ACCESS_DENIED = P + "access_denied";
    public static final String ZONE_RESTRICTED = P + "zone_restricted";
    public static final String TELEPORTED = P + "teleported";
    public static final String NO_WARPS = P + "no_warps";
    public static final String AVAILABLE_WARPS = P + "available_warps";
    public static final String USE_WARP_HINT = P + "use_warp_hint";
    // /setwarp
    public static final String SET_NO_PERMISSION = P + "set_no_permission";
    public static final String SET_USAGE = P + "set_usage";
    public static final String SET_NAME_LENGTH = P + "set_name_length";
    public static final String SET_NAME_INVALID = P + "set_name_invalid";
    public static final String SET_SUCCESS = P + "set_success";
    public static final String SET_LOCATION = P + "set_location";
    public static final String SET_CATEGORY = P + "set_category";
    // /delwarp
    public static final String DEL_NO_PERMISSION = P + "del_no_permission";
    public static final String DEL_USAGE = P + "del_usage";
    public static final String DEL_SUCCESS = P + "del_success";
    public static final String DEL_NOT_FOUND = P + "del_not_found";
    // /warps
    public static final String LIST_NO_PERMISSION = P + "list_no_permission";
    public static final String LIST_EMPTY = P + "list_empty";
    public static final String LIST_CATEGORY_EMPTY = P + "list_category_empty";
    public static final String LIST_HEADER = P + "list_header";
    public static final String LIST_CATEGORY_HEADER = P + "list_category_header";
    public static final String LIST_CATEGORIES = P + "list_categories";
    public static final String LIST_FILTER_HINT = P + "list_filter_hint";
    // /warpinfo
    public static final String INFO_NO_PERMISSION = P + "info_no_permission";
    public static final String INFO_USAGE = P + "info_usage";
    public static final String INFO_HEADER = P + "info_header";
    public static final String INFO_NAME = P + "info_name";
    public static final String INFO_CATEGORY = P + "info_category";
    public static final String INFO_WORLD = P + "info_world";
    public static final String INFO_LOCATION = P + "info_location";
    public static final String INFO_DESCRIPTION = P + "info_description";
    public static final String INFO_PERMISSION = P + "info_permission";
    public static final String INFO_ACCESS = P + "info_access";
    public static final String INFO_CREATED = P + "info_created";

    private Warp() {}
  }

  /** /spawn, /setspawn, /delspawn, /spawns, /spawninfo command messages. */
  public static final class Spawn {
    private static final String P = "hyperessentials.cmd.spawn.";
    // /spawn
    public static final String NO_PERMISSION = P + "no_permission";
    public static final String NOT_FOUND_WORLD = P + "not_found_world";
    public static final String NOT_SET = P + "not_set";
    public static final String TELEPORTED = P + "teleported";
    // /setspawn
    public static final String SET_NO_PERMISSION = P + "set_no_permission";
    public static final String SET_SUCCESS = P + "set_success";
    public static final String SET_LOCATION = P + "set_location";
    public static final String SET_GLOBAL = P + "set_global";
    // /delspawn
    public static final String DEL_NO_PERMISSION = P + "del_no_permission";
    public static final String DEL_NOT_FOUND = P + "del_not_found";
    public static final String DEL_SUCCESS = P + "del_success";
    public static final String DEL_NO_SPAWN = P + "del_no_spawn";
    // /spawns
    public static final String LIST_NO_PERMISSION = P + "list_no_permission";
    public static final String LIST_EMPTY = P + "list_empty";
    public static final String LIST_HINT = P + "list_hint";
    public static final String LIST_HEADER = P + "list_header";
    public static final String LIST_USE_HINT = P + "list_use_hint";
    // /spawninfo
    public static final String INFO_NO_PERMISSION = P + "info_no_permission";
    public static final String INFO_NOT_FOUND = P + "info_not_found";
    public static final String INFO_NO_SPAWN = P + "info_no_spawn";
    public static final String INFO_HEADER = P + "info_header";
    public static final String INFO_LOCATION = P + "info_location";
    public static final String INFO_GLOBAL = P + "info_global";
    public static final String INFO_CREATED_BY = P + "info_created_by";
    public static final String INFO_CREATED = P + "info_created";

    private Spawn() {}
  }

  /** /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel, /tptoggle, /back, /rtp command messages. */
  public static final class Tpa {
    private static final String P = "hyperessentials.cmd.tpa.";
    // /tpa
    public static final String NO_PERMISSION = P + "no_permission";
    public static final String USAGE = P + "usage";
    public static final String CANNOT_SELF = P + "cannot_self";
    public static final String COOLDOWN = P + "cooldown";
    public static final String TARGET_NOT_ACCEPTING = P + "target_not_accepting";
    public static final String SEND_FAILED = P + "send_failed";
    public static final String SENT = P + "sent";
    public static final String EXPIRES_IN = P + "expires_in";
    public static final String RECEIVED_TPA = P + "received_tpa";
    public static final String RESPOND_HINT = P + "respond_hint";
    // /tpahere
    public static final String HERE_NO_PERMISSION = P + "here_no_permission";
    public static final String HERE_USAGE = P + "here_usage";
    public static final String HERE_CANNOT_SELF = P + "here_cannot_self";
    public static final String HERE_SENT = P + "here_sent";
    public static final String RECEIVED_TPAHERE = P + "received_tpahere";
    // /tpaccept
    public static final String ACCEPT_NO_PERMISSION = P + "accept_no_permission";
    public static final String ACCEPT_NO_PENDING = P + "accept_no_pending";
    public static final String ACCEPT_NO_REQUEST_FROM = P + "accept_no_request_from";
    public static final String ACCEPT_EXPIRED = P + "accept_expired";
    public static final String ACCEPT_PLAYER_OFFLINE = P + "accept_player_offline";
    public static final String ACCEPT_SUCCESS = P + "accept_success";
    public static final String ACCEPT_TELEPORTING = P + "accept_teleporting";
    public static final String ACCEPT_TELEPORTING_TO_YOU = P + "accept_teleporting_to_you";
    // /tpdeny
    public static final String DENY_NO_PERMISSION = P + "deny_no_permission";
    public static final String DENY_NO_PENDING = P + "deny_no_pending";
    public static final String DENY_NO_REQUEST_FROM = P + "deny_no_request_from";
    public static final String DENY_SUCCESS = P + "deny_success";
    public static final String DENY_NOTIFY = P + "deny_notify";
    // /tpcancel
    public static final String CANCEL_NO_PERMISSION = P + "cancel_no_permission";
    public static final String CANCEL_NO_PENDING = P + "cancel_no_pending";
    public static final String CANCEL_SUCCESS = P + "cancel_success";
    public static final String CANCEL_NOTIFY = P + "cancel_notify";
    // /tptoggle
    public static final String TOGGLE_NO_PERMISSION = P + "toggle_no_permission";
    public static final String TOGGLE_ENABLED = P + "toggle_enabled";
    public static final String TOGGLE_DISABLED = P + "toggle_disabled";

    private Tpa() {}
  }

  /** /back command messages. */
  public static final class Back {
    private static final String P = "hyperessentials.cmd.back.";
    public static final String NO_PERMISSION = P + "no_permission";
    public static final String NO_LOCATION = P + "no_location";
    public static final String TELEPORTED = P + "teleported";
    public static final String BLOCKED_OWN = P + "blocked_own";
    public static final String BLOCKED_ALLY = P + "blocked_ally";
    public static final String BLOCKED_ENEMY = P + "blocked_enemy";
    public static final String BLOCKED_NEUTRAL = P + "blocked_neutral";
    public static final String BLOCKED_WILDERNESS = P + "blocked_wilderness";
    public static final String BLOCKED_ZONE = P + "blocked_zone";
    public static final String BLOCKED_GENERIC = P + "blocked_generic";

    private Back() {}
  }

  /** /rtp command messages. */
  public static final class Rtp {
    private static final String P = "hyperessentials.cmd.rtp.";
    public static final String NO_PERMISSION = P + "no_permission";
    public static final String WORLD_BLACKLISTED = P + "world_blacklisted";
    public static final String SEARCHING = P + "searching";
    public static final String TELEPORTED = P + "teleported";
    public static final String FOUND_LOCATION = P + "found_location";

    private Rtp() {}
  }

  /** /kit, /kits, /createkit, /deletekit, /previewkit command messages. */
  public static final class Kit {
    private static final String P = "hyperessentials.cmd.kit.";
    // /kit
    public static final String NO_PERMISSION = P + "no_permission";
    public static final String ZONE_RESTRICTED = P + "zone_restricted";
    public static final String USAGE = P + "usage";
    public static final String NOT_FOUND = P + "not_found";
    public static final String CLAIMED = P + "claimed";
    public static final String ON_COOLDOWN = P + "on_cooldown";
    public static final String ALREADY_CLAIMED = P + "already_claimed";
    public static final String KIT_NO_PERMISSION = P + "kit_no_permission";
    public static final String INSUFFICIENT_SPACE = P + "insufficient_space";
    // /kits
    public static final String LIST_NO_PERMISSION = P + "list_no_permission";
    public static final String LIST_EMPTY = P + "list_empty";
    public static final String LIST_HEADER = P + "list_header";
    // /createkit
    public static final String CREATE_NO_PERMISSION = P + "create_no_permission";
    public static final String CREATE_USAGE = P + "create_usage";
    public static final String CREATE_INVALID_NAME = P + "create_invalid_name";
    public static final String CREATE_ALREADY_EXISTS = P + "create_already_exists";
    public static final String CREATE_SUCCESS = P + "create_success";
    // /deletekit
    public static final String DELETE_NO_PERMISSION = P + "delete_no_permission";
    public static final String DELETE_USAGE = P + "delete_usage";
    public static final String DELETE_SUCCESS = P + "delete_success";
    public static final String DELETE_NOT_FOUND = P + "delete_not_found";
    // /previewkit
    public static final String PREVIEW_NO_PERMISSION = P + "preview_no_permission";
    public static final String PREVIEW_USAGE = P + "preview_usage";
    public static final String PREVIEW_HEADER = P + "preview_header";
    public static final String PREVIEW_EMPTY = P + "preview_empty";
    public static final String PREVIEW_COOLDOWN = P + "preview_cooldown";
    public static final String PREVIEW_ONE_TIME = P + "preview_one_time";
    public static final String PREVIEW_PERMISSION = P + "preview_permission";

    private Kit() {}
  }

  /** /ban, /unban, /mute, /unmute, /kick, /freeze, /vanish, /ipban, /ipunban, /punishments. */
  public static final class Moderation {
    private static final String P = "hyperessentials.cmd.mod.";
    // /ban
    public static final String BAN_NO_PERMISSION = P + "ban_no_permission";
    public static final String BAN_USAGE = P + "ban_usage";
    public static final String BAN_CANNOT_BAN = P + "ban_cannot_ban";
    public static final String BAN_TEMP = P + "ban_temp";
    public static final String BAN_PERMANENT = P + "ban_permanent";
    // /unban
    public static final String UNBAN_NO_PERMISSION = P + "unban_no_permission";
    public static final String UNBAN_USAGE = P + "unban_usage";
    public static final String UNBAN_SUCCESS = P + "unban_success";
    public static final String UNBAN_NOT_BANNED = P + "unban_not_banned";
    // /mute
    public static final String MUTE_NO_PERMISSION = P + "mute_no_permission";
    public static final String MUTE_USAGE = P + "mute_usage";
    public static final String MUTE_CANNOT_MUTE = P + "mute_cannot_mute";
    public static final String MUTE_TEMP = P + "mute_temp";
    public static final String MUTE_PERMANENT = P + "mute_permanent";
    // /unmute
    public static final String UNMUTE_NO_PERMISSION = P + "unmute_no_permission";
    public static final String UNMUTE_USAGE = P + "unmute_usage";
    public static final String UNMUTE_SUCCESS = P + "unmute_success";
    public static final String UNMUTE_NOT_MUTED = P + "unmute_not_muted";
    // /kick
    public static final String KICK_NO_PERMISSION = P + "kick_no_permission";
    public static final String KICK_USAGE = P + "kick_usage";
    public static final String KICK_SUCCESS = P + "kick_success";
    // /freeze
    public static final String FREEZE_NO_PERMISSION = P + "freeze_no_permission";
    public static final String FREEZE_USAGE = P + "freeze_usage";
    public static final String FREEZE_CANNOT_FREEZE = P + "freeze_cannot_freeze";
    public static final String FREEZE_FROZEN = P + "freeze_frozen";
    public static final String FREEZE_UNFROZEN = P + "freeze_unfrozen";
    public static final String FREEZE_YOU_FROZEN = P + "freeze_you_frozen";
    public static final String FREEZE_YOU_UNFROZEN = P + "freeze_you_unfrozen";
    // /vanish
    public static final String VANISH_NO_PERMISSION = P + "vanish_no_permission";
    // /ipban
    public static final String IPBAN_NO_PERMISSION = P + "ipban_no_permission";
    public static final String IPBAN_USAGE = P + "ipban_usage";
    public static final String IPBAN_MUST_BE_ONLINE = P + "ipban_must_be_online";
    public static final String IPBAN_NO_IP = P + "ipban_no_ip";
    public static final String IPBAN_TEMP = P + "ipban_temp";
    public static final String IPBAN_PERMANENT = P + "ipban_permanent";
    // /ipunban
    public static final String IPUNBAN_NO_PERMISSION = P + "ipunban_no_permission";
    public static final String IPUNBAN_USAGE = P + "ipunban_usage";
    public static final String IPUNBAN_SUCCESS = P + "ipunban_success";
    public static final String IPUNBAN_NOT_BANNED = P + "ipunban_not_banned";
    // /warn
    public static final String WARN_NO_PERMISSION = P + "warn_no_permission";
    public static final String WARN_USAGE = P + "warn_usage";
    public static final String WARN_SUCCESS = P + "warn_success";
    // /punishments
    public static final String HISTORY_NO_PERMISSION = P + "history_no_permission";
    public static final String HISTORY_USAGE = P + "history_usage";
    public static final String HISTORY_EMPTY = P + "history_empty";
    public static final String HISTORY_HEADER = P + "history_header";
    public static final String HISTORY_MORE = P + "history_more";
    public static final String HISTORY_REASON = P + "history_reason";

    private Moderation() {}
  }

  /** /fly, /heal, /god, /afk, /list, /clearinventory, /repair, /repairmax, etc. */
  public static final class Utility {
    private static final String P = "hyperessentials.cmd.util.";
    // /fly
    public static final String FLY_NO_PERMISSION = P + "fly_no_permission";
    public static final String FLY_OTHERS_NO_PERMISSION = P + "fly_others_no_permission";
    public static final String FLY_ENABLED = P + "fly_enabled";
    public static final String FLY_DISABLED = P + "fly_disabled";
    public static final String FLY_ENABLED_OTHER = P + "fly_enabled_other";
    public static final String FLY_DISABLED_OTHER = P + "fly_disabled_other";
    // /heal
    public static final String HEAL_NO_PERMISSION = P + "heal_no_permission";
    public static final String HEAL_OTHERS_NO_PERMISSION = P + "heal_others_no_permission";
    public static final String HEAL_SUCCESS = P + "heal_success";
    public static final String HEAL_SUCCESS_OTHER = P + "heal_success_other";
    public static final String HEAL_YOU_HEALED = P + "heal_you_healed";
    // /god
    public static final String GOD_NO_PERMISSION = P + "god_no_permission";
    public static final String GOD_OTHERS_NO_PERMISSION = P + "god_others_no_permission";
    public static final String GOD_ENABLED = P + "god_enabled";
    public static final String GOD_DISABLED = P + "god_disabled";
    public static final String GOD_ENABLED_OTHER = P + "god_enabled_other";
    public static final String GOD_DISABLED_OTHER = P + "god_disabled_other";
    // /afk
    public static final String AFK_NO_PERMISSION = P + "afk_no_permission";
    public static final String AFK_NOW = P + "afk_now";
    public static final String AFK_NO_LONGER = P + "afk_no_longer";
    // /list
    public static final String LIST_HEADER = P + "list_header";
    // /clearinventory
    public static final String CI_NO_PERMISSION = P + "ci_no_permission";
    public static final String CI_OTHERS_NO_PERMISSION = P + "ci_others_no_permission";
    public static final String CI_SUCCESS = P + "ci_success";
    public static final String CI_SUCCESS_OTHER = P + "ci_success_other";
    public static final String CI_YOU_CLEARED = P + "ci_you_cleared";
    // /repair
    public static final String REPAIR_NO_PERMISSION = P + "repair_no_permission";
    public static final String REPAIR_CANNOT_REPAIR = P + "repair_cannot_repair";
    public static final String REPAIR_ALREADY_FULL = P + "repair_already_full";
    public static final String REPAIR_SUCCESS = P + "repair_success";
    public static final String REPAIR_FAILED = P + "repair_failed";
    // /repairmax
    public static final String REPAIRMAX_ALREADY_FULL = P + "repairmax_already_full";
    public static final String REPAIRMAX_SUCCESS = P + "repairmax_success";
    // /playtime
    public static final String PLAYTIME_NO_PERMISSION = P + "playtime_no_permission";
    public static final String PLAYTIME_RESULT = P + "playtime_result";
    // /joindate
    public static final String JOINDATE_NO_PERMISSION = P + "joindate_no_permission";
    public static final String JOINDATE_NO_DATA = P + "joindate_no_data";
    public static final String JOINDATE_RESULT = P + "joindate_result";
    // /near
    public static final String NEAR_NO_PERMISSION = P + "near_no_permission";
    public static final String NEAR_INVALID_RADIUS = P + "near_invalid_radius";
    public static final String NEAR_EMPTY = P + "near_empty";
    public static final String NEAR_HEADER = P + "near_header";
    // /discord
    public static final String DISCORD_NO_LINK = P + "discord_no_link";
    public static final String DISCORD_MESSAGE = P + "discord_message";
    // /motd
    public static final String MOTD_NO_PERMISSION = P + "motd_no_permission";
    public static final String MOTD_HEADER = P + "motd_header";
    // /rules
    public static final String RULES_HEADER = P + "rules_header";
    // /clearchat
    public static final String CLEARCHAT_NO_PERMISSION = P + "clearchat_no_permission";
    public static final String CLEARCHAT_OTHERS_NO_PERMISSION = P + "clearchat_others_no_permission";
    public static final String CLEARCHAT_SUCCESS = P + "clearchat_success";
    public static final String CLEARCHAT_ALL_SUCCESS = P + "clearchat_all_success";
    // /invsee
    public static final String INVSEE_NO_PERMISSION = P + "invsee_no_permission";
    public static final String INVSEE_USAGE = P + "invsee_usage";
    public static final String INVSEE_FAILED = P + "invsee_failed";
    // /trash
    public static final String TRASH_NO_PERMISSION = P + "trash_no_permission";
    public static final String TRASH_FAILED = P + "trash_failed";
    // /durability
    public static final String DURA_NO_PERMISSION = P + "dura_no_permission";
    public static final String DURA_USAGE = P + "dura_usage";
    public static final String DURA_NO_DURABILITY = P + "dura_no_durability";
    public static final String DURA_SET_USAGE = P + "dura_set_usage";
    public static final String DURA_INVALID_NUMBER = P + "dura_invalid_number";
    public static final String DURA_MUST_POSITIVE = P + "dura_must_positive";
    public static final String DURA_SET_SUCCESS = P + "dura_set_success";
    public static final String DURA_RESET_SUCCESS = P + "dura_reset_success";
    public static final String DURA_FAILED = P + "dura_failed";
    // /maxstack
    public static final String MAXSTACK_NO_PERMISSION = P + "maxstack_no_permission";
    public static final String MAXSTACK_CANNOT_STACK = P + "maxstack_cannot_stack";
    public static final String MAXSTACK_ALREADY_MAX = P + "maxstack_already_max";
    public static final String MAXSTACK_SUCCESS = P + "maxstack_success";
    public static final String MAXSTACK_FAILED = P + "maxstack_failed";
    // /stamina
    public static final String STAMINA_NO_PERMISSION = P + "stamina_no_permission";
    public static final String STAMINA_OTHERS_NO_PERMISSION = P + "stamina_others_no_permission";
    public static final String STAMINA_ENABLED = P + "stamina_enabled";
    public static final String STAMINA_DISABLED = P + "stamina_disabled";
    public static final String STAMINA_ENABLED_OTHER = P + "stamina_enabled_other";
    public static final String STAMINA_DISABLED_OTHER = P + "stamina_disabled_other";
    // /sleeppercentage
    public static final String SLEEP_NO_PERMISSION = P + "sleep_no_permission";
    public static final String SLEEP_CURRENT = P + "sleep_current";
    public static final String SLEEP_DISABLED_HINT = P + "sleep_disabled_hint";
    public static final String SLEEP_INVALID_RANGE = P + "sleep_invalid_range";
    public static final String SLEEP_SET_GLOBAL = P + "sleep_set_global";
    public static final String SLEEP_SET_WORLD = P + "sleep_set_world";
    public static final String SLEEP_USAGE = P + "sleep_usage";

    private Utility() {}
  }

  /** /announce, /broadcast command messages. */
  public static final class Announce {
    private static final String P = "hyperessentials.cmd.announce.";
    // /announce
    public static final String NO_PERMISSION = P + "no_permission";
    public static final String LIST_EMPTY = P + "list_empty";
    public static final String LIST_HEADER = P + "list_header";
    public static final String ADD_USAGE = P + "add_usage";
    public static final String ADD_SUCCESS = P + "add_success";
    public static final String REMOVE_USAGE = P + "remove_usage";
    public static final String REMOVE_INVALID_INDEX = P + "remove_invalid_index";
    public static final String REMOVE_OUT_OF_RANGE = P + "remove_out_of_range";
    public static final String REMOVE_SUCCESS = P + "remove_success";
    public static final String RELOAD_SUCCESS = P + "reload_success";
    public static final String HELP_HEADER = P + "help_header";
    public static final String HELP_LIST = P + "help_list";
    public static final String HELP_ADD = P + "help_add";
    public static final String HELP_REMOVE = P + "help_remove";
    public static final String HELP_RELOAD = P + "help_reload";
    // /broadcast
    public static final String BC_NO_PERMISSION = P + "bc_no_permission";
    public static final String BC_USAGE = P + "bc_usage";
    public static final String BC_SENT = P + "bc_sent";

    private Announce() {}
  }

  /** /essimport command messages. */
  public static final class Import {
    private static final String P = "hyperessentials.cmd.import.";
    public static final String UNKNOWN_SOURCE = P + "unknown_source";
    public static final String UNKNOWN_FLAG = P + "unknown_flag";
    public static final String ALREADY_IN_PROGRESS = P + "already_in_progress";
    public static final String STARTING = P + "starting";
    public static final String DRY_RUN_NOTICE = P + "dry_run_notice";
    public static final String COMPLETE = P + "complete";
    public static final String FAILED = P + "failed";
    public static final String HELP_USAGE = P + "help_usage";
    public static final String HELP_SOURCES_HEADER = P + "help_sources_header";
    public static final String HELP_NO_SOURCES = P + "help_no_sources";
    public static final String HELP_FLAGS_HEADER = P + "help_flags_header";

    private Import() {}
  }
}
