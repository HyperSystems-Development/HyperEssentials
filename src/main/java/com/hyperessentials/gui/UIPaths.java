package com.hyperessentials.gui;

/**
 * Centralized UI template path constants for all HyperEssentials pages.
 */
public final class UIPaths {

  private UIPaths() {}

  private static final String BASE = "HyperEssentials/";

  // === Shared ===
  public static final String STYLES = BASE + "shared/styles.ui";
  public static final String NAV_BAR = BASE + "shared/nav_bar.ui";
  public static final String NAV_BUTTON = BASE + "shared/nav_button.ui";
  public static final String ERROR_PAGE = BASE + "shared/error_page.ui";
  public static final String EMPTY_STATE = BASE + "shared/empty_state.ui";
  public static final String CONFIRM_MODAL = BASE + "shared/confirm_modal.ui";
  public static final String STAT_ROW = BASE + "shared/stat_row.ui";

  // === Player Pages ===
  public static final String PLAYER_DASHBOARD = BASE + "player/dashboard.ui";
  public static final String PLAYER_STATS = BASE + "player/stats.ui";

  // === Homes ===
  public static final String HOMES_PAGE = BASE + "homes/homes_page.ui";
  public static final String HOME_ENTRY = BASE + "homes/home_entry.ui";
  public static final String FACTION_HOME_ENTRY = BASE + "homes/faction_home_entry.ui";
  public static final String HOME_SHARE_PAGE = BASE + "homes/home_share_page.ui";
  public static final String SHARE_SEARCH_RESULT = BASE + "homes/share_search_result.ui";
  public static final String SHARED_PLAYER_ENTRY = BASE + "homes/shared_player_entry.ui";
  public static final String SHARED_HOME_ENTRY = BASE + "homes/shared_home_entry.ui";

  // === Warps ===
  public static final String WARPS_PAGE = BASE + "warps/warps_page.ui";
  public static final String WARP_ENTRY = BASE + "warps/warp_entry.ui";
  public static final String WARP_CATEGORY_HEADER = BASE + "warps/warp_category_header.ui";

  // === Kits ===
  public static final String KITS_PAGE = BASE + "kits/kits_page.ui";
  public static final String KIT_ENTRY = BASE + "kits/kit_entry.ui";
  public static final String KIT_PREVIEW_PAGE = BASE + "kits/kit_preview_page.ui";
  public static final String KIT_PREVIEW_ITEM = BASE + "kits/kit_preview_item.ui";

  // === Player Settings ===
  public static final String PLAYER_SETTINGS = BASE + "player/player_settings.ui";

  // === Teleport ===
  public static final String TPA_PAGE = BASE + "teleport/tpa_page.ui";
  public static final String TPA_ENTRY = BASE + "teleport/tpa_entry.ui";

  // === Back ===
  public static final String BACK_PAGE = BASE + "back/back_page.ui";
  public static final String BACK_ENTRY = BASE + "back/back_entry.ui";

