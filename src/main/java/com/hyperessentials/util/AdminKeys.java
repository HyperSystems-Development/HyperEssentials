package com.hyperessentials.util;

/**
 * Static constants for HyperEssentials admin GUI i18n message keys.
 *
 * <p>
 * Contains all user-facing strings from admin GUI pages (dashboard, warps,
 * spawns, kits, players, moderation, announcements, settings).
 *
 * <p>
 * Key format: {@code hyperessentials_admin.{page}.{element}}
 */
public final class AdminKeys {

  private AdminKeys() {}

  /** Admin navigation bar labels. */
  public static final class Nav {
    private static final String P = "hyperessentials_admin.nav.";
    public static final String TITLE = P + "title";

    private Nav() {}
  }

  /** Admin dashboard page. */
  public static final class Dashboard {
    private static final String P = "hyperessentials_admin.dashboard.";
    public static final String MODULE_ENABLED = P + "module_enabled";
    public static final String MODULE_DISABLED = P + "module_disabled";

    private Dashboard() {}
  }

  /** Admin players page. */
  public static final class Players {
    private static final String P = "hyperessentials_admin.players.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String PLAYER_INFO = P + "player_info";
    public static final String DETAIL_UUID = P + "detail_uuid";
    public static final String DETAIL_FIRST_JOIN = P + "detail_first_join";
    public static final String DETAIL_LAST_SEEN = P + "detail_last_seen";
    public static final String DETAIL_PLAYTIME = P + "detail_playtime";
    public static final String DETAIL_PUNISHMENTS = P + "detail_punishments";
    public static final String DETAIL_STATUS = P + "detail_status";
    public static final String DETAIL_STATUS_ONLINE = P + "detail_status_online";
    public static final String DETAIL_STATUS_BANNED = P + "detail_status_banned";
    public static final String DETAIL_STATUS_MUTED = P + "detail_status_muted";
    public static final String KICKED = P + "kicked";
    public static final String MUTED = P + "muted";
    public static final String BANNED = P + "banned";

    private Players() {}
  }

  /** Admin warps page. */
  public static final class Warps {
    private static final String P = "hyperessentials_admin.warps.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String EDIT_TITLE = P + "edit_title";
    public static final String EDIT_DISPLAY_NAME = P + "edit_display_name";
    public static final String EDIT_CATEGORY = P + "edit_category";
    public static final String EDIT_DESCRIPTION = P + "edit_description";
    public static final String EDIT_PERMISSION = P + "edit_permission";
    public static final String EDIT_WORLD = P + "edit_world";
    public static final String EDIT_COORDS = P + "edit_coords";
    public static final String NAME_PLACEHOLDER = P + "name_placeholder";
    public static final String CREATE_TITLE = P + "create_title";
    public static final String SEARCH_PLACEHOLDER = P + "search_placeholder";

    private Warps() {}
  }

  /** Admin spawns page. */
  public static final class Spawns {
    private static final String P = "hyperessentials_admin.spawns.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String GLOBAL_BADGE = P + "global_badge";
    public static final String DEFAULT_BADGE = P + "default_badge";
    public static final String DEFAULT_COORDS = P + "default_coords";

    private Spawns() {}
  }

  /** Admin kits page. */
  public static final class Kits {
    private static final String P = "hyperessentials_admin.kits.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String ONE_TIME = P + "one_time";
    public static final String NAME_PLACEHOLDER = P + "name_placeholder";
    public static final String EDIT_TITLE = P + "edit_title";
    public static final String EDIT_DISPLAY_NAME = P + "edit_display_name";
    public static final String EDIT_COOLDOWN = P + "edit_cooldown";
    public static final String EDIT_PERMISSION = P + "edit_permission";
    public static final String PREVIEW_TITLE = P + "preview_title";
    public static final String PREVIEW_NO_ITEMS = P + "preview_no_items";
    public static final String CREATE_TITLE = P + "create_title";
    public static final String SEARCH_PLACEHOLDER = P + "search_placeholder";

    private Kits() {}
  }

