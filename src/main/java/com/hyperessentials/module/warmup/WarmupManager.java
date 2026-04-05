package com.hyperessentials.module.warmup;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Global warmup/cooldown tracking for all modules.
 */
public class WarmupManager {

  private final Map<UUID, WarmupTask> activeWarmups = new ConcurrentHashMap<>();
  private final Map<UUID, ScheduledFuture<?>> warmupFutures = new ConcurrentHashMap<>();
  private final CooldownTracker cooldownTracker = new CooldownTracker();
  private final ScheduledExecutorService scheduler =
    Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "HyperEssentials-Warmup");
      t.setDaemon(true);
      return t;
    });

  /**
   * Starts a warmup for a player action.
   *
   * @param playerUuid the player's UUID
   * @param moduleName the module name
   * @param commandName the command/action name
   * @param callback   the callback to run when warmup completes
   * @return the warmup task, or null if no warmup needed (bypass or 0 duration)
   */
  @Nullable
  public WarmupTask startWarmup(@NotNull UUID playerUuid, @NotNull String moduleName,
                   @NotNull String commandName, @NotNull Runnable callback) {
    // Bypass warmup entirely if player has the permission
    if (CommandUtil.hasPermission(playerUuid, Permissions.BYPASS_WARMUP)) {
      Logger.debug("[Warmup] Bypassing warmup for %s (has bypass permission)", playerUuid);
      callback.run();
      return null;
    }

    int warmupSeconds = ConfigManager.get().warmup().getWarmup(moduleName);

    if (warmupSeconds <= 0) {
      callback.run();
      return null;
    }

    // Cancel any existing warmup for this player
    cancelWarmup(playerUuid);

    WarmupTask task = new WarmupTask(playerUuid, moduleName, commandName, warmupSeconds, callback);
    activeWarmups.put(playerUuid, task);

    // Schedule the completion after the warmup period
    ScheduledFuture<?> future = scheduler.schedule(
      () -> completeWarmup(playerUuid),
      warmupSeconds, TimeUnit.SECONDS
    );
    warmupFutures.put(playerUuid, future);

    Logger.debug("[Warmup] Started %ds warmup for %s (%s/%s)", warmupSeconds, playerUuid, moduleName, commandName);
    return task;
  }

  /**
   * Cancels a player's active warmup.
   */
  public boolean cancelWarmup(@NotNull UUID playerUuid) {
    WarmupTask task = activeWarmups.remove(playerUuid);
    ScheduledFuture<?> future = warmupFutures.remove(playerUuid);
    if (future != null) {
      future.cancel(false);
    }
    if (task != null) {
      Logger.debug("[Warmup] Cancelled warmup for %s", playerUuid);
      return true;
    }
    return false;
  }

  /**
   * Completes a warmup (called when timer expires).
   */
  public void completeWarmup(@NotNull UUID playerUuid) {
    WarmupTask task = activeWarmups.remove(playerUuid);
    warmupFutures.remove(playerUuid);
    if (task != null) {
      Logger.debug("[Warmup] Completed warmup for %s (%s/%s)", playerUuid, task.moduleName(), task.commandName());
      task.callback().run();
      cooldownTracker.setCooldown(playerUuid, task.moduleName(), task.commandName());
    }
  }

  /**
   * Checks if a player has an active warmup.
   */
  public boolean hasActiveWarmup(@NotNull UUID playerUuid) {
    return activeWarmups.containsKey(playerUuid);
  }

  /**
   * Checks if a player is on cooldown.
   */
  public boolean isOnCooldown(@NotNull UUID playerUuid, @NotNull String moduleName, @NotNull String commandName) {
    if (CommandUtil.hasPermission(playerUuid, Permissions.BYPASS_COOLDOWN)) {
      return false;
    }
    return cooldownTracker.isOnCooldown(playerUuid, moduleName, commandName);
  }

  /**
   * Gets remaining cooldown seconds.
   */
  public int getRemainingCooldown(@NotNull UUID playerUuid, @NotNull String moduleName, @NotNull String commandName) {
    return cooldownTracker.getRemainingCooldown(playerUuid, moduleName, commandName);
  }

  public void clear() {
    // Cancel all pending warmup futures
    warmupFutures.values().forEach(f -> f.cancel(false));
    warmupFutures.clear();
    activeWarmups.clear();
    cooldownTracker.clear();
  }

  /**
   * Shuts down the warmup scheduler. Call on plugin disable.
   */
  public void shutdown() {
    clear();
    scheduler.shutdown();
  }
}
