package com.hyperessentials.storage.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperessentials.data.*;
import com.hyperessentials.module.kits.data.Kit;
import com.hyperessentials.module.kits.data.KitItem;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.module.moderation.data.PunishmentType;
import com.hyperessentials.storage.*;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * JSON file-based storage provider with atomic writes and per-file storage.
 *
 * <p>Directory layout:
 * <pre>
 *   data/
 *     players/<uuid>.json    — Unified player data (teleport + stats + punishments)
 *     homes/<playerUuid>.json — Homes + inline sharing
 *     kits/<kitUuid>.json    — One file per kit
 *     spawns/<worldUuid>.json — One spawn per world
 *     warps/<warpUuid>.json  — One file per warp
 * </pre>
 */
public class JsonStorageProvider implements StorageProvider {

  private final Path dataRoot;
  private final Path playersDir;
  private final Path homesDir;
  private final Path kitsDir;
  private final Path spawnsDir;
  private final Path warpsDir;
  private final Gson gson;

  private final JsonHomeStorage homeStorage;
  private final JsonWarpStorage warpStorage;
  private final JsonSpawnStorage spawnStorage;
  private final JsonPlayerDataStorage playerDataStorage;
  private final JsonKitStorage kitStorage;

  /** Per-player locks for homes and player data files. */
  private final ConcurrentHashMap<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();

  public JsonStorageProvider(@NotNull Path dataDir) {
    this.dataRoot = dataDir.resolve("data");
    this.playersDir = dataRoot.resolve("players");
    this.homesDir = dataRoot.resolve("homes");
    this.kitsDir = dataRoot.resolve("kits");
    this.spawnsDir = dataRoot.resolve("spawns");
    this.warpsDir = dataRoot.resolve("warps");
    this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    this.homeStorage = new JsonHomeStorage();
    this.warpStorage = new JsonWarpStorage();
    this.spawnStorage = new JsonSpawnStorage();
    this.playerDataStorage = new JsonPlayerDataStorage();
    this.kitStorage = new JsonKitStorage();
  }

  @Override
  public CompletableFuture<Void> init() {
    return CompletableFuture.runAsync(() -> {
      try {
        Files.createDirectories(playersDir);
        Files.createDirectories(homesDir);
        Files.createDirectories(kitsDir);
        Files.createDirectories(spawnsDir);
        Files.createDirectories(warpsDir);

        // Cleanup orphaned temp/bak files from previous crashes
        int cleaned = 0;
        cleaned += StorageUtils.cleanupOrphanedFiles(playersDir);
        cleaned += StorageUtils.cleanupOrphanedFiles(homesDir);
        cleaned += StorageUtils.cleanupOrphanedFiles(kitsDir);
        cleaned += StorageUtils.cleanupOrphanedFiles(spawnsDir);
        cleaned += StorageUtils.cleanupOrphanedFiles(warpsDir);

        Logger.info("[Storage] JSON storage initialized at %s (cleaned %d orphaned files)", dataRoot, cleaned);
      } catch (IOException e) {
        throw new RuntimeException("Failed to create storage directories", e);
      }
    });
  }

  @Override
  public CompletableFuture<Void> shutdown() {
    playerLocks.clear();
    Logger.info("[Storage] JSON storage provider shut down");
    return CompletableFuture.completedFuture(null);
  }

  @Override @NotNull public HomeStorage getHomeStorage() { return homeStorage; }
  @Override @NotNull public WarpStorage getWarpStorage() { return warpStorage; }
  @Override @NotNull public SpawnStorage getSpawnStorage() { return spawnStorage; }
  @Override @NotNull public PlayerDataStorage getPlayerDataStorage() { return playerDataStorage; }
  @Override @NotNull public KitStorage getKitStorage() { return kitStorage; }

  // ========== Per-player locking ==========

  private ReentrantLock getLock(@NotNull UUID uuid) {
    return playerLocks.computeIfAbsent(uuid, k -> new ReentrantLock());
  }

  // ========== Location serialization ==========

