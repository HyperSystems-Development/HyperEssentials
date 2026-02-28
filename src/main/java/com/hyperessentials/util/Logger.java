package com.hyperessentials.util;

import com.hypixel.hytale.logger.HytaleLogger;
import java.util.EnumSet;
import java.util.logging.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Wrapped logger with category-based debug logging.
 * Uses Hytale's HytaleLogger (Google Flogger) for proper log routing.
 */
public final class Logger {

  private static final String PREFIX = "";

  private static HytaleLogger logger;

  /**
   * Debug categories for category-based debug logging.
   */
  public enum DebugCategory {
    HOMES("homes"),
    WARPS("warps"),
    SPAWNS("spawns"),
    TELEPORT("teleport"),
    KITS("kits"),
    MODERATION("moderation"),
    UTILITY("utility"),
    RTP("rtp"),
    ANNOUNCEMENTS("announcements"),
    INTEGRATION("integration"),
    ECONOMY("economy"),
    STORAGE("storage");

    private final String configKey;

    DebugCategory(String configKey) {
      this.configKey = configKey;
    }

    /** Returns the config key. */
    public String getConfigKey() {
      return configKey;
    }
  }

  // Volatile flags for thread-safety
  private static volatile EnumSet<DebugCategory> enabledCategories = EnumSet.noneOf(DebugCategory.class);

  private static volatile boolean verboseMode = false;

  private static volatile boolean logToConsole = true;

  private Logger() {}

  /**
   * Initializes the logger with the plugin's HytaleLogger.
   *
   * @param parentLogger the HytaleLogger from the plugin
   */
  public static void init(@NotNull HytaleLogger parentLogger) {
    logger = parentLogger;
  }

  // === Standard Logging ===

  /** Logs an info message. */
  public static void info(@NotNull String message) {
    if (logger != null) {
      logger.at(Level.INFO).log("%s", PREFIX + message);
    } else {
      System.out.println(PREFIX + "[INFO] " + message);
    }
  }

  /** Logs an info message with formatting. */
  public static void info(@NotNull String message, Object... args) {
    info(String.format(message, args));
  }

  /** Logs a warning message. */
  public static void warn(@NotNull String message) {
    if (logger != null) {
      logger.at(Level.WARNING).log("%s", PREFIX + message);
    } else {
      System.out.println(PREFIX + "[WARN] " + message);
    }
  }

  /** Logs a warning message with formatting. */
  public static void warn(@NotNull String message, Object... args) {
    warn(String.format(message, args));
  }

  /** Logs a severe error message. */
  public static void severe(@NotNull String message) {
    if (logger != null) {
      logger.at(Level.SEVERE).log("%s", PREFIX + message);
    } else {
      System.err.println(PREFIX + "[SEVERE] " + message);
    }
  }

  /** Logs a severe error message with formatting. */
  public static void severe(@NotNull String message, Object... args) {
    severe(String.format(message, args));
  }

  /** Logs a severe error with exception. */
  public static void severe(@NotNull String message, @NotNull Throwable throwable, Object... args) {
    String formatted = String.format(message, args);
    if (logger != null) {
      logger.at(Level.SEVERE).withCause(throwable).log("%s", PREFIX + formatted);
    } else {
      System.err.println(PREFIX + "[SEVERE] " + formatted);
      throwable.printStackTrace();
    }
  }

  /** Logs a debug message (only if debug is enabled). */
  public static void debug(@NotNull String message) {
    if (logger != null) {
      logger.at(Level.FINE).log("%s", PREFIX + "[DEBUG] " + message);
    }
  }

  /** Logs a debug message with formatting. */
  public static void debug(@NotNull String message, Object... args) {
    debug(String.format(message, args));
  }

  // === Category-Based Debug Logging ===

  /** Enables or disables a specific debug category. */
  public static void setDebugEnabled(@NotNull DebugCategory category, boolean enabled) {
    EnumSet<DebugCategory> newSet = EnumSet.copyOf(enabledCategories);
    if (enabled) {
      newSet.add(category);
    } else {
      newSet.remove(category);
    }
    enabledCategories = newSet;
  }

  /** Checks if a debug category is enabled. */
  public static boolean isDebugEnabled(@NotNull DebugCategory category) {
    return enabledCategories.contains(category);
  }

