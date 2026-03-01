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
 * /maxstack (/stack) - Set held item to its max stack size.
 */
public class MaxStackCommand extends AbstractPlayerCommand {

  public MaxStackCommand() {
    super("maxstack", "Set held item to max stack size");
    addAliases("stack");
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_MAXSTACK)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to max stack items."));
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

      int maxStack = heldItem.getItem().getMaxStack();
      if (maxStack <= 1) {
        ctx.sendMessage(CommandUtil.error("This item cannot be stacked."));
        return;
      }

      if (heldItem.getQuantity() >= maxStack) {
        ctx.sendMessage(CommandUtil.info("Item is already at max stack size (" + maxStack + ")."));
        return;
      }

      ItemStack maxed = heldItem.withQuantity(maxStack);
      byte activeSlot = inventory.getActiveHotbarSlot();
      inventory.getHotbar().setItemStackForSlot(activeSlot, maxed);
      ctx.sendMessage(CommandUtil.success("Item stack set to " + maxStack + "."));
    } catch (Exception e) {
      Logger.warn("[Utility] Failed to max stack: %s", e.getMessage());
      ctx.sendMessage(CommandUtil.error("Failed to max stack item."));
    }
  }
}