  private JsonObject serializeLocation(@NotNull Location loc) {
    JsonObject obj = new JsonObject();
    obj.addProperty("world", loc.world());
    obj.addProperty("worldUuid", loc.worldUuid());
    obj.addProperty("x", loc.x());
    obj.addProperty("y", loc.y());
    obj.addProperty("z", loc.z());
    obj.addProperty("yaw", loc.yaw());
    obj.addProperty("pitch", loc.pitch());
    return obj;
  }

  private Location deserializeLocation(@NotNull JsonObject obj) {
    return new Location(
      obj.get("world").getAsString(),
      obj.has("worldUuid") ? obj.get("worldUuid").getAsString() : "",
      obj.get("x").getAsDouble(),
      obj.get("y").getAsDouble(),
      obj.get("z").getAsDouble(),
      obj.get("yaw").getAsFloat(),
      obj.get("pitch").getAsFloat()
    );
  }

  // ========== Punishment serialization ==========

  private JsonObject serializePunishment(@NotNull Punishment p) {
    JsonObject obj = new JsonObject();
    obj.addProperty("id", p.id().toString());
    obj.addProperty("type", p.type().name());
    obj.addProperty("playerUuid", p.playerUuid().toString());
    obj.addProperty("playerName", p.playerName());
    obj.addProperty("issuerUuid", p.issuerUuid() != null ? p.issuerUuid().toString() : null);
    obj.addProperty("issuerName", p.issuerName());
    obj.addProperty("reason", p.reason());
    obj.addProperty("issuedAt", p.issuedAt().toEpochMilli());
    obj.addProperty("expiresAt", p.expiresAt() != null ? p.expiresAt().toEpochMilli() : null);
    obj.addProperty("active", p.active());
    obj.addProperty("revokedBy", p.revokedBy() != null ? p.revokedBy().toString() : null);
    obj.addProperty("revokedAt", p.revokedAt() != null ? p.revokedAt().toEpochMilli() : null);
    return obj;
  }

  @Nullable
  private Punishment deserializePunishment(@NotNull JsonObject obj) {
    try {
      return new Punishment(
        UUID.fromString(obj.get("id").getAsString()),
        PunishmentType.valueOf(obj.get("type").getAsString()),
        UUID.fromString(obj.get("playerUuid").getAsString()),
        obj.get("playerName").getAsString(),
        obj.has("issuerUuid") && !obj.get("issuerUuid").isJsonNull()
          ? UUID.fromString(obj.get("issuerUuid").getAsString()) : null,
        obj.get("issuerName").getAsString(),
        obj.has("reason") && !obj.get("reason").isJsonNull()
          ? obj.get("reason").getAsString() : null,
        Instant.ofEpochMilli(obj.get("issuedAt").getAsLong()),
        obj.has("expiresAt") && !obj.get("expiresAt").isJsonNull()
          ? Instant.ofEpochMilli(obj.get("expiresAt").getAsLong()) : null,
        obj.get("active").getAsBoolean(),
        obj.has("revokedBy") && !obj.get("revokedBy").isJsonNull()
          ? UUID.fromString(obj.get("revokedBy").getAsString()) : null,
        obj.has("revokedAt") && !obj.get("revokedAt").isJsonNull()
          ? Instant.ofEpochMilli(obj.get("revokedAt").getAsLong()) : null
      );
    } catch (Exception e) {
      Logger.warn("[Storage] Failed to parse punishment: %s", e.getMessage());
      return null;
    }
  }

  // ========== Directory scanning helper ==========

  private List<Path> listJsonFiles(@NotNull Path dir) {
    List<Path> files = new ArrayList<>();
    if (!Files.exists(dir)) return files;
    try (var stream = Files.newDirectoryStream(dir, "*.json")) {
      for (Path file : stream) {
        files.add(file);
      }
    } catch (IOException e) {
      Logger.severe("[Storage] Failed to scan directory %s: %s", dir.getFileName(), e.getMessage());
    }
    return files;
  }

  // ========== Home Storage ==========

  private class JsonHomeStorage implements HomeStorage {

    @Override public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
    @Override public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }

