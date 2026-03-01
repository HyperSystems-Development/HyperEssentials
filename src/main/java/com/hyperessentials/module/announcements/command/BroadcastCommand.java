package com.hyperessentials.module.announcements.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.announcements.AnnouncementsModule;
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
      ctx.sendMessage(CommandUtil.error("You don't have permission to broadcast."));
      return;
    }

    String input = ctx.getInputString();
    if (input == null || input.isBlank()) {
      ctx.sendMessage(CommandUtil.error("Usage: /broadcast <message>"));
      return;
    }

    // Remove command name from input
    String message = input.trim();
    int spaceIdx = message.indexOf(' ');
    if (spaceIdx < 0) {
      ctx.sendMessage(CommandUtil.error("Usage: /broadcast <message>"));
      return;
    }
    message = message.substring(spaceIdx + 1).trim();

    if (message.isEmpty()) {
      ctx.sendMessage(CommandUtil.error("Usage: /broadcast <message>"));
      return;
    }

    module.getScheduler().broadcastNow(message);
    ctx.sendMessage(CommandUtil.success("Broadcast sent."));
  }
}
