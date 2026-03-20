package com.hyperessentials.module.moderation;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.data.Location;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages freeze state for players. Session-only (not persisted).
 * Frozen players are teleported back to their freeze location if they move.
 */
public class FreezeManager {

  private final Map<UUID, Location> frozenPlayers = new ConcurrentHashMap<>();
  private ScheduledFuture<?> checkerTask;

  public void start() {
    int intervalMs = ConfigManager.get().moderation().getFreezeCheckIntervalMs();
    checkerTask = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
      this::checkFrozenPlayers, intervalMs, intervalMs, TimeUnit.MILLISECONDS
    );
    Logger.debug("[FreezeManager] Movement checker started (interval: %dms)", intervalMs);
  }

  public void shutdown() {
    if (checkerTask != null) {
      checkerTask.cancel(false);
      checkerTask = null;
    }
    frozenPlayers.clear();
  }

  public void freeze(@NotNull UUID playerUuid, @NotNull Location location) {
    frozenPlayers.put(playerUuid, location);
  }

  public void unfreeze(@NotNull UUID playerUuid) {
    frozenPlayers.remove(playerUuid);
  }

  public boolean isFrozen(@NotNull UUID playerUuid) {
    return frozenPlayers.containsKey(playerUuid);
  }

  public void onPlayerDisconnect(@NotNull UUID playerUuid) {
    frozenPlayers.remove(playerUuid);
  }

  private void checkFrozenPlayers() {
    if (frozenPlayers.isEmpty()) return;

    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    for (Map.Entry<UUID, Location> entry : frozenPlayers.entrySet()) {
      try {
        PlayerRef playerRef = plugin.getTrackedPlayer(entry.getKey());
        if (playerRef == null) {
          frozenPlayers.remove(entry.getKey());
          continue;
        }

        Location frozenLoc = entry.getValue();
        Transform transform = playerRef.getTransform();
        Vector3d pos = transform.getPosition();
        double px = pos.getX();
        double py = pos.getY();
        double pz = pos.getZ();

        double distSq = Math.pow(px - frozenLoc.x(), 2)
          + Math.pow(py - frozenLoc.y(), 2)
          + Math.pow(pz - frozenLoc.z(), 2);

        if (distSq > 0.25) {
          // Teleport player back to frozen location using Teleport ECS component
          try {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null) {
              Store<EntityStore> store = ref.getStore();
              if (store != null) {
                Vector3d targetPos = new Vector3d(frozenLoc.x(), frozenLoc.y(), frozenLoc.z());
                Vector3f targetRot = new Vector3f(frozenLoc.pitch(), frozenLoc.yaw(), 0);
                // Use same-world Teleport constructor (no World param)
                store.addComponent(ref, Teleport.getComponentType(),
                  new Teleport(targetPos, targetRot));
              }
            }
          } catch (Exception e) {
            ErrorHandler.report("[Freeze] Teleport failed for frozen player", e);
          }
        }
      } catch (Exception e) {
        ErrorHandler.report("[Freeze] Error checking frozen player", e);
      }
    }
  }
}