  /** Admin moderation page. */
  public static final class Moderation {
    private static final String P = "hyperessentials_admin.mod.";
    public static final String EMPTY_ACTIVE_TITLE = P + "empty_active_title";
    public static final String EMPTY_ALL_TITLE = P + "empty_all_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String NO_REASON = P + "no_reason";
    public static final String PERMANENT = P + "permanent";
    public static final String EXPIRED = P + "expired";
    public static final String REVOKED = P + "revoked";

    private Moderation() {}
  }

  /** Admin player moderation page. */
  public static final class PlayerMod {
    private static final String P = "hyperessentials_admin.playermod.";
    public static final String SEARCH_PLACEHOLDER = P + "search_placeholder";
    public static final String SEARCH_EMPTY = P + "search_empty";
    public static final String SEARCH_HINT = P + "search_hint";
    public static final String ONLINE_BADGE = P + "online_badge";
    public static final String OFFLINE_BADGE = P + "offline_badge";
    public static final String DETAIL_UUID = P + "detail_uuid";
    public static final String DETAIL_STATUS = P + "detail_status";
    public static final String DETAIL_FIRST_JOIN = P + "detail_first_join";
    public static final String DETAIL_LAST_SEEN = P + "detail_last_seen";
    public static final String DETAIL_PLAYTIME = P + "detail_playtime";
    public static final String DETAIL_HISTORY = P + "detail_history";
    public static final String DETAIL_NO_HISTORY = P + "detail_no_history";
    public static final String ACTION_DURATION = P + "action_duration";
    public static final String ACTION_REASON = P + "action_reason";
    public static final String ACTION_CONFIRM = P + "action_confirm";
    public static final String ACTION_CANCEL = P + "action_cancel";
    public static final String ACTION_SUCCESS = P + "action_success";
    public static final String ACTION_FAILED = P + "action_failed";
    public static final String PLAYER_NOT_FOUND = P + "player_not_found";

    private PlayerMod() {}
  }

  /** Admin announcements page. */
  public static final class Announcements {
    private static final String P = "hyperessentials_admin.announce.";
    public static final String INTERVAL_LABEL = P + "interval_label";
    public static final String DISABLED = P + "disabled";
    public static final String MODE_RANDOM = P + "mode_random";
    public static final String MODE_SEQUENTIAL = P + "mode_sequential";
    public static final String EDIT_TITLE = P + "edit_title";
    public static final String EVENTS_TITLE = P + "events_title";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";

    private Announcements() {}
  }

  /** Admin backups page. */
  public static final class Backups {
    private static final String P = "hyperessentials_admin.backups.";
    public static final String CREATING = P + "creating";
    public static final String CREATED = P + "created";
    public static final String CREATE_FAILED = P + "create_failed";
    public static final String RESTORING = P + "restoring";
    public static final String RESTORED = P + "restored";
    public static final String RESTORE_FAILED = P + "restore_failed";
    public static final String DELETING = P + "deleting";
    public static final String DELETED = P + "deleted";
    public static final String DELETE_FAILED = P + "delete_failed";

    private Backups() {}
  }

  /** Admin config editor page. */
  public static final class Config {
    private static final String P = "hyperessentials_admin.config.";
    public static final String SAVED = P + "saved";
    public static final String CHANGED = P + "changed";

    private Config() {}
  }

  /** Admin settings page. */
  public static final class Settings {
    private static final String P = "hyperessentials_admin.settings.";
    public static final String DATA_DIR = P + "data_dir";
    public static final String MODULE_ENABLED = P + "module_enabled";
    public static final String MODULE_DISABLED = P + "module_disabled";

    private Settings() {}
  }

  /** Admin permission quick-add view. */
  public static final class Perms {
    private static final String P = "hyperessentials_admin.perms.";
    public static final String TITLE = P + "title";
    public static final String NODE_LABEL = P + "node_label";
    public static final String ROLES_HEADER = P + "roles_header";
    public static final String ADD = P + "add";
    public static final String REMOVE = P + "remove";
    public static final String NO_ROLES = P + "no_roles";

    private Perms() {}
  }
}
