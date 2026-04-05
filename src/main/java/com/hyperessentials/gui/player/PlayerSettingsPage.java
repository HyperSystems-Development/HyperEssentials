package com.hyperessentials.gui.player;

import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.PlayerSettingsData;
import com.hyperessentials.util.GuiKeys;
import com.hyperessentials.util.HEMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Player settings page — allows players to configure their language preference.
 * Mirrors the HyperFactions PlayerSettingsPage locale selector.
 */
public class PlayerSettingsPage extends InteractiveCustomUIPage<PlayerSettingsData> {

  private final Player player;
  private final PlayerRef playerRef;
  private final GuiManager guiManager;

  /** Current language preference: null = auto-detect. */
  @Nullable
  private String languagePreference;

  public PlayerSettingsPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerSettingsData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.guiManager = guiManager;

    // Load current preference from HEMessages override cache
    String current = HEMessages.getLanguageOverride(playerRef.getUuid());
    this.languagePreference = current;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.PLAYER_SETTINGS);
    NavBarHelper.setupBar(playerRef, "player_settings", guiManager.getPlayerRegistry(), cmd, events);
    buildContent(cmd, events);
  }

  private void buildContent(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    // Page title
    cmd.set("#PageTitle.Text", HEMessages.get(playerRef, GuiKeys.PlayerSettings.TITLE));

    // Language section
    cmd.set("#LanguageSectionTitle.Text", HEMessages.get(playerRef, GuiKeys.PlayerSettings.LANGUAGE_SECTION));
    cmd.set("#AutoDetectDesc.Text", HEMessages.get(playerRef, GuiKeys.PlayerSettings.AUTO_DETECT_DESC));
    cmd.set("#LanguageLabel.Text", HEMessages.get(playerRef, GuiKeys.PlayerSettings.LANGUAGE_LABEL));

    // Auto-detect checkbox
    boolean autoDetect = (languagePreference == null);
    cmd.set("#AutoDetectCB #CheckBox.Value", autoDetect);

    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#AutoDetectCB #CheckBox",
        EventData.of("Button", "ToggleAutoDetect"),
        false
    );

    // Language dropdown
    List<String> locales = HEMessages.getSupportedLocalesList();
    List<DropdownEntryInfo> localeEntries = new ArrayList<>();
    for (String code : locales) {
      localeEntries.add(new DropdownEntryInfo(
          LocalizableString.fromString(nativeDisplayName(code)), code));
    }
    cmd.set("#LanguageDropdown.Entries", localeEntries);

    String selectedLocale = (languagePreference != null && locales.contains(languagePreference))
        ? languagePreference : locales.get(0);
    cmd.set("#LanguageDropdown.Value", selectedLocale);

    cmd.set("#LanguageDropdown.Disabled", autoDetect);

    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged,
        "#LanguageDropdown",
        EventData.of("Button", "LanguageChanged")
            .append("@Language", "#LanguageDropdown.Value"),
        false
    );
  }

  @Override
  public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                              @NotNull PlayerSettingsData data) {
    super.handleDataEvent(ref, store, data);

    if ("Nav".equals(data.button)) {
      NavBarHelper.handleNavEvent(
          data.navTarget != null ? data.navTarget : "",
          player, ref, store, playerRef, guiManager, GuiType.PLAYER
      );
      return;
    }

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "ToggleAutoDetect" -> {
        if (languagePreference == null) {
          // Switching to manual — use current client language
          languagePreference = playerRef.getLanguage();
        } else {
          // Switching to auto-detect
          languagePreference = null;
        }
        HEMessages.setLanguageOverride(playerRef.getUuid(), languagePreference);
        rebuildContent();
      }
      case "LanguageChanged" -> {
        List<String> locales = HEMessages.getSupportedLocalesList();
        if (data.language != null && locales.contains(data.language)) {
          languagePreference = data.language;
          HEMessages.setLanguageOverride(playerRef.getUuid(), languagePreference);
        }
        rebuildContent();
      }
      default -> sendUpdate();
    }
  }

  private void rebuildContent() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildContent(cmd, events);
    sendUpdate(cmd, events, false);
  }

  /**
   * Returns a compact native display name for a locale code (e.g. "es-ES" -> "Espanol (ES)").
   */
  private static String nativeDisplayName(String localeCode) {
    Locale locale = Locale.forLanguageTag(localeCode);
    String lang = locale.getDisplayLanguage(locale);
    if (!lang.isEmpty()) {
      lang = Character.toUpperCase(lang.charAt(0)) + lang.substring(1);
    }
    String country = locale.getCountry();
    return country.isEmpty() ? lang : lang + " (" + country + ")";
  }
}
