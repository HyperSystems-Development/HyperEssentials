package com.hyperessentials.gui;

/**
 * Centralized GUI color constants for HyperEssentials.
 * Matches HyperFactions visual language with gold brand accent.
 */
public final class GuiColors {

  private GuiColors() {}

  // === Brand ===
  public static final String BRAND_GOLD = "#FFAA00";
  public static final String BRAND_GOLD_LIGHT = "#FFD700";
  public static final String BRAND_GOLD_DARK = "#CC8800";

  // === Text ===
  public static final String TEXT_PRIMARY = "#cccccc";
  public static final String TEXT_HEADING = "#ffffff";
  public static final String TEXT_MUTED = "#888888";
  public static final String TEXT_LABEL = "#7c8b99";

  // === Status ===
  public static final String STATUS_ONLINE = "#55FF55";
  public static final String STATUS_OFFLINE = "#888888";
  public static final String STATUS_ACTIVE = "#55FF55";
  public static final String STATUS_INACTIVE = "#FF5555";

  // === Semantic ===
  public static final String SUCCESS = "#44cc44";
  public static final String DANGER = "#ff5555";
  public static final String WARNING = "#FFAA00";
  public static final String INFO = "#55FFFF";

  // === Backgrounds ===
  public static final String BG_DARK = "#0a1119";
  public static final String BG_PANEL = "#141c26";
  public static final String BG_CARD = "#1a2a3a";
  public static final String BG_NAV = "#16212f";

  // === Dividers ===
  public static final String DIVIDER = "#FFAA00";
  public static final String DIVIDER_SUBTLE = "#2a3a4a";

  // === Module status ===
  public static String forModuleEnabled(boolean enabled) {
    return enabled ? STATUS_ACTIVE : STATUS_INACTIVE;
  }

  // === Online status ===
  public static String forOnlineStatus(boolean online) {
    return online ? STATUS_ONLINE : STATUS_OFFLINE;
  }
}
