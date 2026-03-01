package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.utility.UtilityModule;
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
 * /afk (/away) - Toggle AFK status.
 */
public class AfkCommand extends AbstractPlayerCommand {

  private final UtilityModule module;

  public AfkCommand(@NotNull UtilityModule module) {
    super("afk", "Toggle AFK status");
    addAliases("away");
    this.module = module;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_AFK)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to use AFK."));
      return;
    }

    boolean nowAfk = module.getUtilityManager().toggleAfk(playerRef.getUuid());

    // Broadcast to all players
    String text = playerRef.getUsername() + (nowAfk ? " is now AFK" : " is no longer AFK");
    Message msg = CommandUtil.msg(text, CommandUtil.COLOR_GRAY);

    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      for (PlayerRef p : plugin.getTrackedPlayers().values()) {
        p.sendMessage(msg);
      }
    }
  }
}
