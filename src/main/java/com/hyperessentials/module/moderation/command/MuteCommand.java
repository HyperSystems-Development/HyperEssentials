package com.hyperessentials.module.moderation.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.DurationParser;
import com.hyperessentials.util.HEMessageUtil;
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.MUTE_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.MUTE_USAGE));
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.PLAYER_NOT_FOUND, targetName));
      return;
    }

    if (CommandUtil.hasPermission(targetUuid, Permissions.BYPASS_MUTE)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.MUTE_CANNOT_MUTE));
      return;
    }

    module.getModerationManager().mute(targetUuid, targetName, playerRef.getUuid(), playerRef.getUsername(), reason, durationMs);

    if (durationMs != null) {
      ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Moderation.MUTE_TEMP, targetName, DurationParser.formatHuman(durationMs)));
    } else {
      ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Moderation.MUTE_PERMANENT, targetName));
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
