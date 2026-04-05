package com.hyperessentials.gui;

/**
 * Interface for GUI pages that support real-time content refresh.
 */
public interface RefreshablePage {

  /**
   * Refreshes the page content with current data (full rebuild).
   * Used for structural changes (add, delete, etc.).
   */
  void refreshContent();

  /**
   * Lightweight cooldown-only refresh. Updates timer text and button states
   * without rebuilding page structure or re-binding events.
   * Default falls back to full refresh.
   */
  default void refreshCooldowns() {
    refreshContent();
  }
}
