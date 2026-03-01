package com.hyperessentials.storage;

import com.google.gson.*;
import com.hyperessentials.data.PlayerStats;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON persistence for player statistics (playtime, first join).
 * File: data/playerstats.json
 */
public class PlayerStatsStorage {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  private final Path filePath;
  private final Map<UUID, PlayerStats> stats = new ConcurrentHashMap<>();

  public PlayerStatsStorage(@NotNull Path dataDir) {
    this.filePath = dataDir.resolve("data").resolve("playerstats.json");
  }

  public void load() {
    stats.clear();

    if (!Files.exists(filePath)) {
      Logger.info("[PlayerStatsStorage] No stats file found, starting fresh");
      return;
    }

    try {
      String json = Files.readString(filePath);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();

      if (root.has("players") && root.get("players").isJsonObject()) {
        JsonObject players = root.getAsJsonObject("players");
        for (Map.Entry<String, JsonElement> entry : players.entrySet()) {
          try {
            UUID uuid = UUID.fromString(entry.getKey());
            JsonObject obj = entry.getValue().getAsJsonObject();
            PlayerStats ps = new PlayerStats(
              uuid,
              obj.get("username").getAsString(),
              Instant.ofEpochMilli(obj.get("firstJoin").getAsLong()),
              obj.get("totalPlaytimeMs").getAsLong(),
              Instant.ofEpochMilli(obj.get("lastJoin").getAsLong())
            );
            stats.put(uuid, ps);
          } catch (Exception e) {
            Logger.warn("[PlayerStatsStorage] Failed to parse entry: %s", e.getMessage());
          }
        }
      }

      Logger.info("[PlayerStatsStorage] Loaded stats for %d player(s)", stats.size());
    } catch (Exception e) {
      Logger.severe("[PlayerStatsStorage] Failed to load: %s", e.getMessage());
    }
  }

  public synchronized void save() {
    try {
      Files.createDirectories(filePath.getParent());

      JsonObject root = new JsonObject();
      JsonObject players = new JsonObject();

      for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
        PlayerStats ps = entry.getValue();
        JsonObject obj = new JsonObject();
        obj.addProperty("username", ps.username());
        obj.addProperty("firstJoin", ps.firstJoin().toEpochMilli());
        obj.addProperty("totalPlaytimeMs", ps.totalPlaytimeMs());
        obj.addProperty("lastJoin", ps.lastJoin().toEpochMilli());
        players.add(entry.getKey().toString(), obj);
      }

      root.add("players", players);
      Files.writeString(filePath, GSON.toJson(root));
      Logger.debug("[PlayerStatsStorage] Saved stats");
    } catch (IOException e) {
      Logger.severe("[PlayerStatsStorage] Failed to save: %s", e.getMessage());
    }
  }

  @Nullable
  public PlayerStats getStats(@NotNull UUID uuid) {
    return stats.get(uuid);
  }

  public void updateStats(@NotNull PlayerStats playerStats) {
    stats.put(playerStats.uuid(), playerStats);
    save();
  }
}