  /** Enables all debug categories. */
  public static void enableAll() {
    enabledCategories = EnumSet.allOf(DebugCategory.class);
    info("[Debug] All debug categories enabled");
  }

  /** Disables all debug categories. */
  public static void disableAll() {
    enabledCategories = EnumSet.noneOf(DebugCategory.class);
    info("[Debug] All debug categories disabled");
  }

  /** Gets the currently enabled categories. */
  public static EnumSet<DebugCategory> getEnabledCategories() {
    return EnumSet.copyOf(enabledCategories);
  }

  /** Sets verbose mode for extra detailed output. */
  public static void setVerboseMode(boolean enabled) {
    verboseMode = enabled;
  }

  /** Checks if verbose mode is enabled. */
  public static boolean isVerboseMode() {
    return verboseMode;
  }

  /** Sets whether debug output goes to console. */
  public static void setLogToConsole(boolean enabled) {
    logToConsole = enabled;
  }

  // === Category-Specific Debug Methods ===

  /** Logs a homes-related debug message. */
  public static void debugHomes(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.HOMES)) {
      logDebug("HOMES", message, args);
    }
  }

  /** Logs a warps-related debug message. */
  public static void debugWarps(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.WARPS)) {
      logDebug("WARPS", message, args);
    }
  }

  /** Logs a spawns-related debug message. */
  public static void debugSpawns(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.SPAWNS)) {
      logDebug("SPAWNS", message, args);
    }
  }

  /** Logs a teleport-related debug message. */
  public static void debugTeleport(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.TELEPORT)) {
      logDebug("TELEPORT", message, args);
    }
  }

  /** Logs a kits-related debug message. */
  public static void debugKits(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.KITS)) {
      logDebug("KITS", message, args);
    }
  }

  /** Logs a moderation-related debug message. */
  public static void debugModeration(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.MODERATION)) {
      logDebug("MODERATION", message, args);
    }
  }

  /** Logs a utility-related debug message. */
  public static void debugUtility(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.UTILITY)) {
      logDebug("UTILITY", message, args);
    }
  }

  /** Logs an RTP-related debug message. */
  public static void debugRtp(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.RTP)) {
      logDebug("RTP", message, args);
    }
  }

  /** Logs an announcements-related debug message. */
  public static void debugAnnouncements(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.ANNOUNCEMENTS)) {
      logDebug("ANNOUNCEMENTS", message, args);
    }
  }

  /** Logs an integration-related debug message. */
  public static void debugIntegration(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.INTEGRATION)) {
      logDebug("INTEGRATION", message, args);
    }
  }

  /** Logs an economy-related debug message. */
  public static void debugEconomy(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.ECONOMY)) {
      logDebug("ECONOMY", message, args);
    }
  }

  /** Logs a storage-related debug message. */
  public static void debugStorage(@NotNull String message, Object... args) {
    if (isDebugEnabled(DebugCategory.STORAGE)) {
      logDebug("STORAGE", message, args);
    }
  }

  /** Internal method to log a categorized debug message. */
  private static void logDebug(@NotNull String category, @NotNull String message, Object... args) {
    String formatted = args.length > 0 ? String.format(message, args) : message;
    String logMessage = PREFIX + "[DEBUG:" + category + "] " + formatted;

    if (logToConsole) {
      if (logger != null) {
        logger.at(Level.INFO).log("%s", logMessage); // Use INFO level for visibility
      } else {
        System.out.println(logMessage);
      }
    } else if (logger != null) {
      logger.at(Level.FINE).log("%s", logMessage);
    }
  }

  /** Logs a debug message with verbose details if verbose mode is enabled. */
  public static void debugVerbose(@NotNull DebugCategory category, @NotNull String message,
                                  @NotNull String verboseDetails, Object... args) {
    if (!isDebugEnabled(category)) {
      return;
    }

    String formatted = args.length > 0 ? String.format(message, args) : message;
    logDebug(category.name(), formatted);

    if (verboseMode) {
      String verboseFormatted = args.length > 0 ? String.format(verboseDetails, args) : verboseDetails;
      logDebug(category.name() + ":VERBOSE", verboseFormatted);
    }
  }
}
