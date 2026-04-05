package com.hyperessentials.gui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Central GUI manager for HyperEssentials.
 * Manages page registries for player and admin pages.
 */
public class GuiManager {

  private final PageRegistry playerRegistry = new PageRegistry();
  private final PageRegistry adminRegistry = new PageRegistry();
  private final ActivePageTracker pageTracker = new ActivePageTracker();

  /**
   * Gets the player page registry.
   */
  @NotNull
  public PageRegistry getPlayerRegistry() {
    return playerRegistry;
  }

  /**
   * Gets the admin page registry.
   */
  @NotNull
  public PageRegistry getAdminRegistry() {
    return adminRegistry;
  }

  /**
   * Gets the active page tracker.
   */
  @NotNull
  public ActivePageTracker getPageTracker() {
    return pageTracker;
  }

  /**
   * Opens a player page by ID.
   *
   * @return true if the page was opened successfully
   */
  public boolean openPlayerPage(
      @NotNull String pageId,
      @NotNull Player player,
      @NotNull Ref<EntityStore> ref,
      @NotNull Store<EntityStore> store,
      @NotNull PlayerRef playerRef
  ) {
    return PlayerPageOpener.open(pageId, player, ref, store, playerRef, this);
  }

  /**
   * Opens an admin page by ID.
   *
   * @return true if the page was opened successfully
   */
  public boolean openAdminPage(
      @NotNull String pageId,
      @NotNull Player player,
      @NotNull Ref<EntityStore> ref,
      @NotNull Store<EntityStore> store,
      @NotNull PlayerRef playerRef
  ) {
    return AdminPageOpener.open(pageId, player, ref, store, playerRef, this);
  }

  /**
   * Clears all registries and trackers. Used during shutdown.
   */
  public void shutdown() {
    playerRegistry.clear();
    adminRegistry.clear();
    pageTracker.clear();
  }
}
