package com.hyperessentials.module.teleport.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.teleport.TpaManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * /tptoggle - Toggle accepting teleport requests.
 */
public class TpToggleCommand extends AbstractPlayerCommand {

  private final TpaManager tpaManager;

  public TpToggleCommand(@NotNull TpaManager tpaManager) {
    super("tptoggle", "Toggle accepting teleport requests");
    this.tpaManager = tpaManager;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.TPTOGGLE)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to toggle TPA."));
      return;
    }

    boolean newState = tpaManager.toggleTpToggle(uuid);

    if (newState) {
      ctx.sendMessage(CommandUtil.success("You are now accepting teleport requests."));
    } else {
      ctx.sendMessage(CommandUtil.error("You are no longer accepting teleport requests."));
    }
  }
}
