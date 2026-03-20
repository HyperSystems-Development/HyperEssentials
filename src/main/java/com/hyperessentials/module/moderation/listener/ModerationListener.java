package com.hyperessentials.module.moderation.listener;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.module.moderation.ModerationManager;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.module.moderation.VanishManager;
import com.hyperessentials.module.moderation.data.IpBan;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.util.DurationParser;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * Handles player connect and chat events for ban/mute enforcement, IP bans, and vanish.
 */
public class ModerationListener {

  private final ModerationModule module;

  public ModerationListener(@NotNull ModerationModule module) {
    this.module = module;
  }

  /**
   * Called on player connect. Checks ban and IP ban status, applies vanish hiding.
   */
  public void onPlayerConnect(@NotNull PlayerConnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    ModerationManager modManager = module.getModerationManager();

    // Check for active ban
    Punishment ban = modManager.getActiveBan(playerRef.getUuid());
    if (ban != null && ban.isEffective()) {
      StringBuilder message = new StringBuilder("You are banned from this server.");
      if (ban.reason() != null) {
        message.append("\nReason: ").append(ban.reason());
      }
      if (!ban.isPermanent()) {
        message.append("\nExpires in: ").append(DurationParser.formatHuman(ban.getRemainingMillis()));
      }

      try {
        playerRef.getPacketHandler().disconnect(message.toString());
      } catch (Exception e) {
        ErrorHandler.report("[Moderation] Failed to disconnect banned player", e);
      }
      return;
    }

    // Check for IP ban
    try {
      InetSocketAddress addr = (InetSocketAddress) playerRef.getPacketHandler()
          .getChannel().remoteAddress();
      if (addr != null) {
        String ip = addr.getAddress().getHostAddress();
        if (modManager.isIpBanned(ip)) {
          try {
            playerRef.getPacketHandler().disconnect("Your IP address has been banned.");
          } catch (Exception e) {
            ErrorHandler.report("[Moderation] Failed to disconnect IP-banned player", e);
          }
          return;
        }
      }
    } catch (Exception e) {
      ErrorHandler.report("[Moderation] Failed to check IP ban for " + playerRef.getUsername(), e);
    }

    // Hide vanished players from the new player
    VanishManager vanishManager = module.getVanishManager();
    vanishManager.onPlayerConnect(playerRef.getUuid(), playerRef);
  }

  /**
   * Called on player chat. Checks mute status.
   */
  public void onPlayerChat(@NotNull PlayerChatEvent event) {
    PlayerRef playerRef = event.getSender();

    // Check bypass
    if (CommandUtil.hasPermission(playerRef.getUuid(), Permissions.BYPASS_MUTE)) {
      return;
    }

    // Check mute
    ModerationManager modManager = module.getModerationManager();
    if (modManager.isMuted(playerRef.getUuid())) {
      event.setCancelled(true);
      String muteMsg = ConfigManager.get().moderation().getMutedChatMessage();
      playerRef.sendMessage(CommandUtil.error(muteMsg));
    }
  }
}
