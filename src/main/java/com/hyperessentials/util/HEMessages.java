package com.hyperessentials.util;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.integration.HyperFactionsIntegration;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Centralized i18n message resolution for HyperEssentials.
 *
 * <p>
 * Uses Hytale's native {@link I18nModule} for translations.
 * Supports server-wide language and per-player client language.
 *
 * <p>
 * Language resolution order:
 * <ol>
 *   <li>Player's saved language preference (from PlayerData, cached in memory)</li>
 *   <li>Player's client language via {@link PlayerRef#getLanguage()} (if {@code usePlayerLanguage=true})</li>
 *   <li>Server default language from config</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>
 *   HEMessages.get(playerRef, CommandKeys.Home.NO_PERMISSION);
 *   HEMessages.get(playerRef, CommandKeys.Home.SET_SUCCESS, homeName);
 *   HEMessages.get(CommandKeys.Common.LOADING); // server language
 * </pre>
 */
public final class HEMessages {

  /** Per-player language overrides from PlayerData preferences. */
  private static final Map<UUID, String> languageOverrides = new ConcurrentHashMap<>();

  /** Authoritative list of supported locale codes. Single source of truth. */
  private static final List<String> SUPPORTED_LOCALES = List.of(
      "en-US", "es-ES", "de-DE", "fr-FR", "pt-BR",
      "ru-RU", "pl-PL", "it-IT", "nl-NL", "tl-PH"
  );

  /** Unmodifiable set view for the public API. */
  private static final Set<String> SUPPORTED_LOCALES_SET =
      Collections.unmodifiableSet(new LinkedHashSet<>(SUPPORTED_LOCALES));

  private HEMessages() {}

  /**
   * Sets a language override for a player.
   * Called when preferences are loaded from PlayerData on connect,
   * or when the player changes their language in settings.
   * Syncs the preference to HyperFactions if available.
   *
   * @param uuid     The player's UUID
   * @param language The language code, or null to clear the override (auto-detect)
   */
  public static void setLanguageOverride(@NotNull UUID uuid, @Nullable String language) {
    if (language == null) {
      languageOverrides.remove(uuid);
    } else {
      languageOverrides.put(uuid, language);
      // Sync to HyperFactions so both plugins share the same preference
      HyperFactionsIntegration.syncLanguage(uuid, language);
    }
  }

  /**
   * Clears the language override for a player.
   * Called on player disconnect.
   *
   * @param uuid The player's UUID
   */
  public static void clearLanguageOverride(@NotNull UUID uuid) {
    languageOverrides.remove(uuid);
  }

  /**
   * Gets a translated message for a specific player.
   * Uses the player's resolved language (preference -> client -> server default).
   *
   * @param player The player (null falls back to server language)
   * @param key    The full message key (e.g. "hyperessentials.cmd.home.no_permission")
   * @param args   Replacement arguments for {0}, {1}, etc.
   * @return Translated and formatted message, or the key itself if not found
   */
  @NotNull
  public static String get(@Nullable PlayerRef player, @NotNull String key, Object... args) {
    String lang = getLanguageFor(player);
    return getForLanguage(lang, key, args);
  }

  /**
   * Gets a translated message using the server default language.
   *
   * @param key  The full message key
   * @param args Replacement arguments for {0}, {1}, etc.
   * @return Translated and formatted message
   */
  @NotNull
  public static String get(@NotNull String key, Object... args) {
    return get((PlayerRef) null, key, args);
  }

  /**
   * Gets a translated message for a specific language code.
   *
   * @param language The language code (e.g. "en-US", "es-ES")
   * @param key      The full message key
   * @param args     Replacement arguments
   * @return Translated and formatted message
   */
  @NotNull
  public static String getForLanguage(@NotNull String language, @NotNull String key, Object... args) {
    I18nModule i18n = I18nModule.get();
    if (i18n == null) {
      return formatFallback(key, args);
    }

    String message = i18n.getMessage(language, key);
    if (message == null) {
      // Try fallback to en-US
      message = i18n.getMessage("en-US", key);
    }
    if (message == null) {
      // Key not found — return key itself for debugging
      return key;
    }

    return format(message, args);
  }

