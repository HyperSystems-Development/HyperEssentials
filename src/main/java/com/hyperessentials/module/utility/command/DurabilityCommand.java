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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.DURA_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.DURA_USAGE));
      return;
    }

    String subCommand = parts[1].toLowerCase();

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

      if (heldItem.getMaxDurability() <= 0) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.DURA_NO_DURABILITY));
        return;
      }

      byte activeSlot = inventory.getActiveHotbarSlot();

      switch (subCommand) {
        case "set" -> {
          if (parts.length < 3) {
            ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.DURA_SET_USAGE));
            return;
          }

          double value;
          try {
            value = Double.parseDouble(parts[2]);
          } catch (NumberFormatException e) {
            ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.DURA_INVALID_NUMBER, parts[2]));
            return;
          }

          if (value <= 0) {
            ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.DURA_MUST_POSITIVE));
            return;
          }

          // Set both max and current durability to the new value
          ItemStack modified = heldItem.withRestoredDurability(value);
          inventory.getHotbar().setItemStackForSlot(activeSlot, modified);
          ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Utility.DURA_SET_SUCCESS, (int) value));
        }
        case "reset" -> {
          double defaultMax = heldItem.getItem().getMaxDurability();
          ItemStack reset = heldItem.withRestoredDurability(defaultMax);
          inventory.getHotbar().setItemStackForSlot(activeSlot, reset);
          ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Utility.DURA_RESET_SUCCESS, (int) defaultMax));
        }
        default -> ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.DURA_USAGE));
      }
    } catch (Exception e) {
      ErrorHandler.report("[Utility] Failed to modify durability", e);
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.DURA_FAILED));
    }
  }
}
