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
 * /repairmax - Fully restore the item in hand (reset max durability to item default and fill).
 */
public class RepairMaxCommand extends AbstractPlayerCommand {

  public RepairMaxCommand() {
    super("repairmax", "Fully restore held item durability");
    addAliases("fixmax");
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_REPAIR)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.REPAIR_NO_PERMISSION));
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

      double currentMax = heldItem.getMaxDurability();
      if (currentMax <= 0) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.REPAIR_CANNOT_REPAIR));
        return;
      }

      double defaultMax = heldItem.getItem().getMaxDurability();
      if (heldItem.getDurability() >= defaultMax && currentMax >= defaultMax) {
        ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Utility.REPAIRMAX_ALREADY_FULL, HEMessageUtil.COLOR_YELLOW));
        return;
      }

      // Restore both current and max durability to the item's default
      ItemStack repaired = heldItem.withRestoredDurability(defaultMax);
      byte activeSlot = inventory.getActiveHotbarSlot();
      inventory.getHotbar().setItemStackForSlot(activeSlot, repaired);
      ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Utility.REPAIRMAX_SUCCESS));
    } catch (Exception e) {
      ErrorHandler.report("[Utility] Failed to fully repair item", e);
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.REPAIR_FAILED));
    }
  }
}
