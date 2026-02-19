package com.hyperessentials.gui;

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
     * Clears all registries and trackers. Used during shutdown.
     */
    public void shutdown() {
        playerRegistry.clear();
        adminRegistry.clear();
        pageTracker.clear();
    }
}
