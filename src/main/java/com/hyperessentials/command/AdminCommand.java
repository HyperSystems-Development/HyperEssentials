package com.hyperessentials.command;

import com.hyperessentials.BuildInfo;
import com.hyperessentials.Permissions;
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

/**
 * Main admin command for HyperEssentials (/hessentials).
 */
public class AdminCommand extends AbstractPlayerCommand {

    public AdminCommand() {
        super("hessentials", "HyperEssentials admin command");
        setAllowsExtraArguments(true);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef playerRef,
                          @NotNull World world) {

        String input = ctx.getInputString();
        String subcommand = "";
        if (input != null && !input.isEmpty()) {
            String[] parts = input.trim().split("\\s+");
            if (parts.length > 1) {
                subcommand = parts[1].toLowerCase();
            }
        }

        switch (subcommand) {
            case "reload" -> handleReload(ctx, playerRef);
            case "version", "ver" -> showVersion(ctx);
            default -> showHelp(ctx);
        }
    }

    private void handleReload(@NotNull CommandContext ctx, @NotNull PlayerRef playerRef) {
        if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.ADMIN_RELOAD)) {
            ctx.sendMessage(CommandUtil.error("You don't have permission to reload."));
            return;
        }

        ConfigManager.get().reloadAll();
        ctx.sendMessage(CommandUtil.success("Configuration reloaded."));
    }

    private void showVersion(@NotNull CommandContext ctx) {
        ctx.sendMessage(CommandUtil.info("HyperEssentials v" + BuildInfo.VERSION));
    }

    private void showHelp(@NotNull CommandContext ctx) {
        ctx.sendMessage(CommandUtil.info("HyperEssentials v" + BuildInfo.VERSION));
        ctx.sendMessage(CommandUtil.msg("/hessentials reload - Reload configuration", CommandUtil.COLOR_GRAY));
        ctx.sendMessage(CommandUtil.msg("/hessentials version - Show version", CommandUtil.COLOR_GRAY));
    }
}
