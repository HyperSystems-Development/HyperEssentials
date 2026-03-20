package com.hyperessentials.util;

/**
 * Static constants for HyperEssentials player GUI i18n message keys.
 *
 * <p>
 * Contains all user-facing strings from player GUI pages (dashboard, homes,
 * warps, kits, TPA, stats, back, kit preview, home sharing).
 *
 * <p>
 * Key format: {@code hyperessentials.gui.{page}.{element}}
 */
public final class GuiKeys {

  private GuiKeys() {}

  /** Navigation bar labels. */
  public static final class Nav {
    private static final String P = "hyperessentials.gui.nav.";
    public static final String TITLE = P + "title";

    private Nav() {}
  }

  /** Player dashboard page. */
  public static final class Dashboard {
    private static final String P = "hyperessentials.gui.dashboard.";
    public static final String WELCOME = P + "welcome";
    public static final String PLAYTIME = P + "playtime";
    public static final String FIRST_JOINED = P + "first_joined";

    private Dashboard() {}
  }

  /** Homes page. */
  public static final class Homes {
    private static final String P = "hyperessentials.gui.homes.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String ZONE_RESTRICTED = P + "zone_restricted";
    public static final String COOLDOWN = P + "cooldown";
    public static final String COOLDOWN_LABEL = P + "cooldown_label";

    private Homes() {}
  }

  /** Warps page. */
  public static final class Warps {
    private static final String P = "hyperessentials.gui.warps.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String ZONE_RESTRICTED = P + "zone_restricted";

    private Warps() {}
  }

  /** Kits page. */
  public static final class Kits {
    private static final String P = "hyperessentials.gui.kits.";
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
    private static final String P = "hyperessentials.gui.kit_preview.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String CLAIMED_BUTTON = P + "claimed_button";

    private KitPreview() {}
  }

  /** TPA page. */
  public static final class Tpa {
    private static final String P = "hyperessentials.gui.tpa.";
    // Placeholder for TPA page strings (populated when page has them)

    private Tpa() {}
  }

  /** Stats page. */
  public static final class Stats {
    private static final String P = "hyperessentials.gui.stats.";
    // Placeholder for Stats page strings

    private Stats() {}
  }

  /** Back locations page. */
  public static final class Back {
    private static final String P = "hyperessentials.gui.back.";
    public static final String EMPTY_TITLE = P + "empty_title";
    public static final String EMPTY_MESSAGE = P + "empty_message";
    public static final String COOLDOWN_BUTTON = P + "cooldown_button";

    private Back() {}
  }

  /** Home sharing page. */
  public static final class HomeShare {
    private static final String P = "hyperessentials.gui.home_share.";
    public static final String SHARE_TITLE = P + "share_title";
    public static final String SHARED_COUNT = P + "shared_count";

    private HomeShare() {}
  }
}
