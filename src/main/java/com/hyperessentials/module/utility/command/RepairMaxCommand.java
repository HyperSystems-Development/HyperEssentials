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

      double currentMax = heldItem.getMaxDurability();
      if (currentMax <= 0) {
        ctx.sendMessage(CommandUtil.error("This item cannot be repaired."));
        return;
      }

      double defaultMax = heldItem.getItem().getMaxDurability();
      if (heldItem.getDurability() >= defaultMax && currentMax >= defaultMax) {
        ctx.sendMessage(CommandUtil.info("Item is already at full default durability."));
        return;
      }

      // Restore both current and max durability to the item's default
      ItemStack repaired = heldItem.withRestoredDurability(defaultMax);
      byte activeSlot = inventory.getActiveHotbarSlot();
      inventory.getHotbar().setItemStackForSlot(activeSlot, repaired);
      ctx.sendMessage(CommandUtil.success("Item fully restored to default durability."));
    } catch (Exception e) {
      Logger.warn("[Utility] Failed to fully repair item: %s", e.getMessage());
      ctx.sendMessage(CommandUtil.error("Failed to repair item."));
    }
  }
}
