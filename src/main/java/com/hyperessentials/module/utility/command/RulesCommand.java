package com.hyperessentials.module.utility.command;

import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
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
 * /rules - Show server rules.
 */
public class RulesCommand extends AbstractPlayerCommand {

  public RulesCommand() {
    super("rules", "Show server rules");
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    List<String> lines = ConfigManager.get().utility().getRuleLines();
    ctx.sendMessage(CommandUtil.msg("=== Server Rules ===", CommandUtil.COLOR_GOLD));
    for (String line : lines) {
      ctx.sendMessage(CommandUtil.msg(line, CommandUtil.COLOR_GRAY));
    }
  }
}
