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
 * Imports data from EssentialsPlus (v1.16.1, by fof1092).
 *
 * <p>Key format differences:
 * <ul>
 *   <li>World references are <b>UUIDs</b> (not names)</li>
 *   <li>Rotation is {@code Vector3f} where x=pitch, y=yaw (swapped)</li>
 *   <li>Kit cooldowns are in <b>milliseconds</b> (divided by 1000 for HE)</li>
 *   <li>Playtime is in milliseconds (direct match with HE)</li>
 *   <li>Data files include a {@code version} field wrapper</li>
 *   <li>Kits stored as individual files in {@code kits/} directory</li>
 * </ul>
 */
public class EssentialsPlusImporter extends AbstractEssentialsImporter {

  public EssentialsPlusImporter(@NotNull StorageProvider storageProvider,
                                @Nullable BackupManager backupManager) {
    super(storageProvider, backupManager);
  }

  @Override
  @NotNull
  public String getSourceName() {
    return "EssentialsPlus";
  }

  @Override
  @NotNull
  public String getDefaultPath() {
    return "mods/EssentialsPlus";
  }

  @Override
  protected void doValidate(@NotNull Path sourcePath, @NotNull ImportValidationReport.Builder report) {
    // Count homes
    int homeCount = 0;
    Path homesFile = sourcePath.resolve("homes.json");
    JsonObject homesWrapper = readJsonFile(homesFile);
    if (homesWrapper != null && homesWrapper.has("homes") && homesWrapper.get("homes").isJsonArray()) {
      homeCount = homesWrapper.getAsJsonArray("homes").size();
    }

    // Count warps
    int warpCount = 0;
    Path warpsFile = sourcePath.resolve("warps.json");
    JsonObject warpsWrapper = readJsonFile(warpsFile);
    if (warpsWrapper != null && warpsWrapper.has("warps") && warpsWrapper.get("warps").isJsonArray()) {
      warpCount = warpsWrapper.getAsJsonArray("warps").size();
    }

    // Count spawns
    int spawnCount = 0;
    Path spawnsFile = sourcePath.resolve("spawns.json");
    JsonObject spawnsWrapper = readJsonFile(spawnsFile);
    if (spawnsWrapper != null && spawnsWrapper.has("spawns") && spawnsWrapper.get("spawns").isJsonArray()) {
      spawnCount = spawnsWrapper.getAsJsonArray("spawns").size();
    }

    // Count kits
    int kitCount = 0;
    Path kitsDir = sourcePath.resolve("kits");
    if (Files.isDirectory(kitsDir)) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(kitsDir, "*.json")) {
        for (Path ignored : stream) {
          kitCount++;
        }
      } catch (IOException e) {
        report.warning("Failed to scan kits directory: " + e.getMessage());
      }
    }

    // Count users
    int playerCount = 0;
    Path usersDir = sourcePath.resolve("users");
    if (Files.isDirectory(usersDir)) {
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(usersDir, "*.json")) {
        for (Path ignored : stream) {
          playerCount++;
        }
      } catch (IOException e) {
        report.warning("Failed to scan users directory: " + e.getMessage());
      }
    }

    report.totalHomes(homeCount)
        .totalWarps(warpCount)
        .totalSpawns(spawnCount)
        .totalKits(kitCount)
        .totalPlayers(playerCount);
  }

  @Override
  protected void doImport(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    importHomes(sourcePath, result);
    importWarps(sourcePath, result);
    importSpawns(sourcePath, result);
    importKits(sourcePath, result);
    importUsers(sourcePath, result);
  }

  // === Homes ===

  private void importHomes(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing homes...");

    Path homesFile = sourcePath.resolve("homes.json");
    JsonObject wrapper = readJsonFile(homesFile);
    if (wrapper == null || !wrapper.has("homes") || !wrapper.get("homes").isJsonArray()) {
      progress("No homes data found");
      return;
    }

    JsonArray homesArray = wrapper.getAsJsonArray("homes");

    // Group homes by player UUID for efficient batch loading/saving
    Map<UUID, List<JsonObject>> homesByPlayer = new java.util.LinkedHashMap<>();
    for (JsonElement element : homesArray) {
      if (!element.isJsonObject()) continue;
      JsonObject homeObj = element.getAsJsonObject();

      String playerUuidStr = getString(homeObj, "uuid");
      UUID playerUuid = parseUUID(playerUuidStr);
      if (playerUuid == null) {
        result.warning("Home with invalid player UUID: " + playerUuidStr);
        continue;
      }
      homesByPlayer.computeIfAbsent(playerUuid, k -> new ArrayList<>()).add(homeObj);
    }

    // Import homes per player (one load + one save per player)
    for (Map.Entry<UUID, List<JsonObject>> entry : homesByPlayer.entrySet()) {
      UUID playerUuid = entry.getKey();

      PlayerHomes playerHomes = null;
      if (!dryRun) {
        playerHomes = storageProvider.getHomeStorage()
            .loadPlayerHomes(playerUuid).join()
            .orElse(new PlayerHomes(playerUuid, "Unknown"));
      }

      for (JsonObject homeObj : entry.getValue()) {
        String homeName = getString(homeObj, "name");
        if (homeName == null) continue;

        // Position: Vector3d {x, y, z}
        double x = 0, y = 64, z = 0;
        if (homeObj.has("position") && homeObj.get("position").isJsonObject()) {
          JsonObject pos = homeObj.getAsJsonObject("position");
          x = getDouble(pos, "x", 0);
          y = getDouble(pos, "y", 64);
          z = getDouble(pos, "z", 0);
        }

        // Rotation: Vector3f {x=pitch, y=yaw, z=roll} — SWAP
        float yaw = 0, pitch = 0;
        if (homeObj.has("rotation") && homeObj.get("rotation").isJsonObject()) {
          JsonObject rot = homeObj.getAsJsonObject("rotation");
          pitch = getFloat(rot, "x", 0); // x = pitch
          yaw = getFloat(rot, "y", 0);   // y = yaw
        }

        // World: UUID string
        String worldUuidStr = getString(homeObj, "world");
        WorldResolver.ResolvedWorld resolved;
        if (worldUuidStr != null && !worldUuidStr.isEmpty()) {
          resolved = resolveWorldByUuid(worldUuidStr);
        } else {
          resolved = resolveWorldByName("default");
        }

        Home home = new Home(
            homeName,
            resolved.worldName(),
            resolved.worldUuid(),
            x, y, z, yaw, pitch,
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
  }

  // === Warps ===

  private void importWarps(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing warps...");

    Path warpsFile = sourcePath.resolve("warps.json");
    JsonObject wrapper = readJsonFile(warpsFile);
    if (wrapper == null || !wrapper.has("warps") || !wrapper.get("warps").isJsonArray()) {
      progress("No warps data found");
      return;
    }

    Map<String, Warp> existingWarps = null;
    if (!dryRun) {
      existingWarps = storageProvider.getWarpStorage().loadAllWarps().join();
    }

    JsonArray warpsArray = wrapper.getAsJsonArray("warps");
    for (JsonElement element : warpsArray) {
      if (!element.isJsonObject()) continue;
      JsonObject warpObj = element.getAsJsonObject();

      String warpName = getString(warpObj, "name");
      if (warpName == null) continue;

      if (!dryRun && existingWarps != null && existingWarps.containsKey(warpName.toLowerCase()) && !overwrite) {
        result.incrementWarpsSkipped();
        continue;
      }

      double x = 0, y = 64, z = 0;
      if (warpObj.has("position") && warpObj.get("position").isJsonObject()) {
        JsonObject pos = warpObj.getAsJsonObject("position");
        x = getDouble(pos, "x", 0);
        y = getDouble(pos, "y", 64);
        z = getDouble(pos, "z", 0);
      }

      float yaw = 0, pitch = 0;
      if (warpObj.has("rotation") && warpObj.get("rotation").isJsonObject()) {
        JsonObject rot = warpObj.getAsJsonObject("rotation");
        pitch = getFloat(rot, "x", 0);
        yaw = getFloat(rot, "y", 0);
      }

      String worldUuidStr = getString(warpObj, "world");
      WorldResolver.ResolvedWorld resolved;
      if (worldUuidStr != null && !worldUuidStr.isEmpty()) {
        resolved = resolveWorldByUuid(worldUuidStr);
      } else {
        resolved = resolveWorldByName("default");
      }

      Warp warp = new Warp(
          UUID.randomUUID(),
          warpName,
          warpName,
          "imported",
          resolved.worldName(),
          resolved.worldUuid(),
          x, y, z, yaw, pitch,
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

  // === Spawns ===

  private void importSpawns(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing spawns...");

    Path spawnsFile = sourcePath.resolve("spawns.json");
    JsonObject wrapper = readJsonFile(spawnsFile);
    if (wrapper == null || !wrapper.has("spawns") || !wrapper.get("spawns").isJsonArray()) {
      progress("No spawns data found");
      return;
    }

    Map<String, Spawn> existingSpawns = null;
    if (!dryRun) {
      existingSpawns = storageProvider.getSpawnStorage().loadAllSpawns().join();
    }

    JsonArray spawnsArray = wrapper.getAsJsonArray("spawns");
    for (JsonElement element : spawnsArray) {
      if (!element.isJsonObject()) continue;
      JsonObject spawnObj = element.getAsJsonObject();

      double x = 0, y = 64, z = 0;
      if (spawnObj.has("position") && spawnObj.get("position").isJsonObject()) {
        JsonObject pos = spawnObj.getAsJsonObject("position");
        x = getDouble(pos, "x", 0);
        y = getDouble(pos, "y", 64);
        z = getDouble(pos, "z", 0);
      }

      float yaw = 0, pitch = 0;
      if (spawnObj.has("rotation") && spawnObj.get("rotation").isJsonObject()) {
        JsonObject rot = spawnObj.getAsJsonObject("rotation");
        pitch = getFloat(rot, "x", 0);
        yaw = getFloat(rot, "y", 0);
      }

      String worldUuidStr = getString(spawnObj, "world");
      WorldResolver.ResolvedWorld resolved;
      if (worldUuidStr != null && !worldUuidStr.isEmpty()) {
        resolved = resolveWorldByUuid(worldUuidStr);
      } else {
        resolved = resolveWorldByName("default");
      }

      boolean isGlobal = getBoolean(spawnObj, "mainSpawn", false);

      if (!dryRun && existingSpawns != null && existingSpawns.containsKey(resolved.worldUuid()) && !overwrite) {
        continue;
      }

      Spawn spawn = new Spawn(
          resolved.worldUuid(),
          resolved.worldName(),
          x, y, z, yaw, pitch,
          isGlobal,
          System.currentTimeMillis(),
          null
      );

      if (!dryRun) {
        storageProvider.getSpawnStorage().saveSpawn(spawn).join();
      }
      result.incrementSpawnsImported();
    }
  }

  // === Kits ===

  private void importKits(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing kits...");

    Path kitsDir = sourcePath.resolve("kits");
    if (!Files.isDirectory(kitsDir)) {
      progress("No kits directory found");
      return;
    }

    Map<String, Kit> existingKits = null;
    if (!dryRun) {
      existingKits = storageProvider.getKitStorage().loadAllKits().join();
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(kitsDir, "*.json")) {
      for (Path kitFile : stream) {
        importKitFile(kitFile, existingKits, result);
      }
    } catch (IOException e) {
      result.warning("Failed to scan kits directory: " + e.getMessage());
    }
  }

  private void importKitFile(@NotNull Path kitFile, @Nullable Map<String, Kit> existingKits,
                              @NotNull ImportResult.Builder result) {
    JsonObject kitObj = readJsonFile(kitFile);
    if (kitObj == null) return;

    String kitName = getString(kitObj, "name");
    if (kitName == null) {
      kitName = kitFile.getFileName().toString().replace(".json", "");
    }

    if (!dryRun && existingKits != null && existingKits.containsKey(kitName.toLowerCase()) && !overwrite) {
      result.incrementKitsSkipped();
      return;
    }

    // Cooldown: EssentialsPlus stores in milliseconds, HE uses seconds
    long cooldownMs = getLong(kitObj, "cooldown", 0);
    int cooldownSeconds = (int) (cooldownMs / 1000);

    // Parse items from container sections
    List<KitItem> items = new ArrayList<>();
    extractContainerItems(kitObj, "hotbar", KitItem.HOTBAR, items);
    extractContainerItems(kitObj, "storage", KitItem.STORAGE, items);
    extractContainerItems(kitObj, "armor", KitItem.ARMOR, items);
    extractContainerItems(kitObj, "utility", KitItem.UTILITY, items);

    Kit kit = new Kit(
        UUID.randomUUID(),
        kitName.toLowerCase(),
        kitName,
        items,
        cooldownSeconds,
        false,
        null
    );

    if (!dryRun) {
      storageProvider.getKitStorage().saveKit(kit).join();
    }
    result.incrementKitsImported();
  }

  /**
   * Extracts items from an EssentialsPlus SerializableItemContainer section.
   * Format: { "capacity": N, "items": { "slot": { "itemId": "...", "count": N, ... } } }
   */
  private void extractContainerItems(@NotNull JsonObject kitObj, @NotNull String containerName,
                                     @NotNull String section, @NotNull List<KitItem> items) {
    if (!kitObj.has(containerName) || !kitObj.get(containerName).isJsonObject()) return;
    JsonObject container = kitObj.getAsJsonObject(containerName);

    if (!container.has("items") || !container.get("items").isJsonObject()) return;
    JsonObject itemsMap = container.getAsJsonObject("items");

    for (Map.Entry<String, JsonElement> entry : itemsMap.entrySet()) {
      if (!entry.getValue().isJsonObject()) continue;
      JsonObject itemObj = entry.getValue().getAsJsonObject();

      String itemId = getString(itemObj, "itemId");
      if (itemId == null) continue;

      int count = getInt(itemObj, "count", 1);
      int slot;
      try {
        slot = Integer.parseInt(entry.getKey());
      } catch (NumberFormatException e) {
        slot = -1;
      }

      items.add(new KitItem(itemId, count, slot, section));
    }
  }

  // === Users (bans, mutes, player data) ===

  private void importUsers(@NotNull Path sourcePath, @NotNull ImportResult.Builder result) {
    progress("Importing user data...");

    Path usersDir = sourcePath.resolve("users");
    if (!Files.isDirectory(usersDir)) {
      progress("No users directory found");
      return;
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(usersDir, "*.json")) {
      for (Path userFile : stream) {
        importUserFile(userFile, result);
      }
    } catch (IOException e) {
      result.warning("Failed to scan users directory: " + e.getMessage());
    }
  }

  private void importUserFile(@NotNull Path userFile, @NotNull ImportResult.Builder result) {
    JsonObject userObj = readJsonFile(userFile);
    if (userObj == null) return;

    String uuidStr = getString(userObj, "uuid");
    if (uuidStr == null) {
      uuidStr = userFile.getFileName().toString().replace(".json", "");
    }
    UUID playerUuid = parseUUID(uuidStr);
    if (playerUuid == null) return;

    String username = getString(userObj, "username");
    if (username == null) username = "Unknown";

    if (dryRun) {
      // Count punishments for dry run reporting
      if (userObj.has("tempBannedUser") && userObj.get("tempBannedUser").isJsonObject()) {
        result.incrementPunishmentsImported();
      }
      if (userObj.has("mutedUser") && userObj.get("mutedUser").isJsonObject()) {
        result.incrementPunishmentsImported();
      }
      return;
    }

    PlayerData data = storageProvider.getPlayerDataStorage()
        .loadPlayerData(playerUuid).join()
        .orElse(null);

    if (data != null && !overwrite) return;

    if (data == null) {
      data = new PlayerData(playerUuid, username);
    }
    data.setUsername(username);

    // firstJoinTimestamp
    long firstJoin = getLong(userObj, "firstJoinTimestamp", 0);
    if (firstJoin > 0) {
      data.setFirstJoin(Instant.ofEpochMilli(firstJoin));
    }

    // lastJoinTimestamp -> lastJoin
    long lastJoin = getLong(userObj, "lastJoinTimestamp", 0);
    if (lastJoin > 0) {
      data.setLastJoin(Instant.ofEpochMilli(lastJoin));
    }

    // playtime: EssentialsPlus stores in milliseconds (direct match)
    long playtimeMs = getLong(userObj, "playtime", 0);
    if (playtimeMs > 0) {
      data.setTotalPlaytimeMs(playtimeMs);
    }

    // Import ban (tempBannedUser field)
    if (userObj.has("tempBannedUser") && userObj.get("tempBannedUser").isJsonObject()) {
      JsonObject banObj = userObj.getAsJsonObject("tempBannedUser");
      long banEnd = getLong(banObj, "banEndTimestamp", 0);
      // -1 = permanent in EssentialsPlus
      Instant expiresAt = (banEnd > 0) ? Instant.ofEpochMilli(banEnd) : null;

      String reason = getString(banObj, "reason");
      String bannedBy = getString(banObj, "bannedBy");
      if (bannedBy == null) bannedBy = "Console";

      Punishment ban = new Punishment(
          UUID.randomUUID(),
          PunishmentType.BAN,
          playerUuid,
          username,
          null,
          bannedBy,
          reason,
          Instant.now(),
          expiresAt,
          true,
          null, null
      );
      data.addPunishment(ban);
      result.incrementPunishmentsImported();
    }

    // Import mute (mutedUser field)
    if (userObj.has("mutedUser") && userObj.get("mutedUser").isJsonObject()) {
      JsonObject muteObj = userObj.getAsJsonObject("mutedUser");
      long muteEnd = getLong(muteObj, "muteEndTimestamp", 0);
      Instant expiresAt = (muteEnd > 0) ? Instant.ofEpochMilli(muteEnd) : null;

      String reason = getString(muteObj, "reason");

      Punishment mute = new Punishment(
          UUID.randomUUID(),
          PunishmentType.MUTE,
          playerUuid,
          username,
          null,
          "Console",
          reason,
          Instant.now(),
          expiresAt,
          true,
          null, null
      );
      data.addPunishment(mute);
      result.incrementPunishmentsImported();
    }

    storageProvider.getPlayerDataStorage().savePlayerData(data).join();
  }
}
