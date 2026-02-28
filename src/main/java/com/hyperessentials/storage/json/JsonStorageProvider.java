package com.hyperessentials.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperessentials.data.Location;
import com.hyperessentials.data.PlayerTeleportData;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.data.Warp;
import com.hyperessentials.storage.*;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * JSON file-based storage provider.
 */
public class JsonStorageProvider implements StorageProvider {

  private final Path dataDir;
  private final Path dataRoot;
  private final Path playersDir;
  private final Gson gson;
  private final JsonWarpStorage warpStorage;
  private final JsonSpawnStorage spawnStorage;
  private final JsonPlayerDataStorage playerDataStorage;

  public JsonStorageProvider(@NotNull Path dataDir) {
    this.dataDir = dataDir;
    this.dataRoot = dataDir.resolve("data");
    this.playersDir = dataRoot.resolve("players");
    this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    this.warpStorage = new JsonWarpStorage();
    this.spawnStorage = new JsonSpawnStorage();
    this.playerDataStorage = new JsonPlayerDataStorage();
  }

  @Override
  public CompletableFuture<Void> init() {
    return CompletableFuture.runAsync(() -> {
      try {
        Files.createDirectories(dataRoot);
        Files.createDirectories(playersDir);
        Logger.info("[Storage] JSON storage initialized at %s", dataRoot);
      } catch (IOException e) {
        throw new RuntimeException("Failed to create storage directories", e);
      }
    });
  }

  @Override
  public CompletableFuture<Void> shutdown() {
    Logger.info("[Storage] JSON storage provider shut down");
    return CompletableFuture.completedFuture(null);
  }

