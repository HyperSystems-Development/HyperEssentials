package com.hyperessentials.integration.placeholder;

import com.hyperessentials.HyperEssentials;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * Soft dependency integration with PlaceholderAPI.
 *
 * <p>
 * Detects PlaceholderAPI at runtime and registers the HyperEssentials
 * expansion to expose essentials data as placeholders.
 *
 * <p>
 * Internal expansions must be manually registered since they are not
 * separate JARs in the expansions folder.
 */
public final class PlaceholderAPIIntegration {

  private static boolean available = false;

  @Nullable
  private static HyperEssentialsExpansion expansion;

  private PlaceholderAPIIntegration() {}

  /**
   * Initializes PlaceholderAPI integration.
   * Detects PAPI at runtime and registers the expansion.
   * Must be called after all managers are initialized.
   *
   * @param plugin the HyperEssentials instance
   */
  public static void init(HyperEssentials plugin) {
    try {
      Class.forName("at.helpch.placeholderapi.PlaceholderAPI");
    } catch (ClassNotFoundException e) {
      Logger.info("[Integration] PlaceholderAPI not found - placeholders disabled");
      return;
    }

    try {
      expansion = new HyperEssentialsExpansion(plugin);
      // Internal expansions must be manually registered
      boolean registered = expansion.register();
      if (registered) {
        available = true;
        Logger.info("[Integration] PlaceholderAPI expansion registered (%%essentials_*%%)");
      } else {
        Logger.warn("PlaceholderAPI expansion registration failed");
        expansion = null;
      }
    } catch (Exception e) {
      Logger.severe("Failed to register PlaceholderAPI expansion: %s", e.getMessage());
      expansion = null;
    }
  }

  /**
   * Shuts down PlaceholderAPI integration.
   * Unregisters the expansion.
   */
  public static void shutdown() {
    if (expansion != null) {
      try {
        expansion.unregister();
      } catch (Exception e) {
        Logger.debug("Failed to unregister PlaceholderAPI expansion: %s", e.getMessage());
      }
      expansion = null;
    }
    available = false;
  }

  /**
   * Checks if PlaceholderAPI is available and the expansion is registered.
   *
   * @return true if available
   */
  public static boolean isAvailable() {
    return available;
  }
}
