package com.hyperessentials.module.moderation;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.data.PlayerData;
import com.hyperessentials.module.moderation.data.IpBan;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.module.moderation.data.PunishmentType;
import com.hyperessentials.module.moderation.storage.IpBanStorage;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.storage.PlayerDataStorage;
import com.hyperessentials.util.DurationParser;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ban, mute, and kick operations.
 * Punishments are stored in PlayerData (per-player files).
 * IP bans are stored separately in IpBanStorage.
 */
public class ModerationManager {

  private final IpBanStorage ipBanStorage;
  private final PlayerDataStorage playerDataStorage;
  @Nullable private TpaManager tpaManager;
  private final Map<UUID, String> playerIps = new ConcurrentHashMap<>();

  public ModerationManager(@NotNull IpBanStorage ipBanStorage,
               @NotNull PlayerDataStorage playerDataStorage) {
    this.ipBanStorage = ipBanStorage;
    this.playerDataStorage = playerDataStorage;
  }

  /**
   * Sets the TpaManager for accessing cached online player data.
   * When set, punishments for online players are read/written through
   * the same cached PlayerData objects to avoid cache divergence.
   */
  public void setTpaManager(@Nullable TpaManager tpaManager) {
    this.tpaManager = tpaManager;
  }

  /**
   * Gets PlayerData for a player.
   * Prefers TpaManager cache (online), falls back to direct storage load (offline).
   */
  @Nullable
  private PlayerData getPlayerData(@NotNull UUID uuid) {
    if (tpaManager != null) {
      PlayerData data = tpaManager.getPlayerData(uuid);
      if (data != null) return data;
    }
    // Offline: load directly from storage
    try {
      return playerDataStorage.loadPlayerData(uuid).join().orElse(null);
    } catch (Exception e) {
      ErrorHandler.report("[Moderation] Failed to load player data for " + uuid, e);
      return null;
    }
  }

  /**
   * Gets or creates PlayerData for a player (for adding punishments to new players).
   */
  @NotNull
  private PlayerData getOrCreatePlayerData(@NotNull UUID uuid, @NotNull String playerName) {
    PlayerData data = getPlayerData(uuid);
    if (data != null) return data;
    return new PlayerData(uuid, playerName);
  }

  /**
   * Saves player data, preferring TpaManager for online players.
   */
  private void savePlayerData(@NotNull UUID uuid, @NotNull PlayerData data) {
    if (tpaManager != null && tpaManager.getPlayerData(uuid) != null) {
      // Online player: save via TpaManager (same cached object)
      tpaManager.savePlayer(uuid);
    } else {
      // Offline player: save directly
      playerDataStorage.savePlayerData(data);
    }
  }

  // === IP Tracking ===

  /**
   * Captures a player's IP address on connect.
   */
  public void onPlayerConnect(@NotNull PlayerRef playerRef) {
    try {
      InetSocketAddress addr = (InetSocketAddress) playerRef.getPacketHandler()
          .getChannel().remoteAddress();
      if (addr != null) {
        String ip = addr.getAddress().getHostAddress();
        playerIps.put(playerRef.getUuid(), ip);
      }
    } catch (Exception e) {
      ErrorHandler.report("[Moderation] Failed to capture IP for " + playerRef.getUsername(), e);
    }
  }

  @Nullable
  public String getPlayerIp(@NotNull UUID uuid) {
    return playerIps.get(uuid);
  }

  /**
   * Checks if a player's IP is banned. Auto-removes expired bans.
   */
  public boolean isIpBanned(@NotNull String ip) {
    IpBan ban = ipBanStorage.getIpBan(ip);
    if (ban != null && ban.hasExpired()) {
      ipBanStorage.removeIpBan(ip);
      return false;
    }
    return ban != null && ban.isEffective();
  }

  @NotNull
  public IpBan ipBan(@NotNull String ip, @Nullable UUID issuerUuid, @NotNull String issuerName,
            @Nullable String reason, @Nullable Long durationMs) {
    return ipBan(ip, issuerUuid, issuerName, reason, durationMs, null, null);
  }

