package com.hyperessentials.module.announcements.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.announcements.AnnouncementsModule;
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

/**
 * /broadcast <message> - Send an immediate broadcast to all players.
 */
public class BroadcastCommand extends AbstractPlayerCommand {

  private final AnnouncementsModule module;

  public BroadcastCommand(@NotNull AnnouncementsModule module) {
    super("broadcast", "Broadcast a message to all players");
    this.module = module;
    addAliases("bc");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.ANNOUNCE_BROADCAST)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Announce.BC_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    if (input == null || input.isBlank()) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Announce.BC_USAGE));
      return;
    }

    // Remove command name from input
    String message = input.trim();
    int spaceIdx = message.indexOf(' ');
    if (spaceIdx < 0) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Announce.BC_USAGE));
      return;
    }
    message = message.substring(spaceIdx + 1).trim();

    if (message.isEmpty()) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Announce.BC_USAGE));
      return;
    }

    module.getScheduler().broadcastNow(message);
    ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Announce.BC_SENT));
  }
}
