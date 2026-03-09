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
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /trash - Open a trash disposal inventory.
 * Items placed into this container are discarded when the window closes.
 */
public class TrashCommand extends AbstractPlayerCommand {

  public TrashCommand() {
    super("trash", "Open trash disposal");
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_TRASH)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to use trash."));
      return;
    }

    try {
      Player player = store.getComponent(ref, Player.getComponentType());
      if (player == null) {
        ctx.sendMessage(CommandUtil.error("Could not resolve player."));
        return;
      }

      // Create an empty container — items placed here are discarded when window closes
      SimpleItemContainer trash = new SimpleItemContainer((short) 36);
      ContainerWindow window = new ContainerWindow(trash);
      player.getPageManager().setPageWithWindows(ref, store, Page.Bench, true, window);
    } catch (Exception e) {
      Logger.warn("[Utility] Failed to open trash: %s", e.getMessage());
      ctx.sendMessage(CommandUtil.error("Failed to open trash."));
    }
  }
}
