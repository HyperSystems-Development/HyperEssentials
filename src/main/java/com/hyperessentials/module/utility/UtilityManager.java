package com.hyperessentials.module.utility;

import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.UtilityConfig;
import com.hyperessentials.data.PlayerStats;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.storage.PlayerStatsStorage;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages session-only states for utility commands (fly, god, AFK, stamina)
 * and persistent player stats (playtime, join date).
 */
public class UtilityManager {

  private final Set<UUID> flyingPlayers = ConcurrentHashMap.newKeySet();
  private final Set<UUID> godPlayers = ConcurrentHashMap.newKeySet();
  private final Set<UUID> afkPlayers = ConcurrentHashMap.newKeySet();
  private final Set<UUID> infiniteStaminaPlayers = ConcurrentHashMap.newKeySet();
  private final Map<UUID, Instant> lastActivityTimes = new ConcurrentHashMap<>();
  private final Map<UUID, Instant> sessionStartTimes = new ConcurrentHashMap<>();
  private final Map<UUID, double[]> lastKnownPositions = new ConcurrentHashMap<>();

  private PlayerStatsStorage statsStorage;
  private ScheduledExecutorService scheduler;

  /**
   * Initializes stats storage and starts the periodic task scheduler.
   */
  public void init(@NotNull Path dataDir) {
    statsStorage = new PlayerStatsStorage(dataDir);
    statsStorage.load();

    scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "HyperEssentials-Utility");
      t.setDaemon(true);
      return t;
    });

    // Periodic task: AFK idle check + stamina enforcement (every 1 second)
    scheduler.scheduleAtFixedRate(this::periodicTask, 1, 1, TimeUnit.SECONDS);
  }

  private void periodicTask() {
    try {
      checkMovement();
      checkAfkTimeout();
      enforceInfiniteStamina();
    } catch (Exception e) {
      Logger.debug("[Utility] Periodic task error: %s", e.getMessage());
    }
  }

  /**
   * Checks if tracked players have moved since the last check.
   * Hytale has no PlayerMoveEvent, so we poll position changes to detect WASD movement.
   */
  private void checkMovement() {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    for (Map.Entry<UUID, Instant> entry : lastActivityTimes.entrySet()) {
      UUID uuid = entry.getKey();
      PlayerRef player = plugin.getTrackedPlayer(uuid);
      if (player == null) continue;

      try {
        var pos = player.getTransform().getPosition();
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        double[] last = lastKnownPositions.get(uuid);
        if (last != null && (last[0] != x || last[1] != y || last[2] != z)) {
          onPlayerActivity(uuid);
        }
        lastKnownPositions.put(uuid, new double[]{x, y, z});
      } catch (Exception ignored) {}
    }
  }

  // === Fly ===

  public boolean isFlying(@NotNull UUID uuid) {
    return flyingPlayers.contains(uuid);
  }

  public boolean toggleFly(@NotNull UUID uuid) {
    if (flyingPlayers.contains(uuid)) {
      flyingPlayers.remove(uuid);
      return false;
    } else {
      flyingPlayers.add(uuid);
      return true;
    }
  }

  public void setFlying(@NotNull UUID uuid, boolean flying) {
    if (flying) flyingPlayers.add(uuid);
    else flyingPlayers.remove(uuid);
  }

  // === God ===

  public boolean isGod(@NotNull UUID uuid) {
    return godPlayers.contains(uuid);
  }

  public boolean toggleGod(@NotNull UUID uuid) {
    if (godPlayers.contains(uuid)) {
      godPlayers.remove(uuid);
      return false;
    } else {
      godPlayers.add(uuid);
      return true;
    }
  }

  public void setGod(@NotNull UUID uuid, boolean god) {
    if (god) godPlayers.add(uuid);
    else godPlayers.remove(uuid);
  }

  // === AFK ===

  public boolean isAfk(@NotNull UUID uuid) {
    return afkPlayers.contains(uuid);
  }

  public boolean toggleAfk(@NotNull UUID uuid) {
    if (afkPlayers.contains(uuid)) {
      afkPlayers.remove(uuid);
      return false;
    } else {
      afkPlayers.add(uuid);
      return true;
    }
  }

  /**
   * Records player activity and auto-unsets AFK if needed.
   * Called from event listeners on chat, interact, mouse motion, etc.
   */
  public void onPlayerActivity(@NotNull UUID uuid) {
    lastActivityTimes.put(uuid, Instant.now());
    if (afkPlayers.remove(uuid)) {
      broadcastAfkStatus(uuid, false);
    }
  }

  private void checkAfkTimeout() {
    UtilityConfig config = ConfigManager.get().utility();
    int timeout = config.getAfkTimeoutSeconds();
    if (timeout <= 0) return;

    Instant now = Instant.now();
    for (Map.Entry<UUID, Instant> entry : lastActivityTimes.entrySet()) {
      UUID uuid = entry.getKey();
      if (!afkPlayers.contains(uuid)) {
        Instant last = entry.getValue();
        if (last != null && Duration.between(last, now).getSeconds() >= timeout) {
          afkPlayers.add(uuid);
          broadcastAfkStatus(uuid, true);
        }
      }
    }
  }

  private void broadcastAfkStatus(@NotNull UUID uuid, boolean nowAfk) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    PlayerRef player = plugin.getTrackedPlayer(uuid);
    String name = player != null ? player.getUsername() : uuid.toString();
    String text = name + (nowAfk ? " is now AFK" : " is no longer AFK");
    Message msg = CommandUtil.msg(text, CommandUtil.COLOR_GRAY);

    for (PlayerRef p : plugin.getTrackedPlayers().values()) {
      p.sendMessage(msg);
    }
  }

  // === Infinite Stamina ===

  public boolean isInfiniteStamina(@NotNull UUID uuid) {
    return infiniteStaminaPlayers.contains(uuid);
  }

  public boolean toggleInfiniteStamina(@NotNull UUID uuid) {
    if (infiniteStaminaPlayers.contains(uuid)) {
      infiniteStaminaPlayers.remove(uuid);
      return false;
    } else {
      infiniteStaminaPlayers.add(uuid);
      return true;
    }
  }

  private void enforceInfiniteStamina() {
    if (infiniteStaminaPlayers.isEmpty()) return;

    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    for (UUID uuid : infiniteStaminaPlayers) {
      PlayerRef player = plugin.getTrackedPlayer(uuid);
      if (player == null) continue;

      try {
        UUID worldUuid = player.getWorldUuid();
        if (worldUuid == null) continue;

        World world = Universe.get().getWorld(worldUuid);
        if (world == null) continue;

        // Dispatch to the world thread — PlayerRef.getComponent() requires it
        world.execute(() -> {
          try {
            var statMap = player.getComponent(
              com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule.get().getEntityStatMapComponentType());
            if (statMap != null) {
              statMap.maximizeStatValue(
                com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getStamina());
            }
          } catch (Exception e) {
            Logger.debug("[Utility] Stamina enforcement failed for %s: %s", uuid, e.getMessage());
          }
        });
      } catch (Exception e) {
        Logger.debug("[Utility] Stamina enforcement failed for %s: %s", uuid, e.getMessage());
      }
    }
  }

  // === Player Stats ===

  public void onPlayerConnect(@NotNull UUID uuid, @NotNull String username) {
    sessionStartTimes.put(uuid, Instant.now());
    lastActivityTimes.put(uuid, Instant.now());

    if (statsStorage != null) {
      PlayerStats existing = statsStorage.getStats(uuid);
      Instant now = Instant.now();
      if (existing == null) {
        statsStorage.updateStats(new PlayerStats(uuid, username, now, 0L, now));
      } else {
        statsStorage.updateStats(new PlayerStats(uuid, username, existing.firstJoin(), existing.totalPlaytimeMs(), now));
      }
    }
  }

  public void onPlayerDisconnect(@NotNull UUID uuid) {
    flyingPlayers.remove(uuid);
    godPlayers.remove(uuid);
    afkPlayers.remove(uuid);
    infiniteStaminaPlayers.remove(uuid);
    lastActivityTimes.remove(uuid);
    lastKnownPositions.remove(uuid);

    // Accumulate session playtime
    Instant sessionStart = sessionStartTimes.remove(uuid);
    if (sessionStart != null && statsStorage != null) {
      long sessionMs = Duration.between(sessionStart, Instant.now()).toMillis();
      PlayerStats existing = statsStorage.getStats(uuid);
      if (existing != null) {
        statsStorage.updateStats(new PlayerStats(
          uuid, existing.username(), existing.firstJoin(),
          existing.totalPlaytimeMs() + sessionMs, existing.lastJoin()
        ));
      }
    }
  }

  @Nullable
  public PlayerStats getPlayerStats(@NotNull UUID uuid) {
    return statsStorage != null ? statsStorage.getStats(uuid) : null;
  }

  /**
   * Gets total playtime including the current session.
   */
  public long getTotalPlaytimeMs(@NotNull UUID uuid) {
    PlayerStats stats = getPlayerStats(uuid);
    long total = stats != null ? stats.totalPlaytimeMs() : 0L;

    Instant sessionStart = sessionStartTimes.get(uuid);
    if (sessionStart != null) {
      total += Duration.between(sessionStart, Instant.now()).toMillis();
    }
    return total;
  }

  /**
   * Gets the session start time for a player.
   */
  @Nullable
  public Instant getSessionStart(@NotNull UUID uuid) {
    return sessionStartTimes.get(uuid);
  }

  @Nullable
  public PlayerStatsStorage getStatsStorage() {
    return statsStorage;
  }

  // === Cleanup ===

  public void shutdown() {
    // Save all active session playtimes
    if (statsStorage != null) {
      for (Map.Entry<UUID, Instant> entry : sessionStartTimes.entrySet()) {
        UUID uuid = entry.getKey();
        Instant sessionStart = entry.getValue();
        long sessionMs = Duration.between(sessionStart, Instant.now()).toMillis();
        PlayerStats existing = statsStorage.getStats(uuid);
        if (existing != null) {
          statsStorage.updateStats(new PlayerStats(
            uuid, existing.username(), existing.firstJoin(),
            existing.totalPlaytimeMs() + sessionMs, existing.lastJoin()
          ));
        }
      }
    }

    if (scheduler != null && !scheduler.isShutdown()) {
      scheduler.shutdown();
    }

    flyingPlayers.clear();
    godPlayers.clear();
    afkPlayers.clear();
    infiniteStaminaPlayers.clear();
    lastActivityTimes.clear();
    lastKnownPositions.clear();
    sessionStartTimes.clear();
  }
}
