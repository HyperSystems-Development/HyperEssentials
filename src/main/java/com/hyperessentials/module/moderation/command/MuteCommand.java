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

import java.util.UUID;

/**
 * /mute <player> [duration] [reason...] - Mute a player (permanent or temporary).
 * If the second argument parses as a duration, it's a temp mute; otherwise treat as reason.
 * Aliases: /tempmute, /tmute
 */
public class MuteCommand extends AbstractPlayerCommand {

  private final ModerationModule module;

  public MuteCommand(@NotNull ModerationModule module) {
    super("mute", "Mute a player");
    addAliases("tempmute", "tmute");
    this.module = module;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.MODERATION_MUTE)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to mute players."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /mute <player> [duration] [reason...]"));
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

    UUID targetUuid = module.getModerationManager().findPlayerUuid(targetName);
    if (targetUuid == null) {
      ctx.sendMessage(CommandUtil.error("Player '" + targetName + "' not found."));
      return;
    }

    if (CommandUtil.hasPermission(targetUuid, Permissions.BYPASS_MUTE)) {
      ctx.sendMessage(CommandUtil.error("That player cannot be muted."));
      return;
    }

    module.getModerationManager().mute(targetUuid, targetName, playerRef.getUuid(), playerRef.getUsername(), reason, durationMs);

    if (durationMs != null) {
      ctx.sendMessage(CommandUtil.success("Muted " + targetName + " for " + DurationParser.formatHuman(durationMs) + "."));
    } else {
      ctx.sendMessage(CommandUtil.success("Permanently muted " + targetName + "."));
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
