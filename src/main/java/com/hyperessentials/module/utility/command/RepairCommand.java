package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /repair - Repair the item in hand.
 * Uses Player component -> Inventory -> getItemInHand() pattern.
 */
public class RepairCommand extends AbstractPlayerCommand {

    public RepairCommand() {
        super("repair", "Repair held item");
    }

    @Override
    protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef playerRef,
                          @NotNull World world) {
        if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_REPAIR)) {
            ctx.sendMessage(CommandUtil.error("You don't have permission to repair items."));
            return;
        }

        try {
            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null) {
                ctx.sendMessage(CommandUtil.error("Cannot access player data."));
                return;
            }

            Inventory inventory = playerComponent.getInventory();
            ItemStack heldItem = inventory.getItemInHand();

            if (heldItem == null || heldItem.isEmpty()) {
                ctx.sendMessage(CommandUtil.error("You are not holding an item."));
                return;
            }

            double maxDurability = heldItem.getMaxDurability();
            if (maxDurability <= 0) {
                ctx.sendMessage(CommandUtil.error("This item cannot be repaired."));
                return;
            }

            // Use withRestoredDurability to set both current and max durability
            ItemStack repaired = heldItem.withRestoredDurability(maxDurability);

            // Replace in the active hotbar slot
            byte activeSlot = inventory.getActiveHotbarSlot();
            inventory.getHotbar().setItemStackForSlot(activeSlot, repaired);
            ctx.sendMessage(CommandUtil.success("Item repaired."));
        } catch (Exception e) {
            Logger.warn("[Utility] Failed to repair item: %s", e.getMessage());
            ctx.sendMessage(CommandUtil.error("Failed to repair item."));
        }
    }
}
