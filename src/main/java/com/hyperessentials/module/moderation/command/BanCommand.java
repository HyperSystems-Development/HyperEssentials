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

import java.util.UUID;

/**
 * /ban <player> [reason...] - Permanently ban a player.
 */
public class BanCommand extends AbstractPlayerCommand {

  private final ModerationModule module;

  public BanCommand(@NotNull ModerationModule module) {
    super("ban", "Permanently ban a player");
    this.module = module;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.MODERATION_BAN)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to ban players."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /ban <player> [reason...]"));
      return;
    }

    String targetName = parts[1];
    String reason = parts.length > 2 ? joinArgs(parts, 2) : null;

    // Resolve target
    UUID targetUuid = module.getModerationManager().findPlayerUuid(targetName);
    if (targetUuid == null) {
      ctx.sendMessage(CommandUtil.error("Player '" + targetName + "' not found."));
      return;
    }

    // Check bypass
    if (CommandUtil.hasPermission(targetUuid, Permissions.BYPASS_BAN)) {
      ctx.sendMessage(CommandUtil.error("That player cannot be banned."));
      return;
    }

    module.getModerationManager().ban(targetUuid, targetName, playerRef.getUuid(), playerRef.getUsername(), reason, null);
    ctx.sendMessage(CommandUtil.success("Permanently banned " + targetName + "."));
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
