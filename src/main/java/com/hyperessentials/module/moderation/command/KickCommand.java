package com.hyperessentials.module.moderation.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /kick <player> [reason...] - Kick a player from the server.
 */
public class KickCommand extends AbstractPlayerCommand {

  private final ModerationModule module;

  public KickCommand(@NotNull ModerationModule module) {
    super("kick", "Kick a player");
    this.module = module;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.MODERATION_KICK)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to kick players."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /kick <player> [reason...]"));
      return;
    }

    String targetName = parts[1];
    String reason = parts.length > 2 ? joinArgs(parts, 2) : null;

    PlayerRef target = CommandUtil.findOnlinePlayer(targetName);
    if (target == null) {
      ctx.sendMessage(CommandUtil.error("Player '" + targetName + "' is not online."));
      return;
    }

    module.getModerationManager().kick(
      target.getUuid(), target.getUsername(),
      playerRef.getUuid(), playerRef.getUsername(), reason
    );
    ctx.sendMessage(CommandUtil.success("Kicked " + target.getUsername() + "."));
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