    @Override
    public CompletableFuture<Optional<PlayerHomes>> loadPlayerHomes(@NotNull UUID uuid) {
      return CompletableFuture.supplyAsync(() -> {
        Path file = homesDir.resolve(uuid + ".json");
        if (!Files.exists(file)) return Optional.empty();

        ReentrantLock lock = getLock(uuid);
        lock.lock();
        try {
          String json;
          try {
            json = Files.readString(file);
          } catch (Exception e) {
            Logger.warn("[Storage] Failed to read homes for %s, attempting backup recovery", uuid);
            if (StorageUtils.recoverFromBackup(file)) {
              json = Files.readString(file);
            } else {
              return Optional.empty();
            }
          }

          JsonObject root = JsonParser.parseString(json).getAsJsonObject();
          String username = root.has("username") ? root.get("username").getAsString() : "unknown";
          PlayerHomes playerHomes = new PlayerHomes(uuid, username);

          if (root.has("homes") && root.get("homes").isJsonObject()) {
            JsonObject homesObj = root.getAsJsonObject("homes");
            for (String key : homesObj.keySet()) {
              JsonObject homeObj = homesObj.getAsJsonObject(key);
              Home home = deserializeHome(homeObj);
              playerHomes.setHome(home);
            }
          }

          if (root.has("defaultHome") && !root.get("defaultHome").isJsonNull()) {
            playerHomes.setDefaultHome(root.get("defaultHome").getAsString());
          }

          // Load inline shares
          if (root.has("shares") && root.get("shares").isJsonObject()) {
            Map<String, Set<UUID>> sharesData = new HashMap<>();
            JsonObject sharesObj = root.getAsJsonObject("shares");
            for (String homeName : sharesObj.keySet()) {
              Set<UUID> uuids = new HashSet<>();
              for (JsonElement el : sharesObj.getAsJsonArray(homeName)) {
                try { uuids.add(UUID.fromString(el.getAsString())); }
                catch (Exception ignored) {}
              }
              if (!uuids.isEmpty()) sharesData.put(homeName, uuids);
            }
            playerHomes.setShares(sharesData);
          }

          Logger.debug("[Storage] Loaded %d homes for %s", playerHomes.count(), uuid);
          return Optional.of(playerHomes);
        } catch (Exception e) {
          Logger.severe("[Storage] Failed to load homes for %s: %s", uuid, e.getMessage());
          return Optional.empty();
        } finally {
          lock.unlock();
        }
      });
    }

    @Override
    public CompletableFuture<Void> savePlayerHomes(@NotNull PlayerHomes playerHomes) {
      return CompletableFuture.runAsync(() -> {
        ReentrantLock lock = getLock(playerHomes.getUuid());
        lock.lock();
        try {
          JsonObject root = new JsonObject();
          root.addProperty("uuid", playerHomes.getUuid().toString());
          root.addProperty("username", playerHomes.getUsername());

          JsonObject homesObj = new JsonObject();
          for (Home home : playerHomes.getHomes()) {
            homesObj.add(home.name().toLowerCase(), serializeHome(home));
          }
          root.add("homes", homesObj);

          if (playerHomes.getDefaultHome() != null) {
            root.addProperty("defaultHome", playerHomes.getDefaultHome());
          }

          // Inline shares
          Map<String, Set<UUID>> allShares = playerHomes.getAllShares();
          if (!allShares.isEmpty()) {
            JsonObject sharesObj = new JsonObject();
            for (Map.Entry<String, Set<UUID>> entry : allShares.entrySet()) {
              JsonArray arr = new JsonArray();
              for (UUID id : entry.getValue()) {
                arr.add(id.toString());
              }
              sharesObj.add(entry.getKey(), arr);
            }
            root.add("shares", sharesObj);
          }

          Path file = homesDir.resolve(playerHomes.getUuid() + ".json");
          StorageUtils.WriteResult result = StorageUtils.writeAtomic(file, gson.toJson(root));
          if (result instanceof StorageUtils.WriteResult.Failure f) {
            Logger.severe("[Storage] Failed to save homes for %s: %s", playerHomes.getUuid(), f.error());
          }
        } finally {
          lock.unlock();
        }
      });
    }