  /**
   * Determines the language to use for a player.
   *
   * <p>Resolution order:
   * <ol>
   *   <li>Player's saved language preference (from PlayerData, cached in memory)</li>
   *   <li>Player's client language (if {@code usePlayerLanguage} enabled in config)</li>
   *   <li>Server default language</li>
   * </ol>
   *
   * @param player The player (null returns server default)
   * @return The resolved language code
   */
  @NotNull
  public static String getLanguageFor(@Nullable PlayerRef player) {
    ConfigManager config = ConfigManager.get();
    String serverDefault = config.core().getDefaultLanguage();

    if (player == null) {
      return serverDefault;
    }

    // Check saved language preference first
    String override = languageOverrides.get(player.getUuid());
    if (override != null) {
      return override;
    }

    // Fallback: check if HyperFactions has a stored preference
    String hfLang = HyperFactionsIntegration.getHFLanguage(player.getUuid());
    if (hfLang != null && isLocaleSupported(hfLang)) {
      // Cache it locally so we don't query HF every time
      languageOverrides.put(player.getUuid(), hfLang);
      return hfLang;
    }

    // Use client language if enabled
    if (config.core().isUsePlayerLanguage()) {
      return player.getLanguage();
    }

    return serverDefault;
  }

  /**
   * Returns an unmodifiable set of the supported locale codes.
   *
   * @return set of locale codes (e.g. {"en-US", "pl-PL", "de-DE", ...})
   */
  @NotNull
  public static Set<String> getSupportedLocales() {
    return SUPPORTED_LOCALES_SET;
  }

  /**
   * Returns the ordered list of supported locale codes.
   * Used by GUI dropdown pages that need deterministic ordering.
   *
   * @return ordered list of locale codes
   */
  @NotNull
  public static List<String> getSupportedLocalesList() {
    return SUPPORTED_LOCALES;
  }

  /**
   * Checks if a locale code is supported.
   *
   * @param locale the locale code to check
   * @return true if supported
   */
  public static boolean isLocaleSupported(@NotNull String locale) {
    return SUPPORTED_LOCALES_SET.contains(locale);
  }

  /**
   * Gets the language preference for a player by UUID.
   * Checks the in-memory override map (set by API or loaded from PlayerData),
   * then falls back to server default language.
   *
   * <p>Note: This does NOT resolve the player's client language, only the
   * explicit preference. For full resolution including client language,
   * use {@link #getLanguageFor(PlayerRef)} with an active PlayerRef.
   *
   * @param uuid the player's UUID
   * @return the language preference or server default
   */
  @NotNull
  public static String getLanguageForUuid(@NotNull UUID uuid) {
    String override = languageOverrides.get(uuid);
    if (override != null) {
      return override;
    }
    return ConfigManager.get().core().getDefaultLanguage();
  }

  /**
   * Formats a message by replacing {0}, {1}, etc. with provided arguments.
   */
  @NotNull
  private static String format(@NotNull String message, Object... args) {
    if (args == null || args.length == 0) {
      return message;
    }

    String result = message;
    for (int i = 0; i < args.length; i++) {
      String placeholder = "{" + i + "}";
      String replacement = args[i] != null ? args[i].toString() : "";
      result = result.replace(placeholder, replacement);
    }
    return result;
  }

  /**
   * Fallback formatting when I18nModule is not available.
   */
  @NotNull
  private static String formatFallback(@NotNull String key, Object... args) {
    StringBuilder sb = new StringBuilder(key);
    if (args != null && args.length > 0) {
      sb.append(": ");
      for (Object arg : args) {
        sb.append(arg).append(" ");
      }
    }
    return sb.toString().trim();
  }
}
