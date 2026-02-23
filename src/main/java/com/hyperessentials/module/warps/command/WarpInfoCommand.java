package com.hyperessentials.module.warps.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Warp;
import com.hyperessentials.module.warps.WarpManager;
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
            ctx.sendMessage(CommandUtil.error("You don't have permission to view warp info."));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

        if (parts.length < 2) {
            ctx.sendMessage(CommandUtil.error("Usage: /warpinfo <name>"));
            return;
        }

        String warpName = parts[1].toLowerCase();

        Warp warp = warpManager.getWarp(warpName);
        if (warp == null) {
            ctx.sendMessage(CommandUtil.error("Warp '" + warpName + "' not found."));
            return;
        }

        ctx.sendMessage(CommandUtil.msg("--- Warp: " + warp.displayName() + " ---", CommandUtil.COLOR_GOLD));
        ctx.sendMessage(CommandUtil.msg("Name: " + warp.name(), CommandUtil.COLOR_GRAY));
        ctx.sendMessage(CommandUtil.msg("Category: " + warp.category(), CommandUtil.COLOR_GRAY));
        ctx.sendMessage(CommandUtil.msg("World: " + warp.world(), CommandUtil.COLOR_GRAY));
        ctx.sendMessage(CommandUtil.msg(String.format("Location: %.1f, %.1f, %.1f",
            warp.x(), warp.y(), warp.z()), CommandUtil.COLOR_GRAY));

        if (warp.description() != null && !warp.description().isEmpty()) {
            ctx.sendMessage(CommandUtil.msg("Description: " + warp.description(), CommandUtil.COLOR_GRAY));
        }

        if (warp.permission() != null && !warp.permission().isEmpty()) {
            ctx.sendMessage(CommandUtil.msg("Permission: " + warp.permission(), CommandUtil.COLOR_GRAY));
            boolean hasAccess = warpManager.canAccess(uuid, warp);
            ctx.sendMessage(CommandUtil.msg("You have access: " + (hasAccess ? "Yes" : "No"), CommandUtil.COLOR_GRAY));
        }

        ctx.sendMessage(CommandUtil.msg("Created: " + TimeUtil.formatRelativeTime(warp.createdAt()), CommandUtil.COLOR_GRAY));
    }
}