    @Override
    public CompletableFuture<Map<UUID, Map<String, Set<UUID>>>> scanAllShares() {
      return CompletableFuture.supplyAsync(() -> {
        Map<UUID, Map<String, Set<UUID>>> result = new HashMap<>();

        for (Path file : listJsonFiles(homesDir)) {
          try {
            String json = Files.readString(file);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (!root.has("uuid") || !root.has("shares")) continue;

            UUID ownerUuid = UUID.fromString(root.get("uuid").getAsString());
            JsonObject sharesObj = root.getAsJsonObject("shares");
            Map<String, Set<UUID>> ownerShares = new HashMap<>();

            for (String homeName : sharesObj.keySet()) {
              Set<UUID> uuids = new HashSet<>();
              for (JsonElement el : sharesObj.getAsJsonArray(homeName)) {
                try { uuids.add(UUID.fromString(el.getAsString())); }
                catch (Exception ignored) {}
              }
              if (!uuids.isEmpty()) ownerShares.put(homeName, uuids);
            }

            if (!ownerShares.isEmpty()) result.put(ownerUuid, ownerShares);
          } catch (Exception e) {
            Logger.warn("[Storage] Failed to scan shares from %s: %s", file.getFileName(), e.getMessage());
          }
        }

        Logger.info("[Storage] Scanned shares: %d owner(s) with shared homes", result.size());
        return result;
      });
    }

    private JsonObject serializeHome(@NotNull Home home) {
      JsonObject obj = new JsonObject();
      obj.addProperty("name", home.name());
      obj.addProperty("world", home.world());
      obj.addProperty("worldUuid", home.worldUuid());
      obj.addProperty("x", home.x());
      obj.addProperty("y", home.y());
      obj.addProperty("z", home.z());
      obj.addProperty("yaw", home.yaw());
      obj.addProperty("pitch", home.pitch());
      obj.addProperty("createdAt", home.createdAt());
      obj.addProperty("lastUsed", home.lastUsed());
      return obj;
    }

