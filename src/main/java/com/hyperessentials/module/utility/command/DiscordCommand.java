package com.hyperessentials.module.utility.command;

import com.hyperessentials.config.ConfigManager;
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
 * /discord - Show Discord invite link.
 */
public class DiscordCommand extends AbstractPlayerCommand {

  public DiscordCommand() {
    super("discord", "Show Discord invite link");
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    String url = ConfigManager.get().utility().getDiscordUrl();
    if (url == null || url.isEmpty()) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.DISCORD_NO_LINK));
    } else {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Utility.DISCORD_MESSAGE, HEMessageUtil.COLOR_CYAN, url));
    }
  }
}
