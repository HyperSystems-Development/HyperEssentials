package com.hyperessentials.module.warmup;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Global warmup/cooldown tracking for all modules.
 */
public class WarmupManager {

  private final Map<UUID, WarmupTask> activeWarmups = new ConcurrentHashMap<>();
  private final CooldownTracker cooldownTracker = new CooldownTracker();

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
    int warmupSeconds = ConfigManager.get().warmup().getWarmup(moduleName);

    if (warmupSeconds <= 0) {
      callback.run();
      return null;
    }

    WarmupTask task = new WarmupTask(playerUuid, moduleName, commandName, warmupSeconds, callback);
    activeWarmups.put(playerUuid, task);
    Logger.debug("[Warmup] Started %ds warmup for %s (%s/%s)", warmupSeconds, playerUuid, moduleName, commandName);
    return task;
  }

  /**
   * Cancels a player's active warmup.
   */
  public boolean cancelWarmup(@NotNull UUID playerUuid) {
    WarmupTask task = activeWarmups.remove(playerUuid);
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
    if (task != null) {
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
    return cooldownTracker.isOnCooldown(playerUuid, moduleName, commandName);
  }

  /**
   * Gets remaining cooldown seconds.
   */
  public int getRemainingCooldown(@NotNull UUID playerUuid, @NotNull String moduleName, @NotNull String commandName) {
    return cooldownTracker.getRemainingCooldown(playerUuid, moduleName, commandName);
  }

  public void clear() {
    activeWarmups.clear();
    cooldownTracker.clear();
  }
}