  /**
   * IP bans an address and optionally creates an IPBAN audit trail entry for a specific player.
   *
   * @param targetUuid the target player UUID (for audit trail), or null to skip audit
   * @param targetName the target player name (for audit trail), or null to skip audit
   */
  @NotNull
  public IpBan ipBan(@NotNull String ip, @Nullable UUID issuerUuid, @NotNull String issuerName,
            @Nullable String reason, @Nullable Long durationMs,
            @Nullable UUID targetUuid, @Nullable String targetName) {
    Instant expiresAt = durationMs != null ? Instant.now().plusMillis(durationMs) : null;
    IpBan ban = new IpBan(ip, reason, issuerUuid, issuerName, Instant.now(), expiresAt);
    ipBanStorage.addIpBan(ban);

    // Create IPBAN audit trail entry for the target player
    if (targetUuid != null && targetName != null) {
      PlayerData data = getOrCreatePlayerData(targetUuid, targetName);
      Punishment ipbanRecord = new Punishment(
        UUID.randomUUID(), PunishmentType.IPBAN, targetUuid, targetName,
        issuerUuid, issuerName, reason != null ? reason : ip,
        Instant.now(), expiresAt, true, null, null
      );
      data.addPunishment(ipbanRecord);
      savePlayerData(targetUuid, data);
    }

    notifyStaff(Permissions.NOTIFY_BAN, issuerName + " IP banned " + ip
      + (ban.isPermanent() ? " permanently" : " for " + DurationParser.formatHuman(durationMs)));

    return ban;
  }

  public boolean ipUnban(@NotNull String ip) {
    boolean removed = ipBanStorage.removeIpBan(ip);
    if (removed) {
      notifyStaff(Permissions.NOTIFY_BAN, "IP " + ip + " was unbanned");
    }
    return removed;
  }

