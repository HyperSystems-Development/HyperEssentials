package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.utility.UtilityModule;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
import com.hyperessentials.util.HEMessages;
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.AFK_NO_PERMISSION));
      return;
    }

    boolean nowAfk = module.getUtilityManager().toggleAfk(playerRef.getUuid());

    // Broadcast to all players
    String key = nowAfk ? CommandKeys.Utility.AFK_NOW : CommandKeys.Utility.AFK_NO_LONGER;

    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      for (PlayerRef p : plugin.getTrackedPlayers().values()) {
        String text = HEMessages.get(p, key, playerRef.getUsername());
        p.sendMessage(Message.raw(text).color(HEMessageUtil.COLOR_GRAY));
      }
    }
  }
}
