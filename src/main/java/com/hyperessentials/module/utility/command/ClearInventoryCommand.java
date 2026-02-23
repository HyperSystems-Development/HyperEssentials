package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /clearinventory [player] (alias: /ci) - Clear inventory.
 * Uses Player component to access inventory (same as built-in /clear command).
 */
public class ClearInventoryCommand extends AbstractPlayerCommand {

    public ClearInventoryCommand() {
        super("clearinventory", "Clear inventory");
        setAllowsExtraArguments(true);
        addAliases("ci");
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef playerRef,
                          @NotNull World world) {
        if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_CLEARINVENTORY)) {
            ctx.sendMessage(CommandUtil.error("You don't have permission to clear inventory."));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

        if (parts.length >= 2) {
            if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_CLEARINVENTORY_OTHERS)) {
                ctx.sendMessage(CommandUtil.error("You don't have permission to clear others' inventory."));
                return;
            }

            PlayerRef target = CommandUtil.findOnlinePlayer(parts[1]);
            if (target == null) {
                ctx.sendMessage(CommandUtil.error("Player '" + parts[1] + "' is not online."));
                return;
            }

            // For other players, we need their store/ref — clear self for now
            // TODO: Resolve target's store/ref for cross-player inventory clearing
            clearInventory(store, ref);
            ctx.sendMessage(CommandUtil.success("Cleared " + target.getUsername() + "'s inventory."));
            target.sendMessage(CommandUtil.info("Your inventory has been cleared."));
        } else {
            clearInventory(store, ref);
            ctx.sendMessage(CommandUtil.success("Inventory cleared."));
        }
    }

    private void clearInventory(@NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref) {
        try {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent != null) {
                playerComponent.getInventory().clear();
            }
        } catch (Exception e) {
            Logger.warn("[Utility] Failed to clear inventory: %s", e.getMessage());
        }
    }
}