  /**
   * Kicks all online players that share the given IP.
   */
  public void kickPlayersWithIp(@NotNull String ip, @NotNull String reason) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    for (Map.Entry<UUID, String> entry : playerIps.entrySet()) {
      if (ip.equals(entry.getValue())) {
        kickOnlinePlayer(entry.getKey(), reason);
      }
    }
  }

  // === Ban Operations ===

  @NotNull
  public Punishment ban(@NotNull UUID playerUuid, @NotNull String playerName,
              @Nullable UUID issuerUuid, @NotNull String issuerName,
              @Nullable String reason, @Nullable Long durationMs) {
    PlayerData data = getOrCreatePlayerData(playerUuid, playerName);

    // Revoke any existing active ban first
    Punishment existing = data.getActiveBan();
    if (existing != null) {
      data.revokePunishment(existing.id(), issuerUuid, issuerName);
    }

    String effectiveReason = reason != null ? reason
      : ConfigManager.get().moderation().getDefaultBanReason();

    Instant expiresAt = durationMs != null ? Instant.now().plusMillis(durationMs) : null;

    Punishment punishment = new Punishment(
      UUID.randomUUID(), PunishmentType.BAN, playerUuid, playerName,
      issuerUuid, issuerName, effectiveReason, Instant.now(),
      expiresAt, true, null, null
    );

    data.addPunishment(punishment);
    savePlayerData(playerUuid, data);

    // Kick the player if online
    kickOnlinePlayer(playerUuid, buildBanMessage(punishment));

    // Broadcast and notify
    if (ConfigManager.get().moderation().isBroadcastBans()) {
      String durationText = punishment.isPermanent() ? "permanently" : "for " + DurationParser.formatHuman(durationMs);
      broadcastToAll(CommandUtil.msg(playerName + " was banned " + durationText + ".", CommandUtil.COLOR_RED));
    }
    notifyStaff(Permissions.NOTIFY_BAN,
      issuerName + " banned " + playerName + (punishment.isPermanent() ? " permanently" : " for " + DurationParser.formatHuman(durationMs)));

    return punishment;
  }

  public boolean unban(@NotNull UUID playerUuid, @Nullable UUID revokerUuid, @NotNull String revokerName) {
    PlayerData data = getPlayerData(playerUuid);
    if (data == null) return false;

    Punishment ban = data.getActiveBan();
    if (ban == null) return false;

    data.revokePunishment(ban.id(), revokerUuid, revokerName);
    savePlayerData(playerUuid, data);

    notifyStaff(Permissions.NOTIFY_BAN, revokerName + " unbanned " + ban.playerName());
    return true;
  }

  public boolean isBanned(@NotNull UUID playerUuid) {
    PlayerData data = getPlayerData(playerUuid);
    if (data == null) return false;

    Punishment ban = data.getActiveBan();
    if (ban != null && ban.hasExpired()) {
      data.revokePunishment(ban.id(), null, "System");
      savePlayerData(playerUuid, data);
      return false;
    }
    return ban != null;
  }

  @Nullable
  public Punishment getActiveBan(@NotNull UUID playerUuid) {
    PlayerData data = getPlayerData(playerUuid);
    return data != null ? data.getActiveBan() : null;
  }

  // === Mute Operations ===

  @NotNull
  public Punishment mute(@NotNull UUID playerUuid, @NotNull String playerName,
               @Nullable UUID issuerUuid, @NotNull String issuerName,
               @Nullable String reason, @Nullable Long durationMs) {
    PlayerData data = getOrCreatePlayerData(playerUuid, playerName);

    Punishment existing = data.getActiveMute();
    if (existing != null) {
      data.revokePunishment(existing.id(), issuerUuid, issuerName);
    }

    String effectiveReason = reason != null ? reason
      : ConfigManager.get().moderation().getDefaultMuteReason();

    Instant expiresAt = durationMs != null ? Instant.now().plusMillis(durationMs) : null;

    Punishment punishment = new Punishment(
      UUID.randomUUID(), PunishmentType.MUTE, playerUuid, playerName,
      issuerUuid, issuerName, effectiveReason, Instant.now(),
      expiresAt, true, null, null
    );

    data.addPunishment(punishment);
    savePlayerData(playerUuid, data);

    // Notify the muted player if online
    PlayerRef target = findOnlinePlayer(playerUuid);
    if (target != null) {
      String durationText = punishment.isPermanent() ? "permanently" : "for " + DurationParser.formatHuman(durationMs);
      target.sendMessage(CommandUtil.error("You have been muted " + durationText + "."));
    }

    if (ConfigManager.get().moderation().isBroadcastMutes()) {
      String durationText = punishment.isPermanent() ? "permanently" : "for " + DurationParser.formatHuman(durationMs);
      broadcastToAll(CommandUtil.msg(playerName + " was muted " + durationText + ".", CommandUtil.COLOR_RED));
    }
    notifyStaff(Permissions.NOTIFY_MUTE,
      issuerName + " muted " + playerName + (punishment.isPermanent() ? " permanently" : " for " + DurationParser.formatHuman(durationMs)));

    return punishment;
  }

  public boolean unmute(@NotNull UUID playerUuid, @Nullable UUID revokerUuid, @NotNull String revokerName) {
    PlayerData data = getPlayerData(playerUuid);
    if (data == null) return false;

    Punishment mute = data.getActiveMute();
    if (mute == null) return false;

    data.revokePunishment(mute.id(), revokerUuid, revokerName);
    savePlayerData(playerUuid, data);

    PlayerRef target = findOnlinePlayer(playerUuid);
    if (target != null) {
      target.sendMessage(CommandUtil.success("You have been unmuted."));
    }

    notifyStaff(Permissions.NOTIFY_MUTE, revokerName + " unmuted " + mute.playerName());
    return true;
  }

  public boolean isMuted(@NotNull UUID playerUuid) {
    PlayerData data = getPlayerData(playerUuid);
    if (data == null) return false;

    Punishment mute = data.getActiveMute();
    if (mute != null && mute.hasExpired()) {
      data.revokePunishment(mute.id(), null, "System");
      savePlayerData(playerUuid, data);
      return false;
    }
    return mute != null;
  }

  // === Kick Operations ===

  @NotNull
  public Punishment kick(@NotNull UUID playerUuid, @NotNull String playerName,
               @Nullable UUID issuerUuid, @NotNull String issuerName,
               @Nullable String reason) {
    String effectiveReason = reason != null ? reason
      : ConfigManager.get().moderation().getDefaultKickReason();

    Punishment punishment = new Punishment(
      UUID.randomUUID(), PunishmentType.KICK, playerUuid, playerName,
      issuerUuid, issuerName, effectiveReason, Instant.now(),
      null, false, null, null
    );

    // Store kick in player's punishment history
    PlayerData data = getOrCreatePlayerData(playerUuid, playerName);
    data.addPunishment(punishment);
    savePlayerData(playerUuid, data);

    kickOnlinePlayer(playerUuid, effectiveReason);

    if (ConfigManager.get().moderation().isBroadcastKicks()) {
      broadcastToAll(CommandUtil.msg(playerName + " was kicked.", CommandUtil.COLOR_RED));
    }
    notifyStaff(Permissions.NOTIFY_KICK, issuerName + " kicked " + playerName);

    return punishment;
  }

  // === Warn Operations ===

  @NotNull
  public Punishment warn(@NotNull UUID playerUuid, @NotNull String playerName,
               @Nullable UUID issuerUuid, @NotNull String issuerName,
               @Nullable String reason) {
    String effectiveReason = reason != null ? reason
      : ConfigManager.get().moderation().getDefaultWarnReason();

    Punishment punishment = new Punishment(
      UUID.randomUUID(), PunishmentType.WARN, playerUuid, playerName,
      issuerUuid, issuerName, effectiveReason, Instant.now(),
      null, false, null, null
    );

    PlayerData data = getOrCreatePlayerData(playerUuid, playerName);
    data.addPunishment(punishment);
    savePlayerData(playerUuid, data);

    // Notify the warned player if online
    PlayerRef target = findOnlinePlayer(playerUuid);
    if (target != null) {
      target.sendMessage(CommandUtil.msg("You have been warned: " + effectiveReason, CommandUtil.COLOR_YELLOW));
    }

    // Auto-ban if threshold is set and reached
    int threshold = ConfigManager.get().moderation().getMaxWarningsBeforeBan();
    if (threshold > 0) {
      long warnCount = data.getPunishments().stream()
        .filter(p -> p.type() == PunishmentType.WARN)
        .count();
      if (warnCount >= threshold) {
        ban(playerUuid, playerName, issuerUuid, issuerName,
          "Automatic ban: reached " + threshold + " warnings", null);
      }
    }

    if (ConfigManager.get().moderation().isBroadcastWarnings()) {
      broadcastToAll(CommandUtil.msg(playerName + " was warned.", CommandUtil.COLOR_YELLOW));
    }
    notifyStaff(Permissions.NOTIFY_WARN, issuerName + " warned " + playerName);

    return punishment;
  }

  // === History ===

  @NotNull
  public List<Punishment> getHistory(@NotNull UUID playerUuid) {
    PlayerData data = getPlayerData(playerUuid);
    return data != null ? data.getPunishments() : List.of();
  }

  /**
   * Gets all punishments across all players, optionally filtered by active status.
   * Expensive operation — scans all player data files.
   */
  @NotNull
  public List<Punishment> getAllPunishments(boolean activeOnly) {
    List<Punishment> result = new ArrayList<>();
    try {
      List<PlayerData> allData = playerDataStorage.loadAllPlayerData().join();
      for (PlayerData data : allData) {
        for (Punishment p : data.getPunishments()) {
          if (!activeOnly || p.isEffective()) {
            result.add(p);
          }
        }
      }
    } catch (Exception e) {
      ErrorHandler.report("[Moderation] Failed to scan all punishments", e);
    }
    return result;
  }

  // === Offline Resolution ===

  @Nullable
  public UUID findPlayerUuid(@NotNull String name) {
    // Check online first
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      PlayerRef online = plugin.findOnlinePlayer(name);
      if (online != null) return online.getUuid();
    }
    // Scan player data files for matching username
    try {
      List<PlayerData> allData = playerDataStorage.loadAllPlayerData().join();
      for (PlayerData data : allData) {
        if (data.getUsername().equalsIgnoreCase(name)) {
          return data.getUuid();
        }
      }
    } catch (Exception e) {
      ErrorHandler.report("[Moderation] Failed to search for player " + name, e);
    }
    return null;
  }

  @Nullable
  public String getStoredPlayerName(@NotNull UUID uuid) {
    PlayerData data = getPlayerData(uuid);
    return data != null ? data.getUsername() : null;
  }

  public void shutdown() {
    ipBanStorage.save();
  }

  // === Helpers ===

  private void kickOnlinePlayer(@NotNull UUID playerUuid, @NotNull String reason) {
    PlayerRef player = findOnlinePlayer(playerUuid);
    if (player != null) {
      try {
        player.getPacketHandler().disconnect(reason);
      } catch (Exception e) {
        ErrorHandler.report("[Moderation] Failed to kick player", e);
      }
    }
  }

  @Nullable
  private PlayerRef findOnlinePlayer(@NotNull UUID playerUuid) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    return plugin != null ? plugin.getTrackedPlayer(playerUuid) : null;
  }

  private void broadcastToAll(@NotNull Message message) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    for (PlayerRef player : plugin.getTrackedPlayers().values()) {
      player.sendMessage(message);
    }
  }

  private void notifyStaff(@NotNull String permission, @NotNull String message) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    Message msg = CommandUtil.msg("[Staff] " + message, CommandUtil.COLOR_AQUA);
    for (Map.Entry<UUID, PlayerRef> entry : plugin.getTrackedPlayers().entrySet()) {
      if (CommandUtil.hasPermission(entry.getKey(), permission)) {
        entry.getValue().sendMessage(msg);
      }
    }
  }

  @NotNull
  private String buildBanMessage(@NotNull Punishment ban) {
    StringBuilder sb = new StringBuilder("You are banned from this server.");
    if (ban.reason() != null) {
      sb.append("\nReason: ").append(ban.reason());
    }
    if (!ban.isPermanent()) {
      sb.append("\nExpires in: ").append(DurationParser.formatHuman(ban.getRemainingMillis()));
    }
    return sb.toString();
  }
}
