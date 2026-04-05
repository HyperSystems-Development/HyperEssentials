package com.hyperessentials.importer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hyperessentials.backup.BackupManager;
import com.hyperessentials.data.Home;
import com.hyperessentials.data.PlayerData;
import com.hyperessentials.data.PlayerHomes;
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
 * Imports data from hyessentialsx (v1.4.1, by thelegacyvoyage).
 *
 * <p>Key format details:
 * <ul>
 *   <li>Homes: {@code players/<uuid>.json} — embedded {@code homes} map, dual world ref (worldId + worldName)</li>
 *   <li>Warps: {@code warps.json} — {@code Map<String, WarpModel>} with worldName</li>
 *   <li>Spawns: <b>Skipped</b> — in-memory only, no persistent file</li>
 *   <li>Kits: {@code kits.json} — {@code Map<String, KitModel>}, cooldownSeconds, maxUses</li>
 *   <li>Bans/Mutes: {@code players/<uuid>.json} — {@code ban} and {@code mute} fields, expiresAt 0=permanent</li>
 *   <li>IP Bans: {@code ipbans.json}</li>
 *   <li>Player data: playtimeSeconds (×1000 to ms)</li>
 * </ul>
 */
public class HyEssentialsXImporter extends AbstractEssentialsImporter {

  public HyEssentialsXImporter(@NotNull StorageProvider storageProvider,
                               @Nullable BackupManager backupManager) {
    super(storageProvider, backupManager);
  }

  @Override
  @NotNull
  public String getSourceName() {
    return "hyessentialsx";
  }

  @Override
  @NotNull
  public String getDefaultPath() {
    return "mods/xyz.thelegacyvoyage_hyessentialsx";
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

    int kitCount = 0;
    Path kitsFile = sourcePath.resolve("kits.json");
    JsonObject kitsObj = readJsonFile(kitsFile);
    if (kitsObj != null) {
      kitCount = kitsObj.size();
    }

    report.totalHomes(homeCount)
        .totalWarps(warpCount)
        .totalSpawns(0)
        .totalKits(kitCount)
        .totalPlayers(playerCount);

    report.warning("Spawns are in-memory only in hyessentialsx — spawn import not available");
  }

  @Override
  protected void doImport(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    importHomes(sourcePath, result);
    importWarps(sourcePath, result);
    importKits(sourcePath, result);
    importPlayerData(sourcePath, result);

    progress("Skipped spawns (in-memory only in hyessentialsx)");
    result.warning("Spawns not imported — hyessentialsx stores spawns in memory only");
  }

  // === Homes ===

