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
 * /durability set <number> - Set the max durability of the held item.
 * /durability reset - Reset the held item's max durability to its default.
 */
public class DurabilityCommand extends AbstractPlayerCommand {

  public DurabilityCommand() {
    super("durability", "Modify item durability");
    addAliases("dura");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_DURABILITY)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to modify durability."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /durability set <number> | /durability reset"));
      return;
    }

    String subCommand = parts[1].toLowerCase();

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

      if (heldItem.getMaxDurability() <= 0) {
        ctx.sendMessage(CommandUtil.error("This item has no durability."));
        return;
      }

      byte activeSlot = inventory.getActiveHotbarSlot();

      switch (subCommand) {
        case "set" -> {
          if (parts.length < 3) {
            ctx.sendMessage(CommandUtil.error("Usage: /durability set <number>"));
            return;
          }

          double value;
          try {
            value = Double.parseDouble(parts[2]);
          } catch (NumberFormatException e) {
            ctx.sendMessage(CommandUtil.error("Invalid number: " + parts[2]));
            return;
          }

          if (value <= 0) {
            ctx.sendMessage(CommandUtil.error("Durability must be greater than 0."));
            return;
          }

          // Set both max and current durability to the new value
          ItemStack modified = heldItem.withRestoredDurability(value);
          inventory.getHotbar().setItemStackForSlot(activeSlot, modified);
          ctx.sendMessage(CommandUtil.success("Max durability set to " + (int) value + "."));
        }
        case "reset" -> {
          double defaultMax = heldItem.getItem().getMaxDurability();
          ItemStack reset = heldItem.withRestoredDurability(defaultMax);
          inventory.getHotbar().setItemStackForSlot(activeSlot, reset);
          ctx.sendMessage(CommandUtil.success("Durability reset to default (" + (int) defaultMax + ")."));
        }
        default -> ctx.sendMessage(CommandUtil.error("Usage: /durability set <number> | /durability reset"));
      }
    } catch (Exception e) {
      Logger.warn("[Utility] Failed to modify durability: %s", e.getMessage());
      ctx.sendMessage(CommandUtil.error("Failed to modify durability."));
    }
  }
}
