package com.hyperessentials.gui;

import com.hyperessentials.Permissions;
import com.hyperessentials.integration.PermissionManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Utility for opening admin GUI pages by page ID.
 */
public final class AdminPageOpener {

  private AdminPageOpener() {}

  /**
   * Opens an admin page by ID. Requires admin GUI permission.
   *
   * @return true if the page was opened successfully
   */
  public static boolean open(
      @NotNull String pageId,
      @NotNull Player player,
      @NotNull Ref<EntityStore> ref,
      @NotNull Store<EntityStore> store,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager
  ) {
    if (!PermissionManager.get().hasPermission(playerRef.getUuid(), Permissions.ADMIN_GUI)) {
      return false;
    }

    PageRegistry.Entry entry = guiManager.getAdminRegistry().getEntry(pageId);
    if (entry == null) {
      return false;
    }

    if (entry.permission() != null
        && !PermissionManager.get().hasPermission(playerRef.getUuid(), entry.permission())) {
      return false;
    }

    InteractiveCustomUIPage<?> page = entry.supplier().create(
        player, ref, store, playerRef, guiManager
    );

    if (page != null) {
      player.getPageManager().openCustomPage(ref, store, page);
      return true;
    }

    return false;
  }
}
