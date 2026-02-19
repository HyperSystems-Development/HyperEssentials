package com.hyperessentials.gui;

import com.hyperessentials.integration.PermissionManager;
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

        cmd.set("#NavBar #NavBarTitle #NavBarTitleLabel.Text", "HyperEssentials");
        cmd.appendInline("#NavBar #NavBarButtons", "Group #NavCards { LayoutMode: Left; }");

        int index = 0;
        for (PageRegistry.Entry entry : entries) {
            if (entry.id().equals(currentPage)) {
                cmd.append("#NavCards", "HyperEssentials/shared/nav_button_active.ui");
            } else {
                cmd.append("#NavCards", "HyperEssentials/shared/nav_button.ui");
            }

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
     * Handles navigation events from the nav bar.
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
        if (targetId.isEmpty()) {
            return false;
        }

        PageRegistry.Entry entry = guiManager.getPlayerRegistry().getEntry(targetId);
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
            player.getPageManager().openCustomPage(ref, store, page);
        }

        return true;
    }
}
