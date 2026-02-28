package com.hyperessentials.module.moderation;

import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages vanish state for players. Session-only (not persisted).
 * Uses HiddenPlayersManager to hide vanished players from others.
 */
public class VanishManager {

  private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();

  public boolean isVanished(@NotNull UUID playerUuid) {
    return vanishedPlayers.contains(playerUuid);
  }

  /**
   * Toggles vanish for a player.
   *
   * @return true if now vanished, false if un-vanished
   */
  public boolean toggleVanish(@NotNull UUID playerUuid, @NotNull PlayerRef playerRef) {
    if (vanishedPlayers.contains(playerUuid)) {
      unvanish(playerUuid, playerRef);
      return false;
    } else {
      vanish(playerUuid, playerRef);
      return true;
    }
  }

  public void vanish(@NotNull UUID playerUuid, @NotNull PlayerRef playerRef) {
    vanishedPlayers.add(playerUuid);
    hideFromAll(playerUuid);

    if (ConfigManager.get().vanish().isFakeLeaveMessage()) {
      broadcastFakeMessage(playerRef.getUsername() + " left the game.");
    }

    Logger.debug("[VanishManager] %s vanished", playerRef.getUsername());
  }

  public void unvanish(@NotNull UUID playerUuid, @NotNull PlayerRef playerRef) {
    vanishedPlayers.remove(playerUuid);
    showToAll(playerUuid);

    if (ConfigManager.get().vanish().isFakeJoinMessage()) {
      broadcastFakeMessage(playerRef.getUsername() + " joined the game.");
    }

    Logger.debug("[VanishManager] %s un-vanished", playerRef.getUsername());
  }

  /**
   * Called when a new player connects. Hides all vanished players from them.
   */
  public void onPlayerConnect(@NotNull UUID newPlayerUuid, @NotNull PlayerRef newPlayerRef) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    for (UUID vanishedUuid : vanishedPlayers) {
      try {
        newPlayerRef.getHiddenPlayersManager().hidePlayer(vanishedUuid);
      } catch (Exception e) {
        Logger.debug("[VanishManager] Failed to hide %s from new player: %s", vanishedUuid, e.getMessage());
      }
    }
  }

  /**
   * Called when a player disconnects. Removes them from vanish if vanished.
   */
  public void onPlayerDisconnect(@NotNull UUID playerUuid) {
    vanishedPlayers.remove(playerUuid);
  }

  @NotNull
  public Set<UUID> getVanishedPlayers() {
    return Set.copyOf(vanishedPlayers);
  }

  public void shutdown() {
    // Show all vanished players before shutdown
    for (UUID uuid : vanishedPlayers) {
      showToAll(uuid);
    }
    vanishedPlayers.clear();
  }

  private void hideFromAll(@NotNull UUID vanishedUuid) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    for (Map.Entry<UUID, PlayerRef> entry : plugin.getTrackedPlayers().entrySet()) {
      if (!entry.getKey().equals(vanishedUuid)) {
        try {
          entry.getValue().getHiddenPlayersManager().hidePlayer(vanishedUuid);
        } catch (Exception e) {
          Logger.debug("[VanishManager] Failed to hide from player: %s", e.getMessage());
        }
      }
    }
  }

  private void showToAll(@NotNull UUID vanishedUuid) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    for (Map.Entry<UUID, PlayerRef> entry : plugin.getTrackedPlayers().entrySet()) {
      if (!entry.getKey().equals(vanishedUuid)) {
        try {
          entry.getValue().getHiddenPlayersManager().showPlayer(vanishedUuid);
        } catch (Exception e) {
          Logger.debug("[VanishManager] Failed to show to player: %s", e.getMessage());
        }
      }
    }
  }

  private void broadcastFakeMessage(@NotNull String text) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    Message msg = Message.raw(text).color(CommandUtil.COLOR_YELLOW);
    for (PlayerRef player : plugin.getTrackedPlayers().values()) {
      player.sendMessage(msg);
    }
  }
}
