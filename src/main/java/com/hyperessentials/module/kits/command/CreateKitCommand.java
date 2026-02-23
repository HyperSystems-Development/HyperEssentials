package com.hyperessentials.module.kits.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.module.kits.data.Kit;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /createkit <name> - Create a kit from current inventory.
 */
public class CreateKitCommand extends AbstractPlayerCommand {

    private final KitsModule module;

    public CreateKitCommand(@NotNull KitsModule module) {
        super("createkit", "Create a kit from your inventory");
        this.module = module;
        setAllowsExtraArguments(true);
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef playerRef,
                          @NotNull World world) {
        if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.KIT_CREATE)) {
            ctx.sendMessage(CommandUtil.error("You don't have permission to create kits."));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

        if (parts.length < 2) {
            ctx.sendMessage(CommandUtil.error("Usage: /createkit <name>"));
            return;
        }

        String kitName = parts[1].toLowerCase();

        // Check if name is valid
        if (!kitName.matches("[a-z0-9_-]+")) {
            ctx.sendMessage(CommandUtil.error("Kit name must contain only lowercase letters, numbers, hyphens, and underscores."));
            return;
        }

        // Check if already exists
        if (module.getKitManager().getKit(kitName) != null) {
            ctx.sendMessage(CommandUtil.error("Kit '" + kitName + "' already exists. Delete it first."));
            return;
        }

        Kit kit = module.getKitManager().captureFromInventory(playerRef, store, ref, kitName);
        ctx.sendMessage(CommandUtil.success("Kit '" + kit.displayName() + "' created with " + kit.items().size() + " item(s)."));
    }
}
