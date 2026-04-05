package com.hyperessentials.gui.util;

import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utility for searching online players by partial name match.
 */
public final class PlayerSearchUtil {

  public record SearchResult(@NotNull UUID uuid, @NotNull String username) {}

  private PlayerSearchUtil() {}

  /**
   * Searches online players by partial name match (case-insensitive).
   * Excludes the searching player from results.
   * Exact matches are sorted first, then alphabetical.
   */
  @NotNull
  public static List<SearchResult> search(@NotNull String query, @NotNull UUID excludeSelf) {
    if (query.isBlank()) return List.of();

    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return List.of();

    String lowerQuery = query.toLowerCase();
    Map<UUID, PlayerRef> tracked = plugin.getTrackedPlayers();

    List<SearchResult> exact = new ArrayList<>();
    List<SearchResult> partial = new ArrayList<>();

    for (PlayerRef ref : tracked.values()) {
      if (ref.getUuid().equals(excludeSelf)) continue;

      String name = ref.getUsername();
      String lowerName = name.toLowerCase();

      if (lowerName.equals(lowerQuery)) {
        exact.add(new SearchResult(ref.getUuid(), name));
      } else if (lowerName.contains(lowerQuery)) {
        partial.add(new SearchResult(ref.getUuid(), name));
      }
    }

    partial.sort((a, b) -> a.username().compareToIgnoreCase(b.username()));

    List<SearchResult> results = new ArrayList<>(exact.size() + partial.size());
    results.addAll(exact);
    results.addAll(partial);
    return results;
  }
}
