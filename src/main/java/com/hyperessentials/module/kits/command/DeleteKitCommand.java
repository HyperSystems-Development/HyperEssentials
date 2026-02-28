package com.hyperessentials.module.kits.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.kits.KitsModule;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /deletekit <name> - Delete a kit.
 */
public class DeleteKitCommand extends AbstractPlayerCommand {

    private final KitsModule module;

    public DeleteKitCommand(@NotNull KitsModule module) {
        super("deletekit", "Delete a kit");
        this.module = module;
        setAllowsExtraArguments(true);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef playerRef,
                          @NotNull World world) {
        if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.KIT_DELETE)) {
            ctx.sendMessage(CommandUtil.error("You don't have permission to delete kits."));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

        if (parts.length < 2) {
            ctx.sendMessage(CommandUtil.error("Usage: /deletekit <name>"));
            return;
        }

        String kitName = parts[1].toLowerCase();

        if (module.getKitManager().deleteKit(kitName)) {
            ctx.sendMessage(CommandUtil.success("Kit '" + kitName + "' deleted."));
        } else {
            ctx.sendMessage(CommandUtil.error("Kit '" + kitName + "' not found."));
        }
    }
}
