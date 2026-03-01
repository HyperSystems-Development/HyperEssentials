package com.hyperessentials.module.moderation.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.util.DurationParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /ipban <player> [duration] [reason...] - Ban a player's IP address.
 */
public class IpBanCommand extends AbstractPlayerCommand {

  private final ModerationModule module;

  public IpBanCommand(@NotNull ModerationModule module) {
    super("ipban", "Ban a player's IP address");
    this.module = module;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.MODERATION_IPBAN)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to IP ban players."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /ipban <player> [duration] [reason...]"));
      return;
    }

    String targetName = parts[1];
    Long durationMs = null;
    String reason = null;

    if (parts.length > 2) {
      long parsed = DurationParser.parse(parts[2]);
      if (parsed > 0) {
        durationMs = parsed;
        reason = parts.length > 3 ? joinArgs(parts, 3) : null;
      } else {
        reason = joinArgs(parts, 2);
      }
    }

    // Target must be online to get their IP
    PlayerRef target = CommandUtil.findOnlinePlayer(targetName);
    if (target == null) {
      ctx.sendMessage(CommandUtil.error("Player '" + targetName + "' must be online to IP ban."));
      return;
    }

    String ip = module.getModerationManager().getPlayerIp(target.getUuid());
    if (ip == null) {
      ctx.sendMessage(CommandUtil.error("Could not resolve IP for " + targetName + "."));
      return;
    }

    module.getModerationManager().ipBan(ip, playerRef.getUuid(), playerRef.getUsername(), reason, durationMs);

    // Kick the target and anyone else on the same IP
    module.getModerationManager().kickPlayersWithIp(ip, "Your IP has been banned.");

    if (durationMs != null) {
      ctx.sendMessage(CommandUtil.success("IP banned " + targetName + " (" + ip + ") for " + DurationParser.formatHuman(durationMs) + "."));
    } else {
      ctx.sendMessage(CommandUtil.success("Permanently IP banned " + targetName + " (" + ip + ")."));
    }
  }

  private String joinArgs(String[] parts, int start) {
    StringBuilder sb = new StringBuilder();
    for (int i = start; i < parts.length; i++) {
      if (i > start) sb.append(' ');
      sb.append(parts[i]);
    }
    return sb.toString();
  }
}
