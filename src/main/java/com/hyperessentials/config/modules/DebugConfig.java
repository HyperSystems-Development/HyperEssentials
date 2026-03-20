package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Configuration for the debug logging system.
 * Controls debug output by category, with integration into the Logger utility.
 */
public class DebugConfig extends ModuleConfig {

  // Global debug settings
  private boolean enabledByDefault = false;
  private boolean logToConsole = true;

  // Sentry error tracking settings
  private boolean sentryEnabled = true;
  private boolean sentryDebug = false;
  private String sentryEnvironment = "production";
  private double sentryTracesSampleRate = 0.0;

  // Per-category settings
  private boolean homes = false;
  private boolean warps = false;
  private boolean spawns = false;
  private boolean teleport = false;
  private boolean kits = false;
  private boolean moderation = false;
  private boolean utility = false;
  private boolean rtp = false;
  private boolean announcements = false;
  private boolean integration = false;
  private boolean economy = false;
  private boolean storage = false;

  public DebugConfig(@NotNull Path filePath) {
    super(filePath);
  }

  @Override
  @NotNull
  public String getModuleName() {
    return "debug";
  }

  @Override
  protected boolean getDefaultEnabled() {
    return false;
  }

  @Override
  protected void createDefaults() {
    enabled = false;
    enabledByDefault = false;
    logToConsole = true;
    sentryEnabled = true;
    sentryDebug = false;
    sentryEnvironment = "production";
    sentryTracesSampleRate = 0.0;
    homes = false;
    warps = false;
    spawns = false;
    teleport = false;
    kits = false;
    moderation = false;
    utility = false;
    rtp = false;
    announcements = false;
    integration = false;
    economy = false;
    storage = false;
  }

  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    enabledByDefault = getBool(root, "enabledByDefault", enabledByDefault);
    logToConsole = getBool(root, "logToConsole", logToConsole);

    if (hasSection(root, "sentry")) {
      JsonObject sentry = root.getAsJsonObject("sentry");
      sentryEnabled = getBool(sentry, "enabled", sentryEnabled);
      sentryEnvironment = getString(sentry, "environment", sentryEnvironment);
      sentryDebug = getBool(sentry, "debug", sentryDebug);
      sentryTracesSampleRate = getDouble(sentry, "tracesSampleRate", sentryTracesSampleRate);
    }

    if (hasSection(root, "categories")) {
      JsonObject categories = root.getAsJsonObject("categories");
      homes = getBool(categories, "homes", false);
      warps = getBool(categories, "warps", false);
      spawns = getBool(categories, "spawns", false);
      teleport = getBool(categories, "teleport", false);
      kits = getBool(categories, "kits", false);
      moderation = getBool(categories, "moderation", false);
      utility = getBool(categories, "utility", false);
      rtp = getBool(categories, "rtp", false);
      announcements = getBool(categories, "announcements", false);
      integration = getBool(categories, "integration", false);
      economy = getBool(categories, "economy", false);
      storage = getBool(categories, "storage", false);
    }

    applyToLogger();
  }

  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("enabledByDefault", enabledByDefault);
    root.addProperty("logToConsole", logToConsole);

    JsonObject sentry = new JsonObject();
    sentry.addProperty("enabled", sentryEnabled);
    sentry.addProperty("environment", sentryEnvironment);
    sentry.addProperty("debug", sentryDebug);
    sentry.addProperty("tracesSampleRate", sentryTracesSampleRate);
    root.add("sentry", sentry);

    JsonObject categories = new JsonObject();
    categories.addProperty("homes", homes);
    categories.addProperty("warps", warps);
    categories.addProperty("spawns", spawns);
    categories.addProperty("teleport", teleport);
    categories.addProperty("kits", kits);
    categories.addProperty("moderation", moderation);
    categories.addProperty("utility", utility);
    categories.addProperty("rtp", rtp);
    categories.addProperty("announcements", announcements);
    categories.addProperty("integration", integration);
    categories.addProperty("economy", economy);
    categories.addProperty("storage", storage);
    root.add("categories", categories);
  }

  /**
   * Applies the debug settings to the Logger utility.
   */
  public void applyToLogger() {
    Logger.setLogToConsole(logToConsole);
    Logger.setDebugEnabled(Logger.DebugCategory.HOMES, homes);
    Logger.setDebugEnabled(Logger.DebugCategory.WARPS, warps);
    Logger.setDebugEnabled(Logger.DebugCategory.SPAWNS, spawns);
    Logger.setDebugEnabled(Logger.DebugCategory.TELEPORT, teleport);
    Logger.setDebugEnabled(Logger.DebugCategory.KITS, kits);
    Logger.setDebugEnabled(Logger.DebugCategory.MODERATION, moderation);
    Logger.setDebugEnabled(Logger.DebugCategory.UTILITY, utility);
    Logger.setDebugEnabled(Logger.DebugCategory.RTP, rtp);
    Logger.setDebugEnabled(Logger.DebugCategory.ANNOUNCEMENTS, announcements);
    Logger.setDebugEnabled(Logger.DebugCategory.INTEGRATION, integration);
    Logger.setDebugEnabled(Logger.DebugCategory.ECONOMY, economy);
    Logger.setDebugEnabled(Logger.DebugCategory.STORAGE, storage);
  }

  // Getters
  public boolean isEnabledByDefault() { return enabledByDefault; }
  public boolean isLogToConsole() { return logToConsole; }
  public boolean isHomes() { return homes; }
  public boolean isWarps() { return warps; }
  public boolean isSpawns() { return spawns; }
  public boolean isTeleport() { return teleport; }
  public boolean isKits() { return kits; }
  public boolean isModeration() { return moderation; }
  public boolean isUtility() { return utility; }
  public boolean isRtp() { return rtp; }
  public boolean isAnnouncements() { return announcements; }
  public boolean isIntegration() { return integration; }
  public boolean isEconomy() { return economy; }
  public boolean isStorage() { return storage; }

  // Sentry getters
  public boolean isSentryEnabled() { return sentryEnabled; }
  public boolean isSentryDebug() { return sentryDebug; }
  public String getSentryEnvironment() { return sentryEnvironment; }
  public double getSentryTracesSampleRate() { return sentryTracesSampleRate; }

  // Setters (for admin config editor)
  public void setEnabledByDefault(boolean value) { this.enabledByDefault = value; }
  public void setLogToConsole(boolean value) { this.logToConsole = value; }
  public void setSentryEnabled(boolean value) { this.sentryEnabled = value; }
  public void setSentryDebug(boolean value) { this.sentryDebug = value; }
  public void setSentryTracesSampleRate(double value) { this.sentryTracesSampleRate = value; }
  public void setHomes(boolean value) { this.homes = value; }
  public void setWarps(boolean value) { this.warps = value; }
  public void setSpawns(boolean value) { this.spawns = value; }
  public void setTeleport(boolean value) { this.teleport = value; }
  public void setKits(boolean value) { this.kits = value; }
  public void setModeration(boolean value) { this.moderation = value; }
  public void setUtility(boolean value) { this.utility = value; }
  public void setRtp(boolean value) { this.rtp = value; }
  public void setAnnouncements(boolean value) { this.announcements = value; }
  public void setIntegration(boolean value) { this.integration = value; }
  public void setEconomy(boolean value) { this.economy = value; }
  public void setStorage(boolean value) { this.storage = value; }
}
