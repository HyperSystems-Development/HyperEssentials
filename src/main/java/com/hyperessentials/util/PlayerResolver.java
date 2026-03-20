package com.hyperessentials.util;

import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Centralized player name resolution for HyperEssentials.
 *
 * <p>
 * Resolution chain: online players -> PlayerDB API.
 * Use {@link #resolve(String)} for single-result exact lookup (commands),
 * and {@link #search(String, UUID)} for multi-result partial matching (GUIs).
 */
public final class PlayerResolver {

  /**
   * Where the player was found.
   */
  public enum Source {
    ONLINE,
    PLAYER_DB
  }

  /**
   * A resolved player with UUID, properly-cased username, and resolution source.
   */
  public record ResolvedPlayer(
      @NotNull UUID uuid,
      @NotNull String username,
      @NotNull Source source
  ) {}

  private PlayerResolver() {}

  /**
   * Resolves a single player by exact username match.
   * Checks online players first, then falls back to PlayerDB API.
   *
   * @param name the player username (case-insensitive)
   * @return resolved player, or null if not found anywhere
   */
  @Nullable
  public static ResolvedPlayer resolve(@NotNull String name) {
    // 1. Check online players
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      for (PlayerRef ref : plugin.getTrackedPlayers().values()) {
        String username = ref.getUsername();
        if (username != null && username.equalsIgnoreCase(name)) {
          return new ResolvedPlayer(ref.getUuid(), username, Source.ONLINE);
        }
      }
    }

    // 2. Fall back to PlayerDB API
    var info = PlayerDBService.lookup(name).join();
    if (info != null) {
      return new ResolvedPlayer(info.uuid(), info.username(), Source.PLAYER_DB);
    }

    return null;
  }

  /**
   * Searches for players matching a partial query. Returns multiple results.
   * Searches online players (partial match), then tries PlayerDB (exact) if
   * no results found and query is long enough.
   *
   * <p>
   * Results are deduplicated by UUID and sorted (exact matches first, then alphabetical).
   *
   * @param query the search query (case-insensitive, partial match for online)
   * @param excludeUuid UUID to exclude from results (e.g., self), or null
   * @return list of matching players (may be empty)
   */
  @NotNull
  public static List<ResolvedPlayer> search(@NotNull String query,
                       @Nullable UUID excludeUuid) {
    List<ResolvedPlayer> results = new ArrayList<>();
    Set<UUID> seen = new HashSet<>();
    String queryLower = query.toLowerCase();

    // 1. Search online players (partial match)
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      for (PlayerRef ref : plugin.getTrackedPlayers().values()) {
        UUID uuid = ref.getUuid();
        if (uuid.equals(excludeUuid)) {
          continue;
        }
        if (seen.contains(uuid)) {
          continue;
        }

        String username = ref.getUsername();
        if (username != null && username.toLowerCase().contains(queryLower)) {
          seen.add(uuid);
          results.add(new ResolvedPlayer(uuid, username, Source.ONLINE));
        }
      }
    }

    // 2. If no results and query is long enough, try PlayerDB exact lookup
    if (results.isEmpty() && query.length() >= 3) {
      var info = PlayerDBService.lookup(query).join();
      if (info != null && !info.uuid().equals(excludeUuid)) {
        results.add(new ResolvedPlayer(info.uuid(), info.username(), Source.PLAYER_DB));
      }
    }

    // Sort: exact matches first, then alphabetical
    results.sort((a, b) -> {
      boolean aExact = a.username().equalsIgnoreCase(query);
      boolean bExact = b.username().equalsIgnoreCase(query);
      if (aExact != bExact) {
        return aExact ? -1 : 1;
      }
      return a.username().compareToIgnoreCase(b.username());
    });

    return results;
  }
}
