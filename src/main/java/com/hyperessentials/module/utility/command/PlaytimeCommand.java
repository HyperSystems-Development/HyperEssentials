package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.utility.UtilityModule;
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

/**
 * /playtime (/pt) - Show your total playtime.
 */
public class PlaytimeCommand extends AbstractPlayerCommand {

  private final UtilityModule module;

  public PlaytimeCommand(@NotNull UtilityModule module) {
    super("playtime", "Show your playtime");
    addAliases("pt");
    this.module = module;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_PLAYTIME)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.PLAYTIME_NO_PERMISSION));
      return;
    }

    long totalMs = module.getUtilityManager().getTotalPlaytimeMs(playerRef.getUuid());
    ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Utility.PLAYTIME_RESULT, DurationParser.formatHuman(totalMs)));
  }
}
