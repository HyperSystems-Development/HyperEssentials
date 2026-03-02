package com.hyperessentials.module.moderation;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.module.moderation.data.IpBan;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.module.moderation.data.PunishmentType;
import com.hyperessentials.module.moderation.storage.ModerationStorage;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.DurationParser;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages ban, mute, and kick operations.
 */
public class ModerationManager {

  private final ModerationStorage storage;
  private final Map<UUID, String> playerIps = new ConcurrentHashMap<>();

  public ModerationManager(@NotNull ModerationStorage storage) {
    this.storage = storage;
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
      Logger.debug("[Moderation] Failed to capture IP for %s", playerRef.getUsername());
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
    IpBan ban = storage.getIpBan(ip);
    if (ban != null && ban.hasExpired()) {
      storage.removeIpBan(ip);
      return false;
    }
    return ban != null && ban.isEffective();
  }

  @NotNull
  public IpBan ipBan(@NotNull String ip, @Nullable UUID issuerUuid, @NotNull String issuerName,
            @Nullable String reason, @Nullable Long durationMs) {
    Instant expiresAt = durationMs != null ? Instant.now().plusMillis(durationMs) : null;
    IpBan ban = new IpBan(ip, reason, issuerUuid, issuerName, Instant.now(), expiresAt);
    storage.addIpBan(ban);

    notifyStaff(Permissions.NOTIFY_BAN, issuerName + " IP banned " + ip
      + (ban.isPermanent() ? " permanently" : " for " + DurationParser.formatHuman(durationMs)));

    return ban;
  }

  public boolean ipUnban(@NotNull String ip) {
    boolean removed = storage.removeIpBan(ip);
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
    // Revoke any existing active ban first
    Punishment existing = storage.getActiveBan(playerUuid);
    if (existing != null) {
      storage.updatePunishment(existing.revoke(issuerUuid, issuerName));
    }

    String effectiveReason = reason != null ? reason
      : ConfigManager.get().moderation().getDefaultBanReason();

    Instant expiresAt = durationMs != null ? Instant.now().plusMillis(durationMs) : null;

    Punishment punishment = new Punishment(
      UUID.randomUUID(), PunishmentType.BAN, playerUuid, playerName,
      issuerUuid, issuerName, effectiveReason, Instant.now(),
      expiresAt, true, null, null
    );

    storage.addPunishment(punishment);

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
    Punishment ban = storage.getActiveBan(playerUuid);
    if (ban == null) return false;

    storage.updatePunishment(ban.revoke(revokerUuid, revokerName));

    notifyStaff(Permissions.NOTIFY_BAN, revokerName + " unbanned " + ban.playerName());
    return true;
  }

  public boolean isBanned(@NotNull UUID playerUuid) {
    Punishment ban = storage.getActiveBan(playerUuid);
    if (ban != null && ban.hasExpired()) {
      storage.updatePunishment(ban.revoke(null, "System"));
      return false;
    }
    return ban != null;
  }

  @Nullable
  public Punishment getActiveBan(@NotNull UUID playerUuid) {
    return storage.getActiveBan(playerUuid);
  }

  // === Mute Operations ===

  @NotNull
  public Punishment mute(@NotNull UUID playerUuid, @NotNull String playerName,
               @Nullable UUID issuerUuid, @NotNull String issuerName,
               @Nullable String reason, @Nullable Long durationMs) {
    Punishment existing = storage.getActiveMute(playerUuid);
    if (existing != null) {
      storage.updatePunishment(existing.revoke(issuerUuid, issuerName));
    }

    String effectiveReason = reason != null ? reason
      : ConfigManager.get().moderation().getDefaultMuteReason();

    Instant expiresAt = durationMs != null ? Instant.now().plusMillis(durationMs) : null;

    Punishment punishment = new Punishment(
      UUID.randomUUID(), PunishmentType.MUTE, playerUuid, playerName,
      issuerUuid, issuerName, effectiveReason, Instant.now(),
      expiresAt, true, null, null
    );

    storage.addPunishment(punishment);

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
    Punishment mute = storage.getActiveMute(playerUuid);
    if (mute == null) return false;

    storage.updatePunishment(mute.revoke(revokerUuid, revokerName));

    PlayerRef target = findOnlinePlayer(playerUuid);
    if (target != null) {
      target.sendMessage(CommandUtil.success("You have been unmuted."));
    }

    notifyStaff(Permissions.NOTIFY_MUTE, revokerName + " unmuted " + mute.playerName());
    return true;
  }

  public boolean isMuted(@NotNull UUID playerUuid) {
    Punishment mute = storage.getActiveMute(playerUuid);
    if (mute != null && mute.hasExpired()) {
      storage.updatePunishment(mute.revoke(null, "System"));
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

    storage.addPunishment(punishment);

    kickOnlinePlayer(playerUuid, effectiveReason);

    if (ConfigManager.get().moderation().isBroadcastKicks()) {
      broadcastToAll(CommandUtil.msg(playerName + " was kicked.", CommandUtil.COLOR_RED));
    }
    notifyStaff(Permissions.NOTIFY_KICK, issuerName + " kicked " + playerName);

    return punishment;
  }

  // === History ===

  @NotNull
  public List<Punishment> getHistory(@NotNull UUID playerUuid) {
    return storage.getPunishments(playerUuid);
  }

  /**
   * Gets all punishments across all players, optionally filtered by active status.
   */
  @NotNull
  public List<Punishment> getAllPunishments(boolean activeOnly) {
    return storage.getAllPunishments(activeOnly);
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
    // Check stored records
    return storage.findPlayerUuid(name);
  }

  @Nullable
  public String getStoredPlayerName(@NotNull UUID uuid) {
    List<Punishment> list = storage.getPunishments(uuid);
    return list.isEmpty() ? null : list.getFirst().playerName();
  }

  public void shutdown() {
    storage.save();
  }

  // === Helpers ===

  private void kickOnlinePlayer(@NotNull UUID playerUuid, @NotNull String reason) {
    PlayerRef player = findOnlinePlayer(playerUuid);
    if (player != null) {
      try {
        player.getPacketHandler().disconnect(reason);
      } catch (Exception e) {
        Logger.warn("[Moderation] Failed to kick player: %s", e.getMessage());
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
