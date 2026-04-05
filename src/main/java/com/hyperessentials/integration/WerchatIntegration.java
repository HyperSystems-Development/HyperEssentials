package com.hyperessentials.integration;

import com.hyperessentials.util.Logger;

/**
 * Detects Werchat chat plugin availability via reflection.
 *
 * <p>When Werchat is present, HyperEssentials delegates chat formatting,
 * channel management, nickname handling, and chat moderation to it.
 */
public final class WerchatIntegration {

  private static boolean available = false;
  private static boolean checked = false;

  private WerchatIntegration() {}

  /**
   * Attempts to detect Werchat via reflection.
   * Safe to call multiple times — only checks once.
   */
  public static void init() {
    if (checked) {
      return;
    }
    checked = true;

    try {
      Class.forName("com.werchat.Werchat");
      available = true;
      Logger.info("[Integration] Werchat detected — chat handling delegated");
    } catch (ClassNotFoundException e) {
      available = false;
      Logger.debug("[Integration] Werchat not detected");
    }
  }

  /** Returns true if Werchat is loaded on the server. */
  public static boolean isAvailable() { return available; }

  /** Returns true if Werchat handles chat formatting (prefix/suffix, colors). */
  public static boolean handlesChatFormatting() { return available; }

  /** Returns true if Werchat handles chat channels. */
  public static boolean handlesChannels() { return available; }

  /** Returns true if Werchat handles player nicknames. */
  public static boolean handlesNicknames() { return available; }

  /** Returns true if Werchat handles chat moderation (mute, filter, cooldown). */
  public static boolean handlesModeration() { return available; }
}
