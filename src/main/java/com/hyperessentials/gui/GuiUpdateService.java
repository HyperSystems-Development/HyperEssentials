package com.hyperessentials.gui;

import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.HytaleServer;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Bridges manager events to GUI page refreshes.
 * When a manager fires a change event, this service finds the relevant player's
 * open page and triggers a refresh.
 *
 * <p>Also runs a periodic 1-second tick that calls lightweight cooldown refreshes
 * on pages that display countdown timers. Unlike a full rebuild, the cooldown
 * tick only sends targeted text/state updates — no structure changes or event
 * re-binding — so it doesn't interfere with keyboard input.
 */
public class GuiUpdateService {

  /** Pages that display cooldown timers and need periodic lightweight refresh. */
  private static final Set<String> COOLDOWN_PAGES = Set.of("kits", "homes", "warps", "tpa");

  private final ActivePageTracker tracker;
  private volatile ScheduledFuture<?> cooldownTick;

  public GuiUpdateService(@NotNull ActivePageTracker tracker) {
    this.tracker = tracker;
  }

  /**
   * Starts the periodic cooldown refresh tick (every 1 second).
   */
  public void start() {
    if (cooldownTick != null) return;
    cooldownTick = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
        this::tickCooldowns, 1, 1, TimeUnit.SECONDS
    );
    Logger.debug("[GUI] Cooldown refresh tick started (1s interval)");
  }

  /**
   * Stops the periodic cooldown refresh tick.
   */
  public void shutdown() {
    if (cooldownTick != null) {
      cooldownTick.cancel(false);
      cooldownTick = null;
    }
  }

  /**
   * Periodic tick — calls lightweight cooldown refresh on open pages.
   */
  private void tickCooldowns() {
    for (String pageId : COOLDOWN_PAGES) {
      List<UUID> players = tracker.getPlayersOnPage(pageId);
      for (UUID uuid : players) {
        ActivePageTracker.ActivePageInfo info = tracker.get(uuid);
        if (info != null && info.page() instanceof RefreshablePage refreshable) {
          try {
            refreshable.refreshCooldowns();
          } catch (Exception e) {
            Logger.warn("[GUI] Error in cooldown tick for %s page (%s): %s",
                pageId, uuid, e.getMessage());
          }
        }
      }
    }
  }

  /**
   * Called when a player's homes change (create, delete, share).
   */
  public void onHomeChanged(@NotNull UUID playerUuid) {
    dispatchRefresh(playerUuid, "homes");
  }

  /**
   * Called when any warp is created, deleted, or updated.
   * Refreshes ALL players on the warps page.
   */
  public void onWarpChanged() {
    refreshAllOnPage("warps");
  }

  /**
   * Called when a player claims a kit.
   */
  public void onKitClaimed(@NotNull UUID playerUuid) {
    dispatchRefresh(playerUuid, "kits");
  }

  /**
   * Called when a TPA request changes (create, accept, deny, cancel, toggle).
   */
  public void onTpaRequestChanged(@NotNull UUID playerUuid) {
    dispatchRefresh(playerUuid, "tpa");
  }

  /**
   * Dispatches a full refresh to a specific player if they're on the given page.
   */
  private void dispatchRefresh(@NotNull UUID playerUuid, @NotNull String pageId) {
    ActivePageTracker.ActivePageInfo info = tracker.get(playerUuid);
    if (info == null || !info.pageId().equals(pageId)) return;

    if (info.page() instanceof RefreshablePage refreshable) {
      try {
        refreshable.refreshContent();
      } catch (Exception e) {
        Logger.warn("[GUI] Error refreshing %s page for %s: %s", pageId, playerUuid, e.getMessage());
      }
    }
  }

  /**
   * Full-refreshes all players who are currently viewing the given page.
   */
  private void refreshAllOnPage(@NotNull String pageId) {
    List<UUID> players = tracker.getPlayersOnPage(pageId);
    for (UUID uuid : players) {
      ActivePageTracker.ActivePageInfo info = tracker.get(uuid);
      if (info != null && info.page() instanceof RefreshablePage refreshable) {
        try {
          refreshable.refreshContent();
        } catch (Exception e) {
          Logger.warn("[GUI] Error refreshing %s page for %s: %s", pageId, uuid, e.getMessage());
        }
      }
    }
  }
}
