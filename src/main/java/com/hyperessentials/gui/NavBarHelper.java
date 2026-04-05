package com.hyperessentials.gui;

import com.hyperessentials.integration.PermissionManager;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.GuiKeys;
import com.hyperessentials.util.HEMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Helper class for building and handling the shared navigation bar.
 */
public final class NavBarHelper {

  private NavBarHelper() {}

  /**
   * Sets up the navigation bar in a page.
   */
  public static void setupBar(
      @NotNull PlayerRef playerRef,
      @NotNull String currentPage,
      @NotNull PageRegistry registry,
      @NotNull UICommandBuilder cmd,
      @NotNull UIEventBuilder events
  ) {
    List<PageRegistry.Entry> entries = registry.getAccessibleNavBarEntries(playerRef);

    if (entries.isEmpty()) {
      return;
    }

    cmd.set("#NavBar #NavBarTitle #NavBarTitleLabel.Text", HEMessages.get(playerRef, GuiKeys.Nav.TITLE));
    cmd.appendInline("#NavBar #NavBarButtons", "Group #NavCards { LayoutMode: Left; }");
    buildButtons(entries, cmd, events);

    // Flex spacer pushes "Player" button to far right
    cmd.appendInline("#NavBar #NavBarButtons", "Group { FlexWeight: 1; }");

    // "Player" button on far right
    cmd.append("#NavBar #NavBarButtons", UIPaths.NAV_BUTTON);
    cmd.set("#NavBar #NavBarButtons[2] #NavActionButton.Text",
        HEMessages.get(playerRef, GuiKeys.Nav.PLAYER_MENU));
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#NavBar #NavBarButtons[2] #NavActionButton",
        EventData.of("Button", "Nav").append("NavTarget", "player_settings"),
        false
    );
  }

  /**
   * Sets up the admin navigation bar in a page.
   */
  public static void setupAdminBar(
      @NotNull PlayerRef playerRef,
      @NotNull String currentPage,
      @NotNull PageRegistry registry,
      @NotNull UICommandBuilder cmd,
      @NotNull UIEventBuilder events
  ) {
    List<PageRegistry.Entry> entries = registry.getAccessibleNavBarEntries(playerRef);

    if (entries.isEmpty()) {
      return;
    }

    cmd.set("#NavBar #NavBarTitle #NavBarTitleLabel.Text", HEMessages.get(playerRef, AdminKeys.Nav.TITLE));
    cmd.appendInline("#NavBar #NavBarButtons", "Group #NavCards { LayoutMode: Left; }");
    buildButtons(entries, cmd, events);
  }

  /**
   * Builds navigation buttons using a single template for all entries.
   * Follows HyperFactions NavBarUtil pattern: one template, same element ID.
   */
  private static void buildButtons(
      @NotNull List<PageRegistry.Entry> entries,
      @NotNull UICommandBuilder cmd,
      @NotNull UIEventBuilder events
  ) {
    int index = 0;
    for (PageRegistry.Entry entry : entries) {
      cmd.append("#NavCards", UIPaths.NAV_BUTTON);
      cmd.set("#NavCards[" + index + "] #NavActionButton.Text", entry.displayName());

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#NavCards[" + index + "] #NavActionButton",
          EventData.of("Button", "Nav").append("NavTarget", entry.id()),
          false
      );

      index++;
    }
  }

  /**
   * Handles navigation events from the player nav bar.
   *
   * @param targetId   The target page ID from event data
   * @param player     The player entity
   * @param ref        Entity reference
   * @param store      Entity store
   * @param playerRef  Player reference
   * @param guiManager The GUI manager
   * @return true if the event was handled
   */
  public static boolean handleNavEvent(
      @NotNull String targetId,
      @NotNull Player player,
      @NotNull Ref<EntityStore> ref,
      @NotNull Store<EntityStore> store,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager
  ) {
    return handleNavEvent(targetId, player, ref, store, playerRef, guiManager, GuiType.PLAYER);
  }

  /**
   * Handles navigation events from nav bar with explicit GUI type.
   *
   * @param targetId   The target page ID from event data
   * @param player     The player entity
   * @param ref        Entity reference
   * @param store      Entity store
   * @param playerRef  Player reference
   * @param guiManager The GUI manager
   * @param guiType    Whether this is a player or admin nav bar
   * @return true if the event was handled
   */
  public static boolean handleNavEvent(
      @NotNull String targetId,
      @NotNull Player player,
      @NotNull Ref<EntityStore> ref,
      @NotNull Store<EntityStore> store,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager,
      @NotNull GuiType guiType
  ) {
    if (targetId.isEmpty()) {
      return false;
    }

    PageRegistry registry = guiType == GuiType.ADMIN
        ? guiManager.getAdminRegistry()
        : guiManager.getPlayerRegistry();

    PageRegistry.Entry entry = registry.getEntry(targetId);
    if (entry == null) {
      return true;
    }

    if (entry.permission() != null && !PermissionManager.get().hasPermission(playerRef.getUuid(), entry.permission())) {
      return true;
    }

    InteractiveCustomUIPage<?> page = entry.supplier().create(
        player, ref, store, playerRef, guiManager
    );

    if (page != null) {
      // Unregister current page from tracker before opening new page,
      // preventing stale refresh ticks from sending commands to a replaced page
      guiManager.getPageTracker().unregister(playerRef.getUuid());
      player.getPageManager().openCustomPage(ref, store, page);
    }

    return true;
  }
}
