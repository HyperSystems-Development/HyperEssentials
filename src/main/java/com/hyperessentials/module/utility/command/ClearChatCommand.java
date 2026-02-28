package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /clearchat [player] - Clear chat for self or all players.
 */
public class ClearChatCommand extends AbstractPlayerCommand {

  public ClearChatCommand() {
    super("clearchat", "Clear chat");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_CLEARCHAT)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to clear chat."));
      return;
    }

    int lines = ConfigManager.get().utility().getClearChatLines();
    Message blank = Message.raw(" ");

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length >= 2) {
      // Clear for specific player or "all"
      if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_CLEARCHAT_OTHERS)) {
        ctx.sendMessage(CommandUtil.error("You don't have permission to clear others' chat."));
        return;
      }

      // Clear global chat
      HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
      if (plugin != null) {
        for (PlayerRef player : plugin.getTrackedPlayers().values()) {
          for (int i = 0; i < lines; i++) {
            player.sendMessage(blank);
          }
        }
      }
      ctx.sendMessage(CommandUtil.success("Chat cleared for all players."));
    } else {
      // Clear own chat
      for (int i = 0; i < lines; i++) {
        playerRef.sendMessage(blank);
      }
      ctx.sendMessage(CommandUtil.success("Chat cleared."));
    }
  }
}