  // === Admin ===
  public static final String ADMIN_DASHBOARD = BASE + "admin/admin_dashboard.ui";
  public static final String ADMIN_MODULE_CARD = BASE + "admin/admin_module_card.ui";
  public static final String ADMIN_WARPS = BASE + "admin/admin_warps.ui";
  public static final String ADMIN_WARP_ENTRY = BASE + "admin/admin_warp_entry.ui";
  public static final String ADMIN_WARP_EDIT = BASE + "admin/admin_warp_edit.ui";
  public static final String ADMIN_SPAWNS = BASE + "admin/admin_spawns.ui";
  public static final String ADMIN_SPAWN_ENTRY = BASE + "admin/admin_spawn_entry.ui";
  public static final String ADMIN_KITS = BASE + "admin/admin_kits.ui";
  public static final String ADMIN_KIT_ENTRY = BASE + "admin/admin_kit_entry.ui";
  public static final String ADMIN_KIT_PREVIEW = BASE + "admin/admin_kit_preview.ui";
  public static final String ADMIN_KIT_EDIT = BASE + "admin/admin_kit_edit.ui";
  public static final String ADMIN_KIT_CREATE = BASE + "admin/admin_kit_create.ui";
  public static final String ADMIN_PLAYERS = BASE + "admin/admin_players.ui";
  public static final String ADMIN_PLAYER_ENTRY = BASE + "admin/admin_player_entry.ui";
  public static final String ADMIN_PLAYER_DETAIL = BASE + "admin/admin_player_detail.ui";
  public static final String ADMIN_MODERATION = BASE + "admin/admin_moderation.ui";
  public static final String ADMIN_PUNISHMENT_ENTRY = BASE + "admin/admin_punishment_entry.ui";
  public static final String ADMIN_PLAYER_MODERATION = BASE + "admin/admin_player_moderation.ui";
  public static final String ADMIN_PLAYER_MOD_ENTRY = BASE + "admin/admin_player_mod_entry.ui";
  public static final String ADMIN_PUNISHMENT_ACTION = BASE + "admin/admin_punishment_action.ui";
  public static final String ADMIN_ANNOUNCEMENTS = BASE + "admin/admin_announcements.ui";
  public static final String ADMIN_ANNOUNCEMENT_ENTRY = BASE + "admin/admin_announcement_entry.ui";
  public static final String ADMIN_ANNOUNCEMENT_EDIT = BASE + "admin/admin_announcement_edit.ui";
  public static final String ADMIN_MODULE_TOGGLE = BASE + "admin/admin_module_toggle.ui";
  public static final String ADMIN_BACKUPS = BASE + "admin/admin_backups.ui";
  public static final String ADMIN_BACKUP_ENTRY = BASE + "admin/admin_backup_entry.ui";
  public static final String ADMIN_CONFIG = BASE + "admin/admin_config.ui";
  public static final String ADMIN_CONFIG_ENTRY = BASE + "admin/admin_config_entry.ui";
  public static final String ADMIN_PERMISSION_ADD = BASE + "admin/admin_permission_add.ui";
  public static final String ADMIN_PERM_ROLE_ENTRY = BASE + "admin/admin_perm_role_entry.ui";
  public static final String ADMIN_PLAYER_DETAIL_HEADER = BASE + "admin/admin_player_detail_header.ui";
  public static final String ADMIN_KIT_PREVIEW_ITEM = BASE + "admin/admin_kit_preview_item.ui";
  public static final String ADMIN_CREATE_MODAL = BASE + "admin/admin_create_modal.ui";

  // === Admin Config Editor (tabbed) ===
  public static final String ADMIN_CONFIG_STANDARD = BASE + "admin/admin_config_standard.ui";
  public static final String ADMIN_CONFIG_NARROW = BASE + "admin/admin_config_narrow.ui";
  public static final String ADMIN_CONFIG_SECTION = BASE + "admin/admin_config_section.ui";
  public static final String ADMIN_CONFIG_BOOL_ROW = BASE + "admin/admin_config_bool_row.ui";
  public static final String ADMIN_CONFIG_NUM_ROW = BASE + "admin/admin_config_num_row.ui";
  public static final String ADMIN_CONFIG_STR_ROW = BASE + "admin/admin_config_str_row.ui";
  public static final String ADMIN_CONFIG_COLOR_ROW = BASE + "admin/admin_config_color_row.ui";
  public static final String ADMIN_CONFIG_ENUM_ROW = BASE + "admin/admin_config_enum_row.ui";

  public static final String ADMIN_ANNOUNCEMENT_HEADER = BASE + "admin/admin_announcement_header.ui";
  public static final String ADMIN_ANNOUNCEMENT_EVENTS_HEADER = BASE + "admin/admin_announcement_events_header.ui";
  public static final String ADMIN_ANNOUNCEMENT_EVENT_TOGGLE = BASE + "admin/admin_announcement_event_toggle.ui";
  public static final String ADMIN_ANNOUNCEMENT_EVENT_ENTRY = BASE + "admin/admin_announcement_event_entry.ui";
  public static final String ADMIN_PLAYER_MOD_ACTIONS = BASE + "admin/admin_player_mod_actions.ui";

  // === Admin Updates ===
  public static final String ADMIN_UPDATES = BASE + "admin/admin_updates.ui";
}
