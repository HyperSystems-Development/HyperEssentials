package com.hyperessentials.importer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hyperessentials.backup.BackupManager;
import com.hyperessentials.data.Home;
import com.hyperessentials.data.PlayerHomes;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.data.Warp;
import com.hyperessentials.storage.StorageProvider;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Imports data from Essentials by nhulston (v1.8.0).
 *
 * <p>Storage format:
 * <ul>
 *   <li>Homes: {@code players/<uuid>.json} — embedded {@code homes} map</li>
 *   <li>Warps: {@code warps.json} — {@code Map<String, Warp>} with flat location fields</li>
 *   <li>Spawns: {@code spawn.json} — single flat spawn object</li>
 *   <li>Kits: {@code kits.toml} — TOML format, <b>skipped</b></li>
 *   <li>No punishments system</li>
 * </ul>
 */
public class NhulstonEssentialsImporter extends AbstractEssentialsImporter {

  public NhulstonEssentialsImporter(@NotNull StorageProvider storageProvider,
                                    @Nullable BackupManager backupManager) {
    super(storageProvider, backupManager);
  }

  @Override
  @NotNull
  public String getSourceName() {
    return "Essentials (nhulston)";
  }

  @Override
  @NotNull
  public String getDefaultPath() {
    return "mods/com.nhulston_Essentials";
  }

