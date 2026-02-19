package com.hyperessentials.gui;

/**
 * Interface for GUI pages that support real-time content refresh.
 */
public interface RefreshablePage {

    /**
     * Refreshes the page content with current data.
     * Must be called on the world thread.
     */
    void refreshContent();
}
