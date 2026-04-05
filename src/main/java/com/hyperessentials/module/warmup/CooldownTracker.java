package com.hyperessentials.module.warmup;

import com.hyperessentials.config.ConfigManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player per-action cooldowns.
 */
public class CooldownTracker {

  private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

  private String key(@NotNull UUID playerUuid, @NotNull String moduleName, @NotNull String commandName) {
    return playerUuid + ":" + moduleName + ":" + commandName;
  }

  public void setCooldown(@NotNull UUID playerUuid, @NotNull String moduleName, @NotNull String commandName) {
    int cooldownSeconds = ConfigManager.get().warmup().getCooldown(moduleName);
    if (cooldownSeconds > 0) {
      cooldowns.put(key(playerUuid, moduleName, commandName),
          System.currentTimeMillis() + (cooldownSeconds * 1000L));
    }
  }

  public boolean isOnCooldown(@NotNull UUID playerUuid, @NotNull String moduleName, @NotNull String commandName) {
    Long expiry = cooldowns.get(key(playerUuid, moduleName, commandName));
    if (expiry == null) return false;
    if (System.currentTimeMillis() >= expiry) {
      cooldowns.remove(key(playerUuid, moduleName, commandName));
      return false;
    }
    return true;
  }

  public int getRemainingCooldown(@NotNull UUID playerUuid, @NotNull String moduleName, @NotNull String commandName) {
    Long expiry = cooldowns.get(key(playerUuid, moduleName, commandName));
    if (expiry == null) return 0;
    long remaining = expiry - System.currentTimeMillis();
    if (remaining <= 0) {
      cooldowns.remove(key(playerUuid, moduleName, commandName));
      return 0;
    }
    return (int) Math.ceil(remaining / 1000.0);
  }

  public void clear() {
    cooldowns.clear();
  }
}
