package com.hyperessentials.gui;

import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which GUI page each player currently has open.
 * Thread-safe: uses ConcurrentHashMap.
 */
public class ActivePageTracker {

  public record ActivePageInfo(
      @NotNull String pageId,
      @NotNull InteractiveCustomUIPage<?> page
  ) {}

  private final ConcurrentHashMap<UUID, ActivePageInfo> activePlayers = new ConcurrentHashMap<>();

  public void register(@NotNull UUID playerUuid, @NotNull String pageId,
             @NotNull InteractiveCustomUIPage<?> page) {
    activePlayers.put(playerUuid, new ActivePageInfo(pageId, page));
  }

  public void unregister(@NotNull UUID playerUuid) {
    activePlayers.remove(playerUuid);
  }

  @Nullable
  public ActivePageInfo get(@NotNull UUID playerUuid) {
    return activePlayers.get(playerUuid);
  }

  @NotNull
  public List<UUID> getPlayersOnPage(@NotNull String pageId) {
    List<UUID> result = new ArrayList<>();
    for (Map.Entry<UUID, ActivePageInfo> entry : activePlayers.entrySet()) {
      if (entry.getValue().pageId().equals(pageId)) {
        result.add(entry.getKey());
      }
    }
    return result;
  }

  public void clear() {
    activePlayers.clear();
  }
}