  private void importHomes(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing homes...");

    Path playersDir = sourcePath.resolve("players");
    if (!Files.isDirectory(playersDir)) {
      progress("No players directory found");
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

    String filename = playerFile.getFileName().toString().replace(".json", "");
    UUID playerUuid = parseUUID(filename);
    if (playerUuid == null) return;

    if (!playerObj.has("homes") || !playerObj.get("homes").isJsonObject()) return;
    JsonObject homesObj = playerObj.getAsJsonObject("homes");
    if (homesObj.isEmpty()) return;

    String playerName = getString(playerObj, "lastKnownName");
    if (playerName == null) playerName = "Unknown";

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

      // Dual world reference: worldId (nullable UUID) + worldName
      String worldId = getString(homeObj, "worldId");
      String worldName = getString(homeObj, "worldName");

      WorldResolver.ResolvedWorld resolved;
      if (worldId != null && !worldId.isEmpty() && !"null".equals(worldId)) {
        resolved = resolveWorldByUuid(worldId);
      } else if (worldName != null && !worldName.isEmpty()) {
        resolved = resolveWorldByName(worldName);
      } else {
        resolved = resolveWorldByName("default");
      }

      Home home = new Home(
          homeName,
          resolved.worldName(),
          resolved.worldUuid(),
          getDouble(homeObj, "x", 0),
          getDouble(homeObj, "y", 64),
          getDouble(homeObj, "z", 0),
          getFloat(homeObj, "yaw", 0),
          getFloat(homeObj, "pitch", 0),
          System.currentTimeMillis(),
          System.currentTimeMillis()
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

    // Format: Map<String, WarpModel>
    for (Map.Entry<String, JsonElement> entry : warpsObj.entrySet()) {
      if (!entry.getValue().isJsonObject()) continue;
      JsonObject warpObj = entry.getValue().getAsJsonObject();

      String warpName = getString(warpObj, "name");
      if (warpName == null) warpName = entry.getKey();

      if (!dryRun && existingWarps != null && existingWarps.containsKey(warpName.toLowerCase()) && !overwrite) {
        result.incrementWarpsSkipped();
        continue;
      }

      String worldName = getString(warpObj, "worldName");
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
          null, null,
          System.currentTimeMillis(),
          null
      );

      if (!dryRun) {
        storageProvider.getWarpStorage().saveWarp(warp).join();
      }
      result.incrementWarpsImported();
    }
  }

  // === Kits ===

  private void importKits(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing kits...");

    Path kitsFile = sourcePath.resolve("kits.json");
    JsonObject kitsObj = readJsonFile(kitsFile);
    if (kitsObj == null) {
      progress("No kits data found");
      return;
    }

    Map<String, Kit> existingKits = null;
    if (!dryRun) {
      existingKits = storageProvider.getKitStorage().loadAllKits().join();
    }

    // Format: Map<String, KitModel>
    for (Map.Entry<String, JsonElement> entry : kitsObj.entrySet()) {
      if (!entry.getValue().isJsonObject()) continue;
      JsonObject kitObj = entry.getValue().getAsJsonObject();

      String kitName = getString(kitObj, "name");
      if (kitName == null) kitName = entry.getKey();

      if (!dryRun && existingKits != null && existingKits.containsKey(kitName.toLowerCase()) && !overwrite) {
        result.incrementKitsSkipped();
        continue;
      }

      int cooldown = getInt(kitObj, "cooldownSeconds", 0);
      int maxUses = getInt(kitObj, "maxUses", 0);
      boolean oneTime = (maxUses == 1);

      // Parse items
      List<KitItem> items = new ArrayList<>();
      if (kitObj.has("items") && kitObj.get("items").isJsonArray()) {
        for (JsonElement itemEl : kitObj.getAsJsonArray("items")) {
          if (!itemEl.isJsonObject()) continue;
          JsonObject itemObj = itemEl.getAsJsonObject();

          String itemId = getString(itemObj, "itemId");
          if (itemId == null) continue;

          int quantity = getInt(itemObj, "quantity", 1);
          int slot = getInt(itemObj, "slot", -1);
          // hyessentialsx doesn't store section — default to hotbar
          items.add(new KitItem(itemId, quantity, slot, KitItem.HOTBAR));
        }
      }

      Kit kit = new Kit(
          UUID.randomUUID(),
          kitName.toLowerCase(),
          kitName,
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

  // === Player Data (bans, mutes, stats) ===

  private void importPlayerData(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing player data and punishments...");

    Path playersDir = sourcePath.resolve("players");
    if (!Files.isDirectory(playersDir)) return;

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(playersDir, "*.json")) {
      for (Path playerFile : stream) {
        importPlayerDataFromFile(playerFile, result);
      }
    } catch (IOException e) {
      result.warning("Failed to scan players directory: " + e.getMessage());
    }

    // IP bans
    importIpBans(sourcePath, result);
  }

  private void importPlayerDataFromFile(@NotNull Path playerFile, @NotNull ImportResult.Builder result) {
    JsonObject playerObj = readJsonFile(playerFile);
    if (playerObj == null) return;

    String filename = playerFile.getFileName().toString().replace(".json", "");
    UUID playerUuid = parseUUID(filename);
    if (playerUuid == null) return;

    String playerName = getString(playerObj, "lastKnownName");
    if (playerName == null) playerName = "Unknown";

    boolean hasBan = playerObj.has("ban") && playerObj.get("ban").isJsonObject();
    boolean hasMute = playerObj.has("mute") && playerObj.get("mute").isJsonObject();
    boolean hasStats = playerObj.has("firstJoinAt") || playerObj.has("playtimeSeconds");

    if (dryRun) {
      if (hasBan) result.incrementPunishmentsImported();
      if (hasMute) result.incrementPunishmentsImported();
      return;
    }

    if (!hasBan && !hasMute && !hasStats) return;

    PlayerData data = storageProvider.getPlayerDataStorage()
        .loadPlayerData(playerUuid).join()
        .orElse(null);

    if (data != null && !overwrite) return;

    if (data == null) {
      data = new PlayerData(playerUuid, playerName);
    }
    data.setUsername(playerName);

    // Stats
    long firstJoin = getLong(playerObj, "firstJoinAt", 0);
    if (firstJoin > 0) {
      data.setFirstJoin(Instant.ofEpochMilli(firstJoin));
    }

    long lastJoin = getLong(playerObj, "lastJoinAt", 0);
    if (lastJoin > 0) {
      data.setLastJoin(Instant.ofEpochMilli(lastJoin));
    }

    // playtimeSeconds -> ms
    long playtimeSeconds = getLong(playerObj, "playtimeSeconds", 0);
    if (playtimeSeconds > 0) {
      data.setTotalPlaytimeMs(playtimeSeconds * 1000L);
    }

    // Ban: expiresAt 0 = permanent
    if (hasBan) {
      JsonObject banObj = playerObj.getAsJsonObject("ban");
      long expiresAt = getLong(banObj, "expiresAt", 0);
      Instant expiresInstant = (expiresAt > 0) ? Instant.ofEpochMilli(expiresAt) : null;

      String reason = getString(banObj, "reason");
      String actorName = getString(banObj, "actorName");
      if (actorName == null) actorName = "Console";
      long createdAt = getLong(banObj, "createdAt", System.currentTimeMillis());

      Punishment ban = new Punishment(
          UUID.randomUUID(),
          PunishmentType.BAN,
          playerUuid,
          playerName,
          null,
          actorName,
          reason,
          Instant.ofEpochMilli(createdAt),
          expiresInstant,
          true,
          null, null
      );
      data.addPunishment(ban);
      result.incrementPunishmentsImported();
    }

    // Mute
    if (hasMute) {
      JsonObject muteObj = playerObj.getAsJsonObject("mute");
      long expiresAt = getLong(muteObj, "expiresAt", 0);
      Instant expiresInstant = (expiresAt > 0) ? Instant.ofEpochMilli(expiresAt) : null;

      String reason = getString(muteObj, "reason");
      String actorName = getString(muteObj, "actorName");
      if (actorName == null) actorName = "Console";
      long createdAt = getLong(muteObj, "createdAt", System.currentTimeMillis());

      Punishment mute = new Punishment(
          UUID.randomUUID(),
          PunishmentType.MUTE,
          playerUuid,
          playerName,
          null,
          actorName,
          reason,
          Instant.ofEpochMilli(createdAt),
          expiresInstant,
          true,
          null, null
      );
      data.addPunishment(mute);
      result.incrementPunishmentsImported();
    }

    storageProvider.getPlayerDataStorage().savePlayerData(data).join();
  }

  // === IP Bans ===

  private void importIpBans(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    Path ipBansFile = sourcePath.resolve("ipbans.json");
    JsonObject ipBansObj = readJsonFile(ipBansFile);
    if (ipBansObj == null || ipBansObj.isEmpty()) return;

    progress("Importing IP bans...");

    // IP bans in hyessentialsx are Map<String, IpBanModel>
    // HyperEssentials stores IP bans separately, but we log them as warnings
    // since the import system currently imports into PlayerData punishments
    int count = 0;
    for (Map.Entry<String, JsonElement> entry : ipBansObj.entrySet()) {
      if (!entry.getValue().isJsonObject()) continue;
      JsonObject ipBanObj = entry.getValue().getAsJsonObject();

      String ip = getString(ipBanObj, "ip");
      if (ip == null) ip = entry.getKey();

      String playerUuidStr = getString(ipBanObj, "playerUuid");
      UUID playerUuid = parseUUID(playerUuidStr);
      String playerName = getString(ipBanObj, "playerName");
      if (playerName == null) playerName = "Unknown";
      String actorName = getString(ipBanObj, "actorName");
      if (actorName == null) actorName = "Console";
      String reason = getString(ipBanObj, "reason");
      long createdAt = getLong(ipBanObj, "createdAt", System.currentTimeMillis());

      if (playerUuid != null && !dryRun) {
        // Store as IPBAN punishment on the player
        PlayerData data = storageProvider.getPlayerDataStorage()
            .loadPlayerData(playerUuid).join()
            .orElse(new PlayerData(playerUuid, playerName));

        Punishment ipBan = new Punishment(
            UUID.randomUUID(),
            PunishmentType.IPBAN,
            playerUuid,
            playerName,
            null,
            actorName,
            (reason != null ? reason + " " : "") + "[IP: " + ip + "]",
            Instant.ofEpochMilli(createdAt),
            null, // permanent
            true,
            null, null
        );
        data.addPunishment(ipBan);
        storageProvider.getPlayerDataStorage().savePlayerData(data).join();
      }
      result.incrementPunishmentsImported();
      count++;
    }

    if (count > 0) {
      progress("Imported " + count + " IP ban(s)");
    }
  }
}