    private Home deserializeHome(@NotNull JsonObject obj) {
      return new Home(
        obj.get("name").getAsString(),
        obj.get("world").getAsString(),
        obj.has("worldUuid") ? obj.get("worldUuid").getAsString() : "",
        obj.get("x").getAsDouble(),
        obj.get("y").getAsDouble(),
        obj.get("z").getAsDouble(),
        obj.get("yaw").getAsFloat(),
        obj.get("pitch").getAsFloat(),
        obj.has("createdAt") ? obj.get("createdAt").getAsLong() : System.currentTimeMillis(),
        obj.has("lastUsed") ? obj.get("lastUsed").getAsLong() : System.currentTimeMillis()
      );
    }
  }

  // ========== Warp Storage ==========

  private class JsonWarpStorage implements WarpStorage {

    @Override public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
    @Override public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }

    @Override
    public CompletableFuture<Map<String, Warp>> loadAllWarps() {
      return CompletableFuture.supplyAsync(() -> {
        Map<String, Warp> warps = new HashMap<>();

        for (Path file : listJsonFiles(warpsDir)) {
          try {
            String json = Files.readString(file);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            Warp warp = deserializeWarp(obj);
            warps.put(warp.name(), warp);
          } catch (Exception e) {
            Logger.warn("[Storage] Failed to load warp from %s: %s", file.getFileName(), e.getMessage());
            StorageUtils.recoverFromBackup(file);
          }
        }

        Logger.info("[Storage] Loaded %d warps", warps.size());
        return warps;
      });
    }

    @Override
    public CompletableFuture<Void> saveWarp(@NotNull Warp warp) {
      return CompletableFuture.runAsync(() -> {
        Path file = warpsDir.resolve(warp.uuid() + ".json");
        StorageUtils.WriteResult result = StorageUtils.writeAtomic(file, gson.toJson(serializeWarp(warp)));
        if (result instanceof StorageUtils.WriteResult.Failure f) {
          Logger.severe("[Storage] Failed to save warp %s: %s", warp.name(), f.error());
        }
      });
    }

    @Override
    public CompletableFuture<Void> deleteWarp(@NotNull UUID warpUuid) {
      return CompletableFuture.runAsync(() -> {
        Path file = warpsDir.resolve(warpUuid + ".json");
        StorageUtils.deleteWithBackup(file);
        Logger.debug("[Storage] Deleted warp file: %s", warpUuid);
      });
    }

    private JsonObject serializeWarp(@NotNull Warp warp) {
      JsonObject obj = new JsonObject();
      obj.addProperty("uuid", warp.uuid().toString());
      obj.addProperty("name", warp.name());
      obj.addProperty("displayName", warp.displayName());
      obj.addProperty("category", warp.category());
      obj.addProperty("world", warp.world());
      obj.addProperty("worldUuid", warp.worldUuid());
      obj.addProperty("x", warp.x());
      obj.addProperty("y", warp.y());
      obj.addProperty("z", warp.z());
      obj.addProperty("yaw", warp.yaw());
      obj.addProperty("pitch", warp.pitch());
      if (warp.permission() != null) obj.addProperty("permission", warp.permission());
      if (warp.description() != null) obj.addProperty("description", warp.description());
      obj.addProperty("createdAt", warp.createdAt());
      if (warp.createdBy() != null) obj.addProperty("createdBy", warp.createdBy());
      return obj;
    }

    private Warp deserializeWarp(@NotNull JsonObject obj) {
      return new Warp(
        obj.has("uuid") ? UUID.fromString(obj.get("uuid").getAsString()) : UUID.randomUUID(),
        obj.get("name").getAsString(),
        obj.has("displayName") ? obj.get("displayName").getAsString() : obj.get("name").getAsString(),
        obj.has("category") ? obj.get("category").getAsString() : "general",
        obj.get("world").getAsString(),
        obj.has("worldUuid") ? obj.get("worldUuid").getAsString() : "",
        obj.get("x").getAsDouble(),
        obj.get("y").getAsDouble(),
        obj.get("z").getAsDouble(),
        obj.get("yaw").getAsFloat(),
        obj.get("pitch").getAsFloat(),
        obj.has("permission") && !obj.get("permission").isJsonNull() ? obj.get("permission").getAsString() : null,
        obj.has("description") && !obj.get("description").isJsonNull() ? obj.get("description").getAsString() : null,
        obj.has("createdAt") ? obj.get("createdAt").getAsLong() : System.currentTimeMillis(),
        obj.has("createdBy") && !obj.get("createdBy").isJsonNull() ? obj.get("createdBy").getAsString() : null
      );
    }
  }

  // ========== Spawn Storage ==========

  private class JsonSpawnStorage implements SpawnStorage {

    @Override public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
    @Override public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }

    @Override
    public CompletableFuture<Map<String, Spawn>> loadAllSpawns() {
      return CompletableFuture.supplyAsync(() -> {
        Map<String, Spawn> spawns = new HashMap<>();

        for (Path file : listJsonFiles(spawnsDir)) {
          try {
            String json = Files.readString(file);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            Spawn spawn = deserializeSpawn(obj);
            spawns.put(spawn.worldUuid(), spawn);
          } catch (Exception e) {
            Logger.warn("[Storage] Failed to load spawn from %s: %s", file.getFileName(), e.getMessage());
            StorageUtils.recoverFromBackup(file);
          }
        }

        Logger.info("[Storage] Loaded %d spawns", spawns.size());
        return spawns;
      });
    }

    @Override
    public CompletableFuture<Void> saveSpawn(@NotNull Spawn spawn) {
      return CompletableFuture.runAsync(() -> {
        Path file = spawnsDir.resolve(spawn.worldUuid() + ".json");
        StorageUtils.WriteResult result = StorageUtils.writeAtomic(file, gson.toJson(serializeSpawn(spawn)));
        if (result instanceof StorageUtils.WriteResult.Failure f) {
          Logger.severe("[Storage] Failed to save spawn for world %s: %s", spawn.worldName(), f.error());
        }
      });
    }

    @Override
    public CompletableFuture<Void> deleteSpawn(@NotNull String worldUuid) {
      return CompletableFuture.runAsync(() -> {
        Path file = spawnsDir.resolve(worldUuid + ".json");
        StorageUtils.deleteWithBackup(file);
        Logger.debug("[Storage] Deleted spawn file for world: %s", worldUuid);
      });
    }

    private JsonObject serializeSpawn(@NotNull Spawn spawn) {
      JsonObject obj = new JsonObject();
      obj.addProperty("worldUuid", spawn.worldUuid());
      obj.addProperty("worldName", spawn.worldName());
      obj.addProperty("x", spawn.x());
      obj.addProperty("y", spawn.y());
      obj.addProperty("z", spawn.z());
      obj.addProperty("yaw", spawn.yaw());
      obj.addProperty("pitch", spawn.pitch());
      obj.addProperty("isGlobal", spawn.isGlobal());
      obj.addProperty("createdAt", spawn.createdAt());
      if (spawn.createdBy() != null) obj.addProperty("createdBy", spawn.createdBy());
      return obj;
    }

    private Spawn deserializeSpawn(@NotNull JsonObject obj) {
      return new Spawn(
        obj.get("worldUuid").getAsString(),
        obj.has("worldName") ? obj.get("worldName").getAsString() : "unknown",
        obj.get("x").getAsDouble(),
        obj.get("y").getAsDouble(),
        obj.get("z").getAsDouble(),
        obj.get("yaw").getAsFloat(),
        obj.get("pitch").getAsFloat(),
        obj.has("isGlobal") && obj.get("isGlobal").getAsBoolean(),
        obj.has("createdAt") ? obj.get("createdAt").getAsLong() : System.currentTimeMillis(),
        obj.has("createdBy") && !obj.get("createdBy").isJsonNull() ? obj.get("createdBy").getAsString() : null
      );
    }
  }

  // ========== Player Data Storage ==========

  private class JsonPlayerDataStorage implements PlayerDataStorage {

    @Override public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
    @Override public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }

    @Override
    public CompletableFuture<Optional<PlayerData>> loadPlayerData(@NotNull UUID uuid) {
      return CompletableFuture.supplyAsync(() -> {
        Path playerFile = playersDir.resolve(uuid + ".json");
        if (!Files.exists(playerFile)) return Optional.empty();

        ReentrantLock lock = getLock(uuid);
        lock.lock();
        try {
          String json;
          try {
            json = Files.readString(playerFile);
          } catch (Exception e) {
            Logger.warn("[Storage] Failed to read player data for %s, attempting backup recovery", uuid);
            if (StorageUtils.recoverFromBackup(playerFile)) {
              json = Files.readString(playerFile);
            } else {
              return Optional.empty();
            }
          }

          JsonObject root = JsonParser.parseString(json).getAsJsonObject();
          String username = root.has("username") ? root.get("username").getAsString() : "Unknown";
          PlayerData data = new PlayerData(uuid, username);

          // Teleport fields
          data.setTpToggle(!root.has("tpToggle") || root.get("tpToggle").getAsBoolean());
          data.setLastTpaRequest(root.has("lastTpaRequest") ? root.get("lastTpaRequest").getAsLong() : 0);
          data.setLastTeleport(root.has("lastTeleport") ? root.get("lastTeleport").getAsLong() : 0);

          if (root.has("backHistory") && root.get("backHistory").isJsonArray()) {
            List<com.hyperessentials.data.BackEntry> history = new ArrayList<>();
            for (var element : root.getAsJsonArray("backHistory")) {
              if (element.isJsonObject()) {
                JsonObject entryObj = element.getAsJsonObject();
                Location loc = deserializeLocation(entryObj);
                String source = entryObj.has("source") ? entryObj.get("source").getAsString()
                    : com.hyperessentials.data.BackEntry.SOURCE_UNKNOWN;
                long timestamp = entryObj.has("timestamp") ? entryObj.get("timestamp").getAsLong()
                    : System.currentTimeMillis();
                history.add(new com.hyperessentials.data.BackEntry(loc, source, timestamp));
              }
            }
            data.setBackHistory(history);
          }

          // Stats fields
          if (root.has("firstJoin")) {
            data.setFirstJoin(Instant.ofEpochMilli(root.get("firstJoin").getAsLong()));
          }
          if (root.has("totalPlaytimeMs")) {
            data.setTotalPlaytimeMs(root.get("totalPlaytimeMs").getAsLong());
          }
          if (root.has("lastJoin")) {
            data.setLastJoin(Instant.ofEpochMilli(root.get("lastJoin").getAsLong()));
          }

          // Punishments
          if (root.has("punishments") && root.get("punishments").isJsonArray()) {
            List<Punishment> punishments = new ArrayList<>();
            for (JsonElement el : root.getAsJsonArray("punishments")) {
              Punishment p = deserializePunishment(el.getAsJsonObject());
              if (p != null) punishments.add(p);
            }
            data.setPunishments(punishments);
          }

          return Optional.of(data);
        } catch (Exception e) {
          Logger.severe("[Storage] Failed to load player data for %s: %s", uuid, e.getMessage());
          return Optional.empty();
        } finally {
          lock.unlock();
        }
      });
    }

    @Override
    public CompletableFuture<Void> savePlayerData(@NotNull PlayerData data) {
      return CompletableFuture.runAsync(() -> {
        ReentrantLock lock = getLock(data.getUuid());
        lock.lock();
        try {
          JsonObject root = new JsonObject();
          root.addProperty("uuid", data.getUuid().toString());
          root.addProperty("username", data.getUsername());

          // Teleport
          root.addProperty("tpToggle", data.isTpToggle());
          root.addProperty("lastTpaRequest", data.getLastTpaRequest());
          root.addProperty("lastTeleport", data.getLastTeleport());

          JsonArray historyArray = new JsonArray();
          for (com.hyperessentials.data.BackEntry entry : data.getBackHistory()) {
            JsonObject entryObj = serializeLocation(entry.location());
            entryObj.addProperty("source", entry.source());
            entryObj.addProperty("timestamp", entry.timestamp());
            historyArray.add(entryObj);
          }
          root.add("backHistory", historyArray);

          // Stats
          root.addProperty("firstJoin", data.getFirstJoin().toEpochMilli());
          root.addProperty("totalPlaytimeMs", data.getTotalPlaytimeMs());
          root.addProperty("lastJoin", data.getLastJoin().toEpochMilli());

          // Punishments
          List<Punishment> punishments = data.getPunishments();
          if (!punishments.isEmpty()) {
            JsonArray punArr = new JsonArray();
            for (Punishment p : punishments) {
              punArr.add(serializePunishment(p));
            }
            root.add("punishments", punArr);
          }

          Path playerFile = playersDir.resolve(data.getUuid() + ".json");
          StorageUtils.WriteResult result = StorageUtils.writeAtomic(playerFile, gson.toJson(root));
          if (result instanceof StorageUtils.WriteResult.Failure f) {
            Logger.severe("[Storage] Failed to save player data for %s: %s", data.getUuid(), f.error());
          }
        } finally {
          lock.unlock();
        }
      });
    }

    @Override
    public CompletableFuture<Void> deletePlayerData(@NotNull UUID uuid) {
      return CompletableFuture.runAsync(() -> {
        Path playerFile = playersDir.resolve(uuid + ".json");
        StorageUtils.deleteWithBackup(playerFile);
        Logger.debug("[Storage] Deleted player data for %s", uuid);
      });
    }

    @Override
    public CompletableFuture<List<PlayerData>> loadAllPlayerData() {
      return CompletableFuture.supplyAsync(() -> {
        List<PlayerData> result = new ArrayList<>();

        for (Path file : listJsonFiles(playersDir)) {
          try {
            String filename = file.getFileName().toString();
            String uuidStr = filename.replace(".json", "");
            UUID uuid = UUID.fromString(uuidStr);

            Optional<PlayerData> data = loadPlayerData(uuid).join();
            data.ifPresent(result::add);
          } catch (Exception e) {
            Logger.warn("[Storage] Failed to load player data from %s: %s", file.getFileName(), e.getMessage());
          }
        }

        return result;
      });
    }
  }

  // ========== Kit Storage ==========

  private class JsonKitStorage implements KitStorage {

    @Override public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
    @Override public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }

    @Override
    public CompletableFuture<Map<String, Kit>> loadAllKits() {
      return CompletableFuture.supplyAsync(() -> {
        Map<String, Kit> kits = new HashMap<>();

        for (Path file : listJsonFiles(kitsDir)) {
          try {
            String json = Files.readString(file);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            Kit kit = deserializeKit(obj);
            if (kit != null) kits.put(kit.name(), kit);
          } catch (Exception e) {
            Logger.warn("[Storage] Failed to load kit from %s: %s", file.getFileName(), e.getMessage());
            StorageUtils.recoverFromBackup(file);
          }
        }

        Logger.info("[Storage] Loaded %d kits", kits.size());
        return kits;
      });
    }

    @Override
    public CompletableFuture<Void> saveKit(@NotNull Kit kit) {
      return CompletableFuture.runAsync(() -> {
        Path file = kitsDir.resolve(kit.uuid() + ".json");
        StorageUtils.WriteResult result = StorageUtils.writeAtomic(file, gson.toJson(serializeKit(kit)));
        if (result instanceof StorageUtils.WriteResult.Failure f) {
          Logger.severe("[Storage] Failed to save kit %s: %s", kit.name(), f.error());
        }
      });
    }

    @Override
    public CompletableFuture<Void> deleteKit(@NotNull UUID kitUuid) {
      return CompletableFuture.runAsync(() -> {
        Path file = kitsDir.resolve(kitUuid + ".json");
        StorageUtils.deleteWithBackup(file);
        Logger.debug("[Storage] Deleted kit file: %s", kitUuid);
      });
    }

    private JsonObject serializeKit(@NotNull Kit kit) {
      JsonObject obj = new JsonObject();
      obj.addProperty("uuid", kit.uuid().toString());
      obj.addProperty("name", kit.name());
      obj.addProperty("displayName", kit.displayName());
      obj.addProperty("cooldownSeconds", kit.cooldownSeconds());
      obj.addProperty("oneTime", kit.oneTime());
      if (kit.permission() != null) obj.addProperty("permission", kit.permission());

      JsonArray items = new JsonArray();
      for (KitItem item : kit.items()) {
        JsonObject itemObj = new JsonObject();
        itemObj.addProperty("itemId", item.itemId());
        itemObj.addProperty("quantity", item.quantity());
        itemObj.addProperty("slot", item.slot());
        itemObj.addProperty("section", item.section());
        items.add(itemObj);
      }
      obj.add("items", items);
      return obj;
    }

    @Nullable
    private Kit deserializeKit(@NotNull JsonObject obj) {
      try {
        UUID uuid = obj.has("uuid") ? UUID.fromString(obj.get("uuid").getAsString()) : UUID.randomUUID();
        String name = obj.get("name").getAsString();
        String displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : name;
        int cooldown = obj.has("cooldownSeconds") ? obj.get("cooldownSeconds").getAsInt() : 0;
        boolean oneTime = obj.has("oneTime") && obj.get("oneTime").getAsBoolean();
        String permission = obj.has("permission") && !obj.get("permission").isJsonNull()
          ? obj.get("permission").getAsString() : null;

        List<KitItem> items = new ArrayList<>();
        if (obj.has("items") && obj.get("items").isJsonArray()) {
          for (JsonElement el : obj.getAsJsonArray("items")) {
            JsonObject itemObj = el.getAsJsonObject();
            items.add(new KitItem(
              itemObj.get("itemId").getAsString(),
              itemObj.has("quantity") ? itemObj.get("quantity").getAsInt() : 1,
              itemObj.has("slot") ? itemObj.get("slot").getAsInt() : -1,
              itemObj.has("section") ? itemObj.get("section").getAsString() : KitItem.STORAGE
            ));
          }
        }

        return new Kit(uuid, name.toLowerCase(), displayName, items, cooldown, oneTime, permission);
      } catch (Exception e) {
        Logger.warn("[Storage] Failed to parse kit: %s", e.getMessage());
        return null;
      }
    }
  }
}
