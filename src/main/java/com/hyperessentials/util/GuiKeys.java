package com.hyperessentials.util;

/**
 * Static constants for HyperEssentials player GUI i18n message keys.
 *
 * <p>
 * Contains all user-facing strings from player GUI pages (dashboard, homes,
 * warps, kits, TPA, stats, back, kit preview, home sharing).
 *
 * <p>
 * Key format: {@code hyperessentials_gui.{page}.{element}}
 */
public final class GuiKeys {

  private GuiKeys() {}

  /** Navigation bar labels. */
  public static final class Nav {
    private static final String P = "hyperessentials_gui.nav.";
    public static final String TITLE = P + "title";

    private Nav() {}
  }

  /** Player dashboard page. */
  public static final class Dashboard {
    private static final String P = "hyperessentials_gui.dashboard.";
    public static final String WELCOME = P + "welcome";
    public static final String PLAYTIME = P + "playtime";
    public static final String FIRST_JOINED = P + "first_joined";

    private Dashboard() {}
  }

  /** Homes page. */
  public static final class Homes {
    private static final String P = "hyperessentials_gui.homes.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String ZONE_RESTRICTED = P + "zone_restricted";
    public static final String COOLDOWN = P + "cooldown";
    public static final String COOLDOWN_LABEL = P + "cooldown_label";
    public static final String HOME_COUNT = P + "home_count";
    public static final String SHARED_BY = P + "shared_by";

    private Homes() {}
  }

  /** Warps page. */
  public static final class Warps {
    private static final String P = "hyperessentials_gui.warps.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String ZONE_RESTRICTED = P + "zone_restricted";

    private Warps() {}
  }

  /** Kits page. */
  public static final class Kits {
    private static final String P = "hyperessentials_gui.kits.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String COOLDOWN_LABEL = P + "cooldown_label";
    public static final String ALREADY_CLAIMED = P + "already_claimed";
    public static final String CLAIMED_BUTTON = P + "claimed_button";
    public static final String ONE_TIME = P + "one_time";
    public static final String READY = P + "ready";
    public static final String ZONE_RESTRICTED = P + "zone_restricted";

    private Kits() {}
  }

  /** Kit preview page. */
  public static final class KitPreview {
    private static final String P = "hyperessentials_gui.kit_preview.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String CLAIMED_BUTTON = P + "claimed_button";

    private KitPreview() {}
  }

  /** TPA page. */
  public static final class Tpa {
    private static final String P = "hyperessentials_gui.tpa.";
    public static final String TOGGLE_ENABLED = P + "toggle_enabled";
    public static final String TOGGLE_DISABLED = P + "toggle_disabled";
    public static final String TOGGLE_LABEL_ON = P + "toggle_label_on";
    public static final String TOGGLE_LABEL_OFF = P + "toggle_label_off";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String TYPE_TPA = P + "type_tpa";
    public static final String TYPE_TPAHERE = P + "type_tpahere";
    public static final String TIME_REMAINING = P + "time_remaining";
    public static final String REQUEST_COUNT = P + "request_count";
    public static final String REQUEST_COUNT_PLURAL = P + "request_count_plural";

    private Tpa() {}
  }

  /** Stats page. */
  public static final class Stats {
    private static final String P = "hyperessentials_gui.stats.";
    public static final String FIRST_JOINED = P + "first_joined";
    public static final String TOTAL_PLAYTIME = P + "total_playtime";
    public static final String CURRENT_SESSION = P + "current_session";

    private Stats() {}
  }

  /** Back locations page. */
  public static final class Back {
    private static final String P = "hyperessentials_gui.back.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String COOLDOWN_BUTTON = P + "cooldown_button";

    private Back() {}
  }

  /** Home sharing page. */
  public static final class HomeShare {
    private static final String P = "hyperessentials_gui.home_share.";
    public static final String SHARE_TITLE = P + "share_title";
    public static final String SHARED_COUNT = P + "shared_count";

    private HomeShare() {}
  }
}
