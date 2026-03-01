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
 * /delhome &lt;name&gt; - Delete a home.
 */
public class DelHomeCommand extends AbstractPlayerCommand {

  private final HomeManager homeManager;

  public DelHomeCommand(@NotNull HomeManager homeManager) {
    super("delhome", "Delete a home");
    this.homeManager = homeManager;
    addAliases("deletehome", "rmhome", "removehome");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef playerRef,
                          @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.HOME_DELETE)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to delete homes."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /delhome <name>"));
      Collection<Home> homes = homeManager.getHomes(uuid);
      if (!homes.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        for (Home h : homes) {
          if (!sb.isEmpty()) sb.append(", ");
          sb.append(h.name());
        }
        ctx.sendMessage(CommandUtil.info("Your homes: " + sb));
      }
      return;
    }

    String homeName = parts[1];

    if (homeManager.deleteHome(uuid, homeName)) {
      ctx.sendMessage(CommandUtil.success("Home '" + homeName + "' has been deleted."));
    } else {
      ctx.sendMessage(CommandUtil.error("Home '" + homeName + "' not found."));
    }
  }
}
