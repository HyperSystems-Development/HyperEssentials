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
 * /ban <player> [duration] [reason...] - Ban a player (permanent or temporary).
 * If the second argument parses as a duration, it's a temp ban; otherwise treat as reason.
 * Aliases: /tempban
 */
public class BanCommand extends AbstractPlayerCommand {

  private final ModerationModule module;

  public BanCommand(@NotNull ModerationModule module) {
    super("ban", "Ban a player");
    addAliases("tempban");
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.BAN_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.BAN_USAGE));
      return;
    }

    String targetName = parts[1];
    Long durationMs = null;
    String reason = null;

    if (parts.length > 2) {
      // Try to parse second arg as duration
      long parsed = DurationParser.parse(parts[2]);
      if (parsed > 0) {
        durationMs = parsed;
        reason = parts.length > 3 ? joinArgs(parts, 3) : null;
      } else {
        // Not a duration, treat as start of reason
        reason = joinArgs(parts, 2);
      }
    }

    // Resolve target
    UUID targetUuid = module.getModerationManager().findPlayerUuid(targetName);
    if (targetUuid == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.PLAYER_NOT_FOUND, targetName));
      return;
    }

    // Check bypass
    if (CommandUtil.hasPermission(targetUuid, Permissions.BYPASS_BAN)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.BAN_CANNOT_BAN));
      return;
    }

    module.getModerationManager().ban(targetUuid, targetName, playerRef.getUuid(), playerRef.getUsername(), reason, durationMs);

    if (durationMs != null) {
      ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Moderation.BAN_TEMP, targetName, DurationParser.formatHuman(durationMs)));
    } else {
      ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Moderation.BAN_PERMANENT, targetName));
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
