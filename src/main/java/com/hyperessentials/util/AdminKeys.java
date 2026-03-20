package com.hyperessentials.util;

/**
 * Static constants for HyperEssentials admin GUI i18n message keys.
 *
 * <p>
 * Contains all user-facing strings from admin GUI pages (dashboard, warps,
 * spawns, kits, players, moderation, announcements, settings).
 *
 * <p>
 * Key format: {@code hyperessentials.admin.{page}.{element}}
 */
public final class AdminKeys {

  private AdminKeys() {}

  /** Admin navigation bar labels. */
  public static final class Nav {
    private static final String P = "hyperessentials.admin.nav.";
    public static final String TITLE = P + "title";

    private Nav() {}
  }

  /** Admin dashboard page. */
  public static final class Dashboard {
    private static final String P = "hyperessentials.admin.dashboard.";
    public static final String MODULE_ENABLED = P + "module_enabled";
    public static final String MODULE_DISABLED = P + "module_disabled";

    private Dashboard() {}
  }

  /** Admin players page. */
  public static final class Players {
    private static final String P = "hyperessentials.admin.players.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String PLAYER_INFO = P + "player_info";

    private Players() {}
  }

  /** Admin warps page. */
  public static final class Warps {
    private static final String P = "hyperessentials.admin.warps.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";

    private Warps() {}
  }

  /** Admin spawns page. */
  public static final class Spawns {
    private static final String P = "hyperessentials.admin.spawns.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String GLOBAL_BADGE = P + "global_badge";

    private Spawns() {}
  }

  /** Admin kits page. */
  public static final class Kits {
    private static final String P = "hyperessentials.admin.kits.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String ONE_TIME = P + "one_time";

    private Kits() {}
  }

  /** Admin moderation page. */
  public static final class Moderation {
    private static final String P = "hyperessentials.admin.mod.";
    public static final String EMPTY_ACTIVE_TITLE = P + "empty_active_title";
    public static final String EMPTY_ALL_TITLE = P + "empty_all_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String NO_REASON = P + "no_reason";
    public static final String PERMANENT = P + "permanent";
    public static final String EXPIRED = P + "expired";
    public static final String REVOKED = P + "revoked";

    private Moderation() {}
  }

  /** Admin announcements page. */
  public static final class Announcements {
    private static final String P = "hyperessentials.admin.announce.";
    public static final String INTERVAL_LABEL = P + "interval_label";
    public static final String DISABLED = P + "disabled";
    public static final String MODE_RANDOM = P + "mode_random";
    public static final String MODE_SEQUENTIAL = P + "mode_sequential";

    private Announcements() {}
  }

  /** Admin settings page. */
  public static final class Settings {
    private static final String P = "hyperessentials.admin.settings.";
    public static final String DATA_DIR = P + "data_dir";
    public static final String MODULE_ENABLED = P + "module_enabled";
    public static final String MODULE_DISABLED = P + "module_disabled";

    private Settings() {}
  }
}
