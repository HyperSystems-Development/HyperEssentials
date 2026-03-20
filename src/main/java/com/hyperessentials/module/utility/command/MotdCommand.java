package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
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

import java.util.List;

/**
 * /motd - Show the message of the day.
 */
public class MotdCommand extends AbstractPlayerCommand {

  public MotdCommand() {
    super("motd", "Show the message of the day");
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_MOTD)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.MOTD_NO_PERMISSION));
      return;
    }

    List<String> lines = ConfigManager.get().utility().getMotdLines();
    ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Utility.MOTD_HEADER, HEMessageUtil.COLOR_GOLD));
    for (String line : lines) {
      ctx.sendMessage(HEMessageUtil.text(line, HEMessageUtil.COLOR_WHITE));
    }
  }
}
