package com.hyperessentials.module.warps.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.warps.WarpManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * /delwarp &lt;name&gt; - Delete a server warp.
 */
public class DelWarpCommand extends AbstractPlayerCommand {

  private final WarpManager warpManager;

  public DelWarpCommand(@NotNull WarpManager warpManager) {
    super("delwarp", "Delete a server warp");
    this.warpManager = warpManager;
    addAliases("deletewarp", "rmwarp", "removewarp");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.WARP_DELETE)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to delete warps."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /delwarp <name>"));
      return;
    }

    String warpName = parts[1].toLowerCase();

    if (warpManager.deleteWarp(warpName)) {
      ctx.sendMessage(CommandUtil.success("Warp '" + warpName + "' has been deleted."));
    } else {
      ctx.sendMessage(CommandUtil.error("Warp '" + warpName + "' not found."));
    }
  }
}
