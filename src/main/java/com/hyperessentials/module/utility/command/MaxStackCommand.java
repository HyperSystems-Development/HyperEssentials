package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.HEMessageUtil;
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.MAXSTACK_NO_PERMISSION));
      return;
    }

    try {
      Player playerComponent = store.getComponent(ref, Player.getComponentType());
      if (playerComponent == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.CANNOT_ACCESS_PLAYER));
        return;
      }

      Inventory inventory = playerComponent.getInventory();
      ItemStack heldItem = inventory.getItemInHand();

      if (heldItem == null || heldItem.isEmpty()) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.NOT_HOLDING_ITEM));
        return;
      }

      int maxStack = heldItem.getItem().getMaxStack();
      if (maxStack <= 1) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.MAXSTACK_CANNOT_STACK));
        return;
      }

      if (heldItem.getQuantity() >= maxStack) {
        ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Utility.MAXSTACK_ALREADY_MAX, HEMessageUtil.COLOR_YELLOW, maxStack));
        return;
      }

      ItemStack maxed = heldItem.withQuantity(maxStack);
      byte activeSlot = inventory.getActiveHotbarSlot();
      inventory.getHotbar().setItemStackForSlot(activeSlot, maxed);
      ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Utility.MAXSTACK_SUCCESS, maxStack));
    } catch (Exception e) {
      ErrorHandler.report("[Utility] Failed to max stack", e);
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.MAXSTACK_FAILED));
    }
  }
}
