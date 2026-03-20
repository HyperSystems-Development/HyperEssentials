package com.hyperessentials.module.warps.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Warp;
import com.hyperessentials.module.warps.WarpManager;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
import com.hyperessentials.util.TimeUtil;
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
 * /warpinfo &lt;name&gt; - Display detailed information about a warp.
 */
public class WarpInfoCommand extends AbstractPlayerCommand {

  private final WarpManager warpManager;

  public WarpInfoCommand(@NotNull WarpManager warpManager) {
    super("warpinfo", "Display warp information");
    this.warpManager = warpManager;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.WARP_INFO)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Warp.INFO_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Warp.INFO_USAGE));
      return;
    }

    String warpName = parts[1].toLowerCase();

    Warp warp = warpManager.getWarp(warpName);
    if (warp == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Warp.NOT_FOUND, warpName));
      return;
    }

    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Warp.INFO_HEADER, HEMessageUtil.COLOR_GOLD, warp.displayName()));
    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Warp.INFO_NAME, HEMessageUtil.COLOR_GRAY, warp.name()));
    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Warp.INFO_CATEGORY, HEMessageUtil.COLOR_GRAY, warp.category()));
    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Warp.INFO_WORLD, HEMessageUtil.COLOR_GRAY, warp.world()));
    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Warp.INFO_LOCATION, HEMessageUtil.COLOR_GRAY,
        String.format("%.1f", warp.x()), String.format("%.1f", warp.y()), String.format("%.1f", warp.z())));

    if (warp.description() != null && !warp.description().isEmpty()) {
      ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Warp.INFO_DESCRIPTION, HEMessageUtil.COLOR_GRAY, warp.description()));
    }

    if (warp.permission() != null && !warp.permission().isEmpty()) {
      ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Warp.INFO_PERMISSION, HEMessageUtil.COLOR_GRAY, warp.permission()));
      boolean hasAccess = warpManager.canAccess(uuid, warp);
      ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Warp.INFO_ACCESS, HEMessageUtil.COLOR_GRAY, hasAccess ? "Yes" : "No"));
    }

    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Warp.INFO_CREATED, HEMessageUtil.COLOR_GRAY, TimeUtil.formatRelativeTime(warp.createdAt())));
  }
}
