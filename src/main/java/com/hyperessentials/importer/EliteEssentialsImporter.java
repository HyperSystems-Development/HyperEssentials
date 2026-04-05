package com.hyperessentials.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hyperessentials.backup.BackupManager;
import com.hyperessentials.data.Home;
import com.hyperessentials.data.PlayerData;
import com.hyperessentials.data.PlayerHomes;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.data.Warp;
import com.hyperessentials.module.kits.data.Kit;
import com.hyperessentials.module.kits.data.KitItem;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.module.moderation.data.PunishmentType;
import com.hyperessentials.storage.StorageProvider;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports data from EliteEssentials (v1.1.21).
 *
 * <p>Storage format:
 * <ul>
 *   <li>Homes: {@code players/<uuid>.json} (preferred) or {@code homes.json} (legacy)</li>
 *   <li>Warps: {@code warps.json} — {@code Map<String, Warp>} with nested location</li>
 *   <li>Spawns: {@code spawn.json} — v2 format: {@code Map<String, List<SpawnData>>}</li>
 *   <li>Kits: {@code kits.json} — {@code List<Kit>}</li>
 *   <li>Bans: {@code bans.json} (permanent) + {@code tempbans.json} (temporary)</li>
 *   <li>Player data: {@code players/<uuid>.json} — firstJoin, lastSeen, playTime (seconds)</li>
 * </ul>
 */
public class EliteEssentialsImporter extends AbstractEssentialsImporter {

  public EliteEssentialsImporter(@NotNull StorageProvider storageProvider,
                                 @Nullable BackupManager backupManager) {
    super(storageProvider, backupManager);
  }

  @Override
  @NotNull
  public String getSourceName() {
    return "EliteEssentials";
  }

  @Override
  @NotNull
  public String getDefaultPath() {
    return "mods/EliteEssentials";
  }

