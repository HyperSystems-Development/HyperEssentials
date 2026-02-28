package com.hyperessentials.module.moderation.storage;

import com.google.gson.*;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.module.moderation.data.PunishmentType;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON persistence for punishment records.
 * File: data/punishments.json
 */
public class ModerationStorage {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  private final Path filePath;
  private final Map<UUID, List<Punishment>> punishments = new ConcurrentHashMap<>();

  public ModerationStorage(@NotNull Path dataDir) {
    this.filePath = dataDir.resolve("data").resolve("punishments.json");
  }

  public void load() {
    punishments.clear();

    if (!Files.exists(filePath)) {
      Logger.info("[ModerationStorage] No punishments file found, starting fresh");
      return;
    }

    try {
      String json = Files.readString(filePath);
      JsonObject root = JsonParser.parseString(json).getAsJsonObject();

      if (root.has("players") && root.get("players").isJsonObject()) {
        JsonObject players = root.getAsJsonObject("players");
        for (Map.Entry<String, JsonElement> entry : players.entrySet()) {
          UUID playerUuid = UUID.fromString(entry.getKey());
          JsonObject playerObj = entry.getValue().getAsJsonObject();

          if (playerObj.has("punishments") && playerObj.get("punishments").isJsonArray()) {
            List<Punishment> list = new ArrayList<>();
            for (JsonElement el : playerObj.getAsJsonArray("punishments")) {
              Punishment p = deserialize(el.getAsJsonObject());
              if (p != null) list.add(p);
            }
            if (!list.isEmpty()) {
              punishments.put(playerUuid, Collections.synchronizedList(list));
            }
          }
        }
      }

      Logger.info("[ModerationStorage] Loaded punishments for %d player(s)", punishments.size());
    } catch (Exception e) {
      Logger.severe("[ModerationStorage] Failed to load punishments: %s", e.getMessage());
    }
  }

  public void save() {
    try {
      Files.createDirectories(filePath.getParent());

      JsonObject root = new JsonObject();
      JsonObject players = new JsonObject();

      for (Map.Entry<UUID, List<Punishment>> entry : punishments.entrySet()) {
        JsonObject playerObj = new JsonObject();
        List<Punishment> list = entry.getValue();

        if (!list.isEmpty()) {
          playerObj.addProperty("playerName", list.getFirst().playerName());
        }

        JsonArray arr = new JsonArray();
        synchronized (list) {
          for (Punishment p : list) {
            arr.add(serialize(p));
          }
        }
        playerObj.add("punishments", arr);

        players.add(entry.getKey().toString(), playerObj);
      }

      root.add("players", players);
      Files.writeString(filePath, GSON.toJson(root));
      Logger.debug("[ModerationStorage] Saved punishments");
    } catch (IOException e) {
      Logger.severe("[ModerationStorage] Failed to save punishments: %s", e.getMessage());
    }
  }

  public void addPunishment(@NotNull Punishment punishment) {
    punishments.computeIfAbsent(punishment.playerUuid(),
      k -> Collections.synchronizedList(new ArrayList<>())).add(punishment);
    save();
  }

  /**
   * Updates a punishment in-place (e.g., revoking).
   */
  public void updatePunishment(@NotNull Punishment updated) {
    List<Punishment> list = punishments.get(updated.playerUuid());
    if (list == null) return;

    synchronized (list) {
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).id().equals(updated.id())) {
          list.set(i, updated);
          break;
        }
      }
    }
    save();
  }

  /**
   * Gets all punishments for a player.
   */
  @NotNull
  public List<Punishment> getPunishments(@NotNull UUID playerUuid) {
    List<Punishment> list = punishments.get(playerUuid);
    if (list == null) return List.of();
    synchronized (list) {
      return new ArrayList<>(list);
    }
  }

  /**
   * Gets the active ban for a player, if any.
   */
  @Nullable
  public Punishment getActiveBan(@NotNull UUID playerUuid) {
    return getActivePunishment(playerUuid, PunishmentType.BAN);
  }

  /**
   * Gets the active mute for a player, if any.
   */
  @Nullable
  public Punishment getActiveMute(@NotNull UUID playerUuid) {
    return getActivePunishment(playerUuid, PunishmentType.MUTE);
  }

  @Nullable
  private Punishment getActivePunishment(@NotNull UUID playerUuid, @NotNull PunishmentType type) {
    List<Punishment> list = punishments.get(playerUuid);
    if (list == null) return null;

    synchronized (list) {
      for (Punishment p : list) {
        if (p.type() == type && p.isEffective()) {
          return p;
        }
      }
    }
    return null;
  }

  /**
   * Finds a player UUID by name from stored punishment records.
   */
  @Nullable
  public UUID findPlayerUuid(@NotNull String name) {
    for (Map.Entry<UUID, List<Punishment>> entry : punishments.entrySet()) {
      List<Punishment> list = entry.getValue();
      synchronized (list) {
        for (Punishment p : list) {
          if (p.playerName().equalsIgnoreCase(name)) {
            return entry.getKey();
          }
        }
      }
    }
    return null;
  }

  private JsonObject serialize(@NotNull Punishment p) {
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
  private Punishment deserialize(@NotNull JsonObject obj) {
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
      Logger.warn("[ModerationStorage] Failed to parse punishment: %s", e.getMessage());
      return null;
    }
  }
}
