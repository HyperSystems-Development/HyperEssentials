package com.hyperessentials.module.moderation.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.util.CommandKeys;
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
 * /unmute <player> - Revoke a player's mute.
 */
public class UnmuteCommand extends AbstractPlayerCommand {

  private final ModerationModule module;

  public UnmuteCommand(@NotNull ModerationModule module) {
    super("unmute", "Unmute a player");
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.UNMUTE_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.UNMUTE_USAGE));
      return;
    }

    String targetName = parts[1];
    UUID targetUuid = module.getModerationManager().findPlayerUuid(targetName);
    if (targetUuid == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.PLAYER_NOT_FOUND, targetName));
      return;
    }

    if (module.getModerationManager().unmute(targetUuid, playerRef.getUuid(), playerRef.getUsername())) {
      ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Moderation.UNMUTE_SUCCESS, targetName));
    } else {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.UNMUTE_NOT_MUTED, targetName));
    }
  }
}
