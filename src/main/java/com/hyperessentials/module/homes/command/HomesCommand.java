package com.hyperessentials.module.homes.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Home;
import com.hyperessentials.module.homes.HomeManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

/**
 * /homes - List all homes with count and limit.
 */
public class HomesCommand extends AbstractPlayerCommand {

  private final HomeManager homeManager;

  public HomesCommand(@NotNull HomeManager homeManager) {
    super("homes", "List your homes");
    this.homeManager = homeManager;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef playerRef,
                          @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.HOME_LIST)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to list homes."));
      return;
    }

    Collection<Home> homes = homeManager.getHomes(uuid);
    int count = homes.size();
    int limit = homeManager.getHomeLimit(uuid);

    String limitStr = limit < 0 ? "unlimited" : String.valueOf(limit);

    if (homes.isEmpty()) {
      ctx.sendMessage(CommandUtil.info(
          "You don't have any homes (0/" + limitStr + "). Use /sethome to create one."));
      return;
    }

    ctx.sendMessage(CommandUtil.info("Your homes (" + count + "/" + limitStr + "):"));

    StringBuilder sb = new StringBuilder();
    for (Home home : homes) {
      if (!sb.isEmpty()) sb.append(", ");
      sb.append(home.name());
    }
    ctx.sendMessage(CommandUtil.msg(sb.toString(), CommandUtil.COLOR_GRAY));
  }
}