  @Override
  @NotNull
  public HomeStorage getHomeStorage() {
    // TODO: Return actual implementation when homes module is built
    return new HomeStorage() {
      @Override public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
      @Override public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }
    };
  }

  @Override
  @NotNull
  public WarpStorage getWarpStorage() {
    return warpStorage;
  }

  @Override
  @NotNull
  public SpawnStorage getSpawnStorage() {
    return spawnStorage;
  }

  @Override
  @NotNull
  public PlayerDataStorage getPlayerDataStorage() {
    return playerDataStorage;
  }

  // ========== Atomic write helper ==========

  private void atomicWrite(@NotNull Path target, @NotNull String content) throws IOException {
    Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
    Files.writeString(tmp, content);
    Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
  }

  // ========== Location serialization ==========

  private JsonObject serializeLocation(Location loc) {
    JsonObject obj = new JsonObject();
    obj.addProperty("world", loc.world());
    obj.addProperty("x", loc.x());
    obj.addProperty("y", loc.y());
    obj.addProperty("z", loc.z());
    obj.addProperty("yaw", loc.yaw());
    obj.addProperty("pitch", loc.pitch());
    return obj;
  }

  private Location deserializeLocation(JsonObject obj) {
    return new Location(
      obj.get("world").getAsString(),
      obj.get("x").getAsDouble(),
      obj.get("y").getAsDouble(),
      obj.get("z").getAsDouble(),
      obj.get("yaw").getAsFloat(),
      obj.get("pitch").getAsFloat()
    );
  }

  // ========== Warp Storage ==========

  private class JsonWarpStorage implements WarpStorage {

    private final Path warpsFile = dataRoot.resolve("warps.json");

    @Override
    public CompletableFuture<Void> init() {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Map<String, Warp>> loadWarps() {
      return CompletableFuture.supplyAsync(() -> {
        Map<String, Warp> warps = new HashMap<>();

        if (!Files.exists(warpsFile)) {
          return warps;
        }

        try {
          String json = Files.readString(warpsFile);
          JsonObject root = JsonParser.parseString(json).getAsJsonObject();

          if (root.has("warps") && root.get("warps").isJsonObject()) {
            JsonObject warpsObj = root.getAsJsonObject("warps");
            for (String name : warpsObj.keySet()) {
              JsonObject warpObj = warpsObj.getAsJsonObject(name);
              Warp warp = deserializeWarp(warpObj);
              warps.put(warp.name(), warp);
            }
          }

          Logger.info("[Storage] Loaded %d warps", warps.size());
        } catch (Exception e) {
          Logger.severe("[Storage] Failed to load warps: %s", e.getMessage());
        }

        return warps;
      });
    }

    @Override
    public CompletableFuture<Void> saveWarps(@NotNull Map<String, Warp> warps) {
      return CompletableFuture.runAsync(() -> {
        try {
          JsonObject root = new JsonObject();
          JsonObject warpsObj = new JsonObject();

          for (Warp warp : warps.values()) {
            warpsObj.add(warp.name(), serializeWarp(warp));
          }

          root.add("warps", warpsObj);
          atomicWrite(warpsFile, gson.toJson(root));
          Logger.debug("[Storage] Saved %d warps", warps.size());
        } catch (IOException e) {
          Logger.severe("[Storage] Failed to save warps: %s", e.getMessage());
        }
      });
    }

    private JsonObject serializeWarp(Warp warp) {
      JsonObject obj = new JsonObject();
      obj.addProperty("name", warp.name());
      obj.addProperty("displayName", warp.displayName());
      obj.addProperty("category", warp.category());
      obj.addProperty("world", warp.world());
      obj.addProperty("x", warp.x());
      obj.addProperty("y", warp.y());
      obj.addProperty("z", warp.z());
      obj.addProperty("yaw", warp.yaw());
      obj.addProperty("pitch", warp.pitch());
      if (warp.permission() != null) {
        obj.addProperty("permission", warp.permission());
      }
      if (warp.description() != null) {
        obj.addProperty("description", warp.description());
      }
      obj.addProperty("createdAt", warp.createdAt());
      if (warp.createdBy() != null) {
        obj.addProperty("createdBy", warp.createdBy());
      }
      return obj;
    }

    private Warp deserializeWarp(JsonObject obj) {
      return new Warp(
        obj.get("name").getAsString(),
        obj.has("displayName") ? obj.get("displayName").getAsString() : obj.get("name").getAsString(),
        obj.has("category") ? obj.get("category").getAsString() : "general",
        obj.get("world").getAsString(),
        obj.get("x").getAsDouble(),
        obj.get("y").getAsDouble(),
        obj.get("z").getAsDouble(),
        obj.get("yaw").getAsFloat(),
        obj.get("pitch").getAsFloat(),
        obj.has("permission") ? obj.get("permission").getAsString() : null,
        obj.has("description") ? obj.get("description").getAsString() : null,
        obj.has("createdAt") ? obj.get("createdAt").getAsLong() : System.currentTimeMillis(),
        obj.has("createdBy") ? obj.get("createdBy").getAsString() : null
      );
    }
  }

  // ========== Spawn Storage ==========

  private class JsonSpawnStorage implements SpawnStorage {

    private final Path spawnsFile = dataRoot.resolve("spawns.json");

    @Override
    public CompletableFuture<Void> init() {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Map<String, Spawn>> loadSpawns() {
      return CompletableFuture.supplyAsync(() -> {
        Map<String, Spawn> spawns = new HashMap<>();

        if (!Files.exists(spawnsFile)) {
          return spawns;
        }

        try {
          String json = Files.readString(spawnsFile);
          JsonObject root = JsonParser.parseString(json).getAsJsonObject();

          if (root.has("spawns") && root.get("spawns").isJsonObject()) {
            JsonObject spawnsObj = root.getAsJsonObject("spawns");
            for (String name : spawnsObj.keySet()) {
              JsonObject spawnObj = spawnsObj.getAsJsonObject(name);
              Spawn spawn = deserializeSpawn(spawnObj);
              spawns.put(spawn.name(), spawn);
            }
          }

          Logger.info("[Storage] Loaded %d spawns", spawns.size());
        } catch (Exception e) {
          Logger.severe("[Storage] Failed to load spawns: %s", e.getMessage());
        }

        return spawns;
      });
    }

    @Override
    public CompletableFuture<Void> saveSpawns(@NotNull Map<String, Spawn> spawns) {
      return CompletableFuture.runAsync(() -> {
        try {
          JsonObject root = new JsonObject();
          JsonObject spawnsObj = new JsonObject();

          for (Spawn spawn : spawns.values()) {
            spawnsObj.add(spawn.name(), serializeSpawn(spawn));
          }

          root.add("spawns", spawnsObj);
          atomicWrite(spawnsFile, gson.toJson(root));
          Logger.debug("[Storage] Saved %d spawns", spawns.size());
        } catch (IOException e) {
          Logger.severe("[Storage] Failed to save spawns: %s", e.getMessage());
        }
      });
    }

    private JsonObject serializeSpawn(Spawn spawn) {
      JsonObject obj = new JsonObject();
      obj.addProperty("name", spawn.name());
      obj.addProperty("world", spawn.world());
      obj.addProperty("x", spawn.x());
      obj.addProperty("y", spawn.y());
      obj.addProperty("z", spawn.z());
      obj.addProperty("yaw", spawn.yaw());
      obj.addProperty("pitch", spawn.pitch());
      if (spawn.permission() != null) {
        obj.addProperty("permission", spawn.permission());
      }
      if (spawn.groupPermission() != null) {
        obj.addProperty("groupPermission", spawn.groupPermission());
      }
      obj.addProperty("isDefault", spawn.isDefault());
      obj.addProperty("createdAt", spawn.createdAt());
      if (spawn.createdBy() != null) {
        obj.addProperty("createdBy", spawn.createdBy());
      }
      return obj;
    }

    private Spawn deserializeSpawn(JsonObject obj) {
      return new Spawn(
        obj.get("name").getAsString(),
        obj.get("world").getAsString(),
        obj.get("x").getAsDouble(),
        obj.get("y").getAsDouble(),
        obj.get("z").getAsDouble(),
        obj.get("yaw").getAsFloat(),
        obj.get("pitch").getAsFloat(),
        obj.has("permission") ? obj.get("permission").getAsString() : null,
        obj.has("groupPermission") ? obj.get("groupPermission").getAsString() : null,
        obj.has("isDefault") && obj.get("isDefault").getAsBoolean(),
        obj.has("createdAt") ? obj.get("createdAt").getAsLong() : System.currentTimeMillis(),
        obj.has("createdBy") ? obj.get("createdBy").getAsString() : null
      );
    }
  }

  // ========== Player Data Storage ==========

  private class JsonPlayerDataStorage implements PlayerDataStorage {

    @Override
    public CompletableFuture<Void> init() {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
      return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Optional<PlayerTeleportData>> loadPlayerData(@NotNull UUID uuid) {
      return CompletableFuture.supplyAsync(() -> {
        Path playerFile = playersDir.resolve(uuid.toString() + ".json");

        if (!Files.exists(playerFile)) {
          return Optional.empty();
        }

        try {
          String json = Files.readString(playerFile);
          JsonObject root = JsonParser.parseString(json).getAsJsonObject();

          String username = root.has("username") ? root.get("username").getAsString() : "Unknown";
          PlayerTeleportData data = new PlayerTeleportData(uuid, username);

          data.setTpToggle(!root.has("tpToggle") || root.get("tpToggle").getAsBoolean());
          data.setLastTpaRequest(root.has("lastTpaRequest") ? root.get("lastTpaRequest").getAsLong() : 0);
          data.setLastTeleport(root.has("lastTeleport") ? root.get("lastTeleport").getAsLong() : 0);

          if (root.has("backHistory") && root.get("backHistory").isJsonArray()) {
            JsonArray historyArray = root.getAsJsonArray("backHistory");
            List<Location> history = new ArrayList<>();
            for (var element : historyArray) {
              if (element.isJsonObject()) {
                history.add(deserializeLocation(element.getAsJsonObject()));
              }
            }
            data.setBackHistory(history);
          }

          return Optional.of(data);
        } catch (Exception e) {
          Logger.severe("[Storage] Failed to load player data for %s: %s", uuid, e.getMessage());
          return Optional.empty();
        }
      });
    }

    @Override
    public CompletableFuture<Void> savePlayerData(@NotNull PlayerTeleportData data) {
      return CompletableFuture.runAsync(() -> {
        Path playerFile = playersDir.resolve(data.getUuid().toString() + ".json");

        try {
          JsonObject root = new JsonObject();
          root.addProperty("uuid", data.getUuid().toString());
          root.addProperty("username", data.getUsername());
          root.addProperty("tpToggle", data.isTpToggle());
          root.addProperty("lastTpaRequest", data.getLastTpaRequest());
          root.addProperty("lastTeleport", data.getLastTeleport());

          JsonArray historyArray = new JsonArray();
          for (Location loc : data.getBackHistory()) {
            historyArray.add(serializeLocation(loc));
          }
          root.add("backHistory", historyArray);

          atomicWrite(playerFile, gson.toJson(root));
          Logger.debug("[Storage] Saved player data for %s", data.getUsername());
        } catch (IOException e) {
          Logger.severe("[Storage] Failed to save player data for %s: %s", data.getUuid(), e.getMessage());
        }
      });
    }

    @Override
    public CompletableFuture<Void> deletePlayerData(@NotNull UUID uuid) {
      return CompletableFuture.runAsync(() -> {
        Path playerFile = playersDir.resolve(uuid.toString() + ".json");
        try {
          Files.deleteIfExists(playerFile);
          Logger.debug("[Storage] Deleted player data for %s", uuid);
        } catch (IOException e) {
          Logger.severe("[Storage] Failed to delete player data for %s: %s", uuid, e.getMessage());
        }
      });
    }
  }
}
