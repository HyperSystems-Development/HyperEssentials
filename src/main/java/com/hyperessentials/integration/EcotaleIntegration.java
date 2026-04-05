package com.hyperessentials.integration;

import com.hyperessentials.util.Logger;

/**
 * Detects Ecotale economy plugin availability via reflection.
 */
public final class EcotaleIntegration {

  private static boolean available = false;
  private static boolean checked = false;

  private EcotaleIntegration() {}

  /** Attempts to detect Ecotale via reflection. */
  public static void init() {
    if (checked) {
      return;
    }
    checked = true;

    try {
      Class.forName("com.ecotale.Ecotale");
      available = true;
      Logger.info("[Integration] Ecotale detected");
    } catch (ClassNotFoundException e) {
      available = false;
      Logger.debug("[Integration] Ecotale not detected");
    }
  }

  /** Returns true if Ecotale is available on the server. */
  public static boolean isAvailable() { return available; }
}
