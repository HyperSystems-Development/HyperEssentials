package com.hyperessentials.module.moderation.storage;

import com.google.gson.*;
import com.hyperessentials.module.moderation.data.IpBan;
import com.hyperessentials.storage.StorageUtils;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON persistence for IP bans only.
 * Per-player punishments are now stored in PlayerData via PlayerDataStorage.
 * File: data/ipbans.json
 */
public class IpBanStorage {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  private final Path filePath;
  private final Map<String, IpBan> ipBans = new ConcurrentHashMap<>();

  public IpBanStorage(@NotNull Path dataDir) {
    this.filePath = dataDir.resolve("data").resolve("ipbans.json");
  }

  public void load() {
    ipBans.clear();

    if (!Files.exists(filePath)) {
      Logger.info("[IpBanStorage] No IP bans file found, starting fresh");
      return;
    }

    try {
      String json = Files.readString(filePath);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();

      if (root.has("ipBans") && root.get("ipBans").isJsonObject()) {
        JsonObject bans = root.getAsJsonObject("ipBans");
        for (Map.Entry<String, JsonElement> entry : bans.entrySet()) {
          try {
            IpBan ban = deserializeIpBan(entry.getKey(), entry.getValue().getAsJsonObject());
            if (ban != null) ipBans.put(entry.getKey(), ban);
          } catch (Exception e) {
            Logger.warn("[IpBanStorage] Failed to parse IP ban for %s: %s", entry.getKey(), e.getMessage());
          }
        }
      }

      Logger.info("[IpBanStorage] Loaded %d IP ban(s)", ipBans.size());
    } catch (Exception e) {
      Logger.severe("[IpBanStorage] Failed to load: %s", e.getMessage());
      StorageUtils.recoverFromBackup(filePath);
    }
  }

  public void save() {
    try {
      JsonObject root = new JsonObject();
      JsonObject ipBansObj = new JsonObject();

      for (Map.Entry<String, IpBan> entry : ipBans.entrySet()) {
        ipBansObj.add(entry.getKey(), serializeIpBan(entry.getValue()));
      }
      root.add("ipBans", ipBansObj);

      StorageUtils.WriteResult result = StorageUtils.writeAtomic(filePath, GSON.toJson(root));
      if (result instanceof StorageUtils.WriteResult.Failure f) {
        Logger.severe("[IpBanStorage] Failed to save: %s", f.error());
      }
    } catch (Exception e) {
      Logger.severe("[IpBanStorage] Failed to save: %s", e.getMessage());
    }
  }

  public void addIpBan(@NotNull IpBan ban) {
    ipBans.put(ban.ip(), ban);
    save();
  }

  public boolean removeIpBan(@NotNull String ip) {
    IpBan removed = ipBans.remove(ip);
    if (removed != null) {
      save();
      return true;
    }
    return false;
  }

  @Nullable
  public IpBan getIpBan(@NotNull String ip) {
    return ipBans.get(ip);
  }

  @NotNull
  public Map<String, IpBan> getAllIpBans() {
    return Collections.unmodifiableMap(ipBans);
  }

  private JsonObject serializeIpBan(@NotNull IpBan ban) {
    JsonObject obj = new JsonObject();
    obj.addProperty("reason", ban.reason());
    obj.addProperty("issuerUuid", ban.issuerUuid() != null ? ban.issuerUuid().toString() : null);
    obj.addProperty("issuerName", ban.issuerName());
    obj.addProperty("issuedAt", ban.issuedAt().toEpochMilli());
    obj.addProperty("expiresAt", ban.expiresAt() != null ? ban.expiresAt().toEpochMilli() : null);
    return obj;
  }

  @Nullable
  private IpBan deserializeIpBan(@NotNull String ip, @NotNull JsonObject obj) {
    try {
      return new IpBan(
        ip,
        obj.has("reason") && !obj.get("reason").isJsonNull() ? obj.get("reason").getAsString() : null,
        obj.has("issuerUuid") && !obj.get("issuerUuid").isJsonNull()
          ? UUID.fromString(obj.get("issuerUuid").getAsString()) : null,
        obj.get("issuerName").getAsString(),
        Instant.ofEpochMilli(obj.get("issuedAt").getAsLong()),
        obj.has("expiresAt") && !obj.get("expiresAt").isJsonNull()
          ? Instant.ofEpochMilli(obj.get("expiresAt").getAsLong()) : null
      );
    } catch (Exception e) {
      Logger.warn("[IpBanStorage] Failed to parse IP ban: %s", e.getMessage());
      return null;
    }
  }
}