  @Override
  protected void doValidate(@NotNull Path sourcePath, @NotNull ImportValidationReport.Builder report) {
    // Count homes from player files
    int homeCount = 0;
    int playerCount = 0;
    Path playersDir = sourcePath.resolve("players");
    if (Files.isDirectory(playersDir)) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersDir, "*.json")) {
        for (Path playerFile : stream) {
          playerCount++;
          JsonObject playerObj = readJsonFile(playerFile);
          if (playerObj != null && playerObj.has("homes")) {
            JsonObject homes = playerObj.getAsJsonObject("homes");
            if (homes != null) {
              homeCount += homes.size();
            }
          }
        }
      } catch (IOException e) {
        report.warning("Failed to scan players directory: " + e.getMessage());
      }
    }

    // Fallback: check legacy homes.json
    if (homeCount == 0) {
      Path homesFile = sourcePath.resolve("homes.json");
      JsonObject homesObj = readJsonFile(homesFile);
      if (homesObj != null) {
        for (Map.Entry<String, JsonElement> entry : homesObj.entrySet()) {
          if (entry.getValue().isJsonObject()) {
            homeCount += entry.getValue().getAsJsonObject().size();
          }
        }
      }
    }

    // Count warps
    int warpCount = 0;
    Path warpsFile = sourcePath.resolve("warps.json");
    JsonObject warpsObj = readJsonFile(warpsFile);
    if (warpsObj != null) {
      warpCount = warpsObj.size();
    }

    // Count spawns
    int spawnCount = 0;
    Path spawnFile = sourcePath.resolve("spawn.json");
    JsonObject spawnObj = readJsonFile(spawnFile);
    if (spawnObj != null) {
      for (Map.Entry<String, JsonElement> entry : spawnObj.entrySet()) {
        if (entry.getValue().isJsonArray()) {
          spawnCount += entry.getValue().getAsJsonArray().size();
        }
      }
    }

    // Count kits
    int kitCount = 0;
    Path kitsFile = sourcePath.resolve("kits.json");
    JsonElement kitsElement = readJsonElement(kitsFile);
    if (kitsElement != null && kitsElement.isJsonArray()) {
      kitCount = kitsElement.getAsJsonArray().size();
    }

    report.totalHomes(homeCount)
        .totalWarps(warpCount)
        .totalSpawns(spawnCount)
        .totalKits(kitCount)
        .totalPlayers(playerCount);

    // Check for existing data conflicts
    checkWarpConflicts(report);
    checkKitConflicts(kitsFile, report);
  }

  @Override
  protected void doImport(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    importHomes(sourcePath, result);
    importWarps(sourcePath, result);
    importSpawns(sourcePath, result);
    importKits(sourcePath, result);
    importBans(sourcePath, result);
    importPlayerData(sourcePath, result);
  }

  // === Homes ===

  private void importHomes(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing homes...");

    // Prefer per-player files over legacy homes.json
    int[] homeCounter = {0};
    Path playersDir = sourcePath.resolve("players");
    if (Files.isDirectory(playersDir)) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersDir, "*.json")) {
        for (Path playerFile : stream) {
          importHomesFromPlayerFile(playerFile, result, homeCounter);
        }
      } catch (IOException e) {
        result.warning("Failed to scan players directory: " + e.getMessage());
      }
    }

    // Fallback: legacy homes.json (if no homes found in player files)
    if (homeCounter[0] > 0) return;
    Path homesFile = sourcePath.resolve("homes.json");
    JsonObject homesObj = readJsonFile(homesFile);
    if (homesObj == null) {
      progress("No homes data found");
      return;
    }

    // Format: Map<UUID, Map<String, Home>>
    for (Map.Entry<String, JsonElement> playerEntry : homesObj.entrySet()) {
      UUID playerUuid = parseUUID(playerEntry.getKey());
      if (playerUuid == null) {
        result.warning("Invalid player UUID in homes.json: " + playerEntry.getKey());
        continue;
      }

      if (!playerEntry.getValue().isJsonObject()) continue;
      JsonObject playerHomes = playerEntry.getValue().getAsJsonObject();

      importHomesForPlayer(playerUuid, "Unknown", playerHomes, result);
    }
  }

  private void importHomesFromPlayerFile(@NotNull Path playerFile, @NotNull ImportResult.Builder result,
                                        @NotNull int[] homeCounter) {
    JsonObject playerObj = readJsonFile(playerFile);
    if (playerObj == null) return;

    // Extract UUID — try "uuid" field first, then filename
    String uuidStr = getString(playerObj, "uuid");
    if (uuidStr == null) {
      String filename = playerFile.getFileName().toString();
      uuidStr = filename.replace(".json", "");
    }
    UUID playerUuid = parseUUID(uuidStr);
    if (playerUuid == null) return;

    String playerName = getString(playerObj, "name");
    if (playerName == null) playerName = getString(playerObj, "username");
    if (playerName == null) playerName = "Unknown";

    // Homes are in "homes" field
    if (!playerObj.has("homes") || !playerObj.get("homes").isJsonObject()) return;
    JsonObject homesObj = playerObj.getAsJsonObject("homes");
    homeCounter[0] += homesObj.size();

    importHomesForPlayer(playerUuid, playerName, homesObj, result);
  }

  private void importHomesForPlayer(@NotNull UUID playerUuid, @NotNull String playerName,
                                    @NotNull JsonObject homesObj, @NotNull ImportResult.Builder result) {
    if (homesObj.isEmpty()) return;

    PlayerHomes playerHomes;
    if (!dryRun) {
      playerHomes = storageProvider.getHomeStorage()
          .loadPlayerHomes(playerUuid).join()
          .orElse(new PlayerHomes(playerUuid, playerName));
    } else {
      playerHomes = null;
    }

    for (Map.Entry<String, JsonElement> homeEntry : homesObj.entrySet()) {
      if (!homeEntry.getValue().isJsonObject()) continue;
      JsonObject homeObj = homeEntry.getValue().getAsJsonObject();

      String homeName = getString(homeObj, "name");
      if (homeName == null) homeName = homeEntry.getKey();

      // Extract location (nested object)
      JsonObject location = homeObj.has("location") && homeObj.get("location").isJsonObject()
          ? homeObj.getAsJsonObject("location") : homeObj;

      String worldName = getString(location, "world");
      if (worldName == null) worldName = "default";

      WorldResolver.ResolvedWorld resolved = resolveWorldByName(worldName);

      Home home = new Home(
          homeName,
          resolved.worldName(),
          resolved.worldUuid(),
          getDouble(location, "x", 0),
          getDouble(location, "y", 64),
          getDouble(location, "z", 0),
          getFloat(location, "yaw", 0),
          getFloat(location, "pitch", 0),
          getLong(homeObj, "createdAt", System.currentTimeMillis()),
          getLong(homeObj, "createdAt", System.currentTimeMillis())
      );

      if (!dryRun && playerHomes != null) {
        if (playerHomes.hasHome(homeName) && !overwrite) {
          result.incrementHomesSkipped();
          continue;
        }
        playerHomes.setHome(home);
      }
      result.incrementHomesImported();
    }

    if (!dryRun && playerHomes != null && playerHomes.count() > 0) {
      storageProvider.getHomeStorage().savePlayerHomes(playerHomes).join();
    }
  }

  // === Warps ===

  private void importWarps(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing warps...");

    Path warpsFile = sourcePath.resolve("warps.json");
    JsonObject warpsObj = readJsonFile(warpsFile);
    if (warpsObj == null) {
      progress("No warps data found");
      return;
    }

    // Load existing warps for conflict checking
    Map<String, Warp> existingWarps = null;
    if (!dryRun) {
      existingWarps = storageProvider.getWarpStorage().loadAllWarps().join();
    }

    // Format: Map<String, Warp> where Warp has nested location
    for (Map.Entry<String, JsonElement> entry : warpsObj.entrySet()) {
      if (!entry.getValue().isJsonObject()) continue;
      JsonObject warpObj = entry.getValue().getAsJsonObject();

      String warpName = getString(warpObj, "name");
      if (warpName == null) warpName = entry.getKey();

      // Check for existing
      if (!dryRun && existingWarps != null && existingWarps.containsKey(warpName.toLowerCase()) && !overwrite) {
        result.incrementWarpsSkipped();
        continue;
      }

      // Extract location
      JsonObject location = warpObj.has("location") && warpObj.get("location").isJsonObject()
          ? warpObj.getAsJsonObject("location") : warpObj;

      String worldName = getString(location, "world");
      if (worldName == null) worldName = "default";

      WorldResolver.ResolvedWorld resolved = resolveWorldByName(worldName);

      // Map permission: "ALL" -> null, "OP" -> permission node
      String permStr = getString(warpObj, "permission");
      String permission = null;
      if ("OP".equalsIgnoreCase(permStr)) {
        permission = "hyperessentials.warp.use." + warpName.toLowerCase();
      }

      String createdBy = getString(warpObj, "createdBy");
      String description = getString(warpObj, "description");

      Warp warp = new Warp(
          UUID.randomUUID(),
          warpName,
          warpName,
          "imported",
          resolved.worldName(),
          resolved.worldUuid(),
          getDouble(location, "x", 0),
          getDouble(location, "y", 64),
          getDouble(location, "z", 0),
          getFloat(location, "yaw", 0),
          getFloat(location, "pitch", 0),
          permission,
          description,
          getLong(warpObj, "createdAt", System.currentTimeMillis()),
          null // createdBy is a player name, not UUID — store null
      );

      if (!dryRun) {
        storageProvider.getWarpStorage().saveWarp(warp).join();
      }
      result.incrementWarpsImported();
    }
  }

  // === Spawns ===

  private void importSpawns(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing spawns...");

    Path spawnFile = sourcePath.resolve("spawn.json");
    JsonObject spawnObj = readJsonFile(spawnFile);
    if (spawnObj == null) {
      progress("No spawns data found");
      return;
    }

    // Load existing spawns
    Map<String, Spawn> existingSpawns = null;
    if (!dryRun) {
      existingSpawns = storageProvider.getSpawnStorage().loadAllSpawns().join();
    }

    boolean firstSpawn = true;

    // v2 format: Map<String, List<SpawnData>> (key=world name)
    for (Map.Entry<String, JsonElement> worldEntry : spawnObj.entrySet()) {
      String worldName = worldEntry.getKey();

      if (!worldEntry.getValue().isJsonArray()) {
        // Might be v1/legacy single spawn format — try as flat object
        if (worldEntry.getValue().isJsonObject()) {
          importSingleSpawn(worldName, worldEntry.getValue().getAsJsonObject(), existingSpawns,
              firstSpawn, result);
          firstSpawn = false;
        }
        continue;
      }

      JsonArray spawnsArray = worldEntry.getValue().getAsJsonArray();
      for (JsonElement spawnElement : spawnsArray) {
        if (!spawnElement.isJsonObject()) continue;
        JsonObject spawnData = spawnElement.getAsJsonObject();

        boolean primary = getBoolean(spawnData, "primary", false);
        importSingleSpawn(worldName, spawnData, existingSpawns,
            primary || firstSpawn, result);
        if (primary || firstSpawn) firstSpawn = false;
      }
    }
  }

  private void importSingleSpawn(@NotNull String worldName, @NotNull JsonObject spawnData,
                                  @Nullable Map<String, Spawn> existingSpawns,
                                  boolean isGlobal, @NotNull ImportResult.Builder result) {
    WorldResolver.ResolvedWorld resolved = resolveWorldByName(worldName);

    // Check existing
    if (!dryRun && existingSpawns != null && existingSpawns.containsKey(resolved.worldUuid()) && !overwrite) {
      return;
    }

    Spawn spawn = new Spawn(
        resolved.worldUuid(),
        resolved.worldName(),
        getDouble(spawnData, "x", 0),
        getDouble(spawnData, "y", 64),
        getDouble(spawnData, "z", 0),
        getFloat(spawnData, "yaw", 0),
        getFloat(spawnData, "pitch", 0),
        isGlobal,
        System.currentTimeMillis(),
        null
    );

    if (!dryRun) {
      storageProvider.getSpawnStorage().saveSpawn(spawn).join();
    }
    result.incrementSpawnsImported();
  }

  // === Kits ===

  private void importKits(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing kits...");

    Path kitsFile = sourcePath.resolve("kits.json");
    JsonElement kitsElement = readJsonElement(kitsFile);
    if (kitsElement == null || !kitsElement.isJsonArray()) {
      progress("No kits data found");
      return;
    }

    // Load existing kits
    Map<String, Kit> existingKits = null;
    if (!dryRun) {
      existingKits = storageProvider.getKitStorage().loadAllKits().join();
    }

    JsonArray kitsArray = kitsElement.getAsJsonArray();
    for (JsonElement kitElement : kitsArray) {
      if (!kitElement.isJsonObject()) continue;
      JsonObject kitObj = kitElement.getAsJsonObject();

      String kitId = getString(kitObj, "id");
      if (kitId == null) kitId = getString(kitObj, "name");
      if (kitId == null) continue;

      // Check existing
      if (!dryRun && existingKits != null && existingKits.containsKey(kitId.toLowerCase()) && !overwrite) {
        result.incrementKitsSkipped();
        continue;
      }

      String displayName = getString(kitObj, "displayName");
      if (displayName == null) displayName = kitId;

      int cooldown = getInt(kitObj, "cooldown", 0); // already in seconds
      boolean oneTime = getBoolean(kitObj, "onetime", false);

      // Parse items
      List<KitItem> items = new ArrayList<>();
      if (kitObj.has("items") && kitObj.get("items").isJsonArray()) {
        JsonArray itemsArray = kitObj.getAsJsonArray("items");
        for (JsonElement itemElement : itemsArray) {
          if (!itemElement.isJsonObject()) continue;
          JsonObject itemObj = itemElement.getAsJsonObject();

          String itemId = getString(itemObj, "itemId");
          if (itemId == null) continue;

          int quantity = getInt(itemObj, "quantity", 1);
          int slot = getInt(itemObj, "slot", -1);
          String section = getString(itemObj, "section");
          if (section == null) section = KitItem.HOTBAR;

          items.add(new KitItem(itemId, quantity, slot, section));
        }
      }

      Kit kit = new Kit(
          UUID.randomUUID(),
          kitId.toLowerCase(),
          displayName,
          items,
          cooldown,
          oneTime,
          null
      );

      if (!dryRun) {
        storageProvider.getKitStorage().saveKit(kit).join();
      }
      result.incrementKitsImported();
    }
  }

  // === Bans ===

  private void importBans(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing punishments...");

    // Permanent bans
    Path bansFile = sourcePath.resolve("bans.json");
    JsonObject bansObj = readJsonFile(bansFile);
    if (bansObj != null) {
      for (Map.Entry<String, JsonElement> entry : bansObj.entrySet()) {
        UUID playerUuid = parseUUID(entry.getKey());
        if (playerUuid == null || !entry.getValue().isJsonObject()) continue;

        JsonObject banObj = entry.getValue().getAsJsonObject();
        importBanEntry(playerUuid, banObj, null, result);
      }
    }

    // Temporary bans
    Path tempBansFile = sourcePath.resolve("tempbans.json");
    JsonObject tempBansObj = readJsonFile(tempBansFile);
    if (tempBansObj != null) {
      for (Map.Entry<String, JsonElement> entry : tempBansObj.entrySet()) {
        UUID playerUuid = parseUUID(entry.getKey());
        if (playerUuid == null || !entry.getValue().isJsonObject()) continue;

        JsonObject banObj = entry.getValue().getAsJsonObject();
        long banEnd = getLong(banObj, "banEndTimestamp", 0);
        Instant expiresAt = banEnd > 0 ? Instant.ofEpochMilli(banEnd) : null;
        importBanEntry(playerUuid, banObj, expiresAt, result);
      }
    }

    if (bansObj == null && tempBansObj == null) {
      progress("No punishment data found");
    }
  }

  private void importBanEntry(@NotNull UUID playerUuid, @NotNull JsonObject banObj,
                               @Nullable Instant expiresAt, @NotNull ImportResult.Builder result) {
    String playerName = getString(banObj, "playerName");
    if (playerName == null) playerName = "Unknown";
    String bannedBy = getString(banObj, "bannedBy");
    if (bannedBy == null) bannedBy = "Console";
    String reason = getString(banObj, "reason");
    long bannedAt = getLong(banObj, "bannedAt", System.currentTimeMillis());

    Punishment punishment = new Punishment(
        UUID.randomUUID(),
        PunishmentType.BAN,
        playerUuid,
        playerName,
        null, // issuerUuid not stored by EliteEssentials
        bannedBy,
        reason,
        Instant.ofEpochMilli(bannedAt),
        expiresAt,
        true, // active
        null,
        null
    );

    if (!dryRun) {
      PlayerData playerData = storageProvider.getPlayerDataStorage()
          .loadPlayerData(playerUuid).join()
          .orElse(new PlayerData(playerUuid, playerName));
      playerData.addPunishment(punishment);
      storageProvider.getPlayerDataStorage().savePlayerData(playerData).join();
    }
    result.incrementPunishmentsImported();
  }

  // === Player Data ===

  private void importPlayerData(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing player data...");

    Path playersDir = sourcePath.resolve("players");
    if (!Files.isDirectory(playersDir)) {
      progress("No player data directory found");
      return;
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersDir, "*.json")) {
      for (Path playerFile : stream) {
        importPlayerDataFromFile(playerFile, result);
      }
    } catch (IOException e) {
      result.warning("Failed to scan players directory: " + e.getMessage());
    }
  }

  private void importPlayerDataFromFile(@NotNull Path playerFile, @NotNull ImportResult.Builder result) {
    JsonObject playerObj = readJsonFile(playerFile);
    if (playerObj == null) return;

    String uuidStr = getString(playerObj, "uuid");
    if (uuidStr == null) {
      uuidStr = playerFile.getFileName().toString().replace(".json", "");
    }
    UUID playerUuid = parseUUID(uuidStr);
    if (playerUuid == null) return;

    String playerName = getString(playerObj, "name");
    if (playerName == null) playerName = getString(playerObj, "username");
    if (playerName == null) playerName = "Unknown";

    if (dryRun) return;

    PlayerData data = storageProvider.getPlayerDataStorage()
        .loadPlayerData(playerUuid).join()
        .orElse(null);

    if (data != null && !overwrite) return;

    if (data == null) {
      data = new PlayerData(playerUuid, playerName);
    }
    data.setUsername(playerName);

    // firstJoin
    long firstJoin = getLong(playerObj, "firstJoin", 0);
    if (firstJoin > 0) {
      data.setFirstJoin(Instant.ofEpochMilli(firstJoin));
    }

    // lastSeen -> lastJoin
    long lastSeen = getLong(playerObj, "lastSeen", 0);
    if (lastSeen > 0) {
      data.setLastJoin(Instant.ofEpochMilli(lastSeen));
    }

    // playTime: EliteEssentials stores in seconds, HE uses milliseconds
    long playTimeSeconds = getLong(playerObj, "playTime", 0);
    if (playTimeSeconds > 0) {
      data.setTotalPlaytimeMs(playTimeSeconds * 1000L);
    }

    storageProvider.getPlayerDataStorage().savePlayerData(data).join();
  }

  // === Validation Helpers ===

  private void checkWarpConflicts(@NotNull ImportValidationReport.Builder report) {
    try {
      Map<String, Warp> existing = storageProvider.getWarpStorage().loadAllWarps().join();
      if (!existing.isEmpty()) {
        report.nameConflict(existing.size() + " existing warp(s) may conflict");
      }
    } catch (Exception ignored) {}
  }

  private void checkKitConflicts(@NotNull Path kitsFile, @NotNull ImportValidationReport.Builder report) {
    try {
      Map<String, Kit> existing = storageProvider.getKitStorage().loadAllKits().join();
      if (!existing.isEmpty()) {
        report.nameConflict(existing.size() + " existing kit(s) may conflict");
      }
    } catch (Exception ignored) {}
  }
}