  @Override
  protected void doValidate(@NotNull Path sourcePath, @NotNull ImportValidationReport.Builder report) {
    int homeCount = 0;
    int playerCount = 0;
    Path playersDir = sourcePath.resolve("players");
    if (Files.isDirectory(playersDir)) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersDir, "*.json")) {
        for (Path playerFile : stream) {
          playerCount++;
          JsonObject playerObj = readJsonFile(playerFile);
          if (playerObj != null && playerObj.has("homes") && playerObj.get("homes").isJsonObject()) {
            homeCount += playerObj.getAsJsonObject("homes").size();
          }
        }
      } catch (IOException e) {
        report.warning("Failed to scan players directory: " + e.getMessage());
      }
    }

    int warpCount = 0;
    Path warpsFile = sourcePath.resolve("warps.json");
    JsonObject warpsObj = readJsonFile(warpsFile);
    if (warpsObj != null) {
      warpCount = warpsObj.size();
    }

    int spawnCount = 0;
    Path spawnFile = sourcePath.resolve("spawn.json");
    if (Files.exists(spawnFile)) {
      spawnCount = 1;
    }

    report.totalHomes(homeCount)
        .totalWarps(warpCount)
        .totalSpawns(spawnCount)
        .totalKits(0)
        .totalPlayers(playerCount);

    // Check for kits.toml
    if (Files.exists(sourcePath.resolve("kits.toml"))) {
      report.warning("kits.toml found but TOML kit import is not supported (use JSON format)");
    }
  }

  @Override
  protected void doImport(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    importHomes(sourcePath, result);
    importWarps(sourcePath, result);
    importSpawns(sourcePath, result);

    // Log skip for kits
    if (Files.exists(sourcePath.resolve("kits.toml"))) {
      result.warning("Skipped kits.toml — TOML kit import is not supported");
      progress("Skipped kits (TOML format not supported)");
    }

    // No punishments in nhulston Essentials
  }

  // === Homes ===

  private void importHomes(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing homes...");

    Path playersDir = sourcePath.resolve("players");
    if (!Files.isDirectory(playersDir)) {
      progress("No player data directory found");
      return;
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersDir, "*.json")) {
      for (Path playerFile : stream) {
        importHomesFromPlayerFile(playerFile, result);
      }
    } catch (IOException e) {
      result.warning("Failed to scan players directory: " + e.getMessage());
    }
  }

  private void importHomesFromPlayerFile(@NotNull Path playerFile, @NotNull ImportResult.Builder result) {
    JsonObject playerObj = readJsonFile(playerFile);
    if (playerObj == null) return;

    // UUID from filename
    String filename = playerFile.getFileName().toString().replace(".json", "");
    UUID playerUuid = parseUUID(filename);
    if (playerUuid == null) return;

    if (!playerObj.has("homes") || !playerObj.get("homes").isJsonObject()) return;
    JsonObject homesObj = playerObj.getAsJsonObject("homes");
    if (homesObj.isEmpty()) return;

    PlayerHomes playerHomes;
    if (!dryRun) {
      playerHomes = storageProvider.getHomeStorage()
          .loadPlayerHomes(playerUuid).join()
          .orElse(new PlayerHomes(playerUuid, "Unknown"));
    } else {
      playerHomes = null;
    }

    for (Map.Entry<String, JsonElement> homeEntry : homesObj.entrySet()) {
      if (!homeEntry.getValue().isJsonObject()) continue;
      JsonObject homeObj = homeEntry.getValue().getAsJsonObject();

      String homeName = homeEntry.getKey();

      // Flat location fields (no nested "location" object)
      String worldName = getString(homeObj, "world");
      if (worldName == null) worldName = "default";

      WorldResolver.ResolvedWorld resolved = resolveWorldByName(worldName);

      Home home = new Home(
          homeName,
          resolved.worldName(),
          resolved.worldUuid(),
          getDouble(homeObj, "x", 0),
          getDouble(homeObj, "y", 64),
          getDouble(homeObj, "z", 0),
          getFloat(homeObj, "yaw", 0),
          getFloat(homeObj, "pitch", 0),
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

    Map<String, Warp> existingWarps = null;
    if (!dryRun) {
      existingWarps = storageProvider.getWarpStorage().loadAllWarps().join();
    }

    // Format: Map<String, Warp> with flat location fields (no nested location)
    for (Map.Entry<String, JsonElement> entry : warpsObj.entrySet()) {
      if (!entry.getValue().isJsonObject()) continue;
      JsonObject warpObj = entry.getValue().getAsJsonObject();

      String warpName = entry.getKey();

      if (!dryRun && existingWarps != null && existingWarps.containsKey(warpName.toLowerCase()) && !overwrite) {
        result.incrementWarpsSkipped();
        continue;
      }

      String worldName = getString(warpObj, "world");
      if (worldName == null) worldName = "default";

      WorldResolver.ResolvedWorld resolved = resolveWorldByName(worldName);

      Warp warp = new Warp(
          UUID.randomUUID(),
          warpName,
          warpName,
          "imported",
          resolved.worldName(),
          resolved.worldUuid(),
          getDouble(warpObj, "x", 0),
          getDouble(warpObj, "y", 64),
          getDouble(warpObj, "z", 0),
          getFloat(warpObj, "yaw", 0),
          getFloat(warpObj, "pitch", 0),
          null, // no permission in nhulston format
          null, // no description
          System.currentTimeMillis(),
          null
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
      progress("No spawn data found");
      return;
    }

    // Single flat spawn object with world, x, y, z, yaw, pitch
    String worldName = getString(spawnObj, "world");
    if (worldName == null) worldName = "default";

    WorldResolver.ResolvedWorld resolved = resolveWorldByName(worldName);

    // Check existing
    if (!dryRun) {
      Map<String, Spawn> existingSpawns = storageProvider.getSpawnStorage().loadAllSpawns().join();
      if (existingSpawns.containsKey(resolved.worldUuid()) && !overwrite) {
        progress("Spawn already exists for world " + resolved.worldName() + ", skipping");
        return;
      }
    }

    Spawn spawn = new Spawn(
        resolved.worldUuid(),
        resolved.worldName(),
        getDouble(spawnObj, "x", 0),
        getDouble(spawnObj, "y", 64),
        getDouble(spawnObj, "z", 0),
        getFloat(spawnObj, "yaw", 0),
        getFloat(spawnObj, "pitch", 0),
        true, // single spawn → global
        System.currentTimeMillis(),
        null
    );

    if (!dryRun) {
      storageProvider.getSpawnStorage().saveSpawn(spawn).join();
    }
    result.incrementSpawnsImported();
  }
}
