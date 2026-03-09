package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.windows.ContainerWindow;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.DelegateItemContainer;
import com.hypixel.hytale.server.core.inventory.container.filter.FilterType;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /invsee <player> - View another player's inventory (read-only).
 * Uses the same pattern as Hytale's built-in InventorySeeCommand.
 */
public class InvSeeCommand extends AbstractPlayerCommand {

  public InvSeeCommand() {
    super("invsee", "View a player's inventory");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_INVSEE)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to view inventories."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /invsee <player>"));
      return;
    }

    PlayerRef target = CommandUtil.findOnlinePlayer(parts[1]);
    if (target == null) {
      ctx.sendMessage(CommandUtil.error("Player '" + parts[1] + "' is not online."));
      return;
    }

    Player ourPlayer = store.getComponent(ref, Player.getComponentType());
    if (ourPlayer == null) {
      ctx.sendMessage(CommandUtil.error("Could not resolve player."));
      return;
    }

    // Resolve target's entity ref
    Ref<EntityStore> targetRef = target.getReference();
    if (targetRef == null || !targetRef.isValid()) {
      ctx.sendMessage(CommandUtil.error("Player '" + parts[1] + "' is not in a world."));
      return;
    }
    Store<EntityStore> targetStore = targetRef.getStore();

    // Execute on target's world thread for thread safety
    World targetWorld = targetStore.getExternalData().getWorld();
    targetWorld.execute(() -> {
      try {
        Player targetPlayer = targetStore.getComponent(targetRef, Player.getComponentType());
        if (targetPlayer == null) {
          ctx.sendMessage(CommandUtil.error("Could not access player's inventory."));
          return;
        }

        CombinedItemContainer targetInventory = targetPlayer.getInventory().getCombinedHotbarFirst();

        // Wrap as read-only
        DelegateItemContainer<CombinedItemContainer> readOnly = new DelegateItemContainer<>(targetInventory);
        readOnly.setGlobalFilter(FilterType.DENY_ALL);

        // Open as Container Window (bench page)
        ourPlayer.getPageManager().setPageWithWindows(ref, store, Page.Bench, true,
            new ContainerWindow(readOnly));
      } catch (Exception e) {
        Logger.warn("[Utility] Failed to open invsee: %s", e.getMessage());
        ctx.sendMessage(CommandUtil.error("Failed to open inventory view."));
      }
    });
  }
}
