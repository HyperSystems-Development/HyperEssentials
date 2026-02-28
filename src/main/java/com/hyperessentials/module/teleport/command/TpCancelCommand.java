package com.hyperessentials.module.teleport.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.TeleportRequest;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
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
 * /tpcancel - Cancel your outgoing teleport request.
 */
public class TpCancelCommand extends AbstractPlayerCommand {

  private final TpaManager tpaManager;

  public TpCancelCommand(@NotNull TpaManager tpaManager) {
    super("tpcancel", "Cancel your teleport request");
    this.tpaManager = tpaManager;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.TPCANCEL)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to cancel requests."));
      return;
    }

    TeleportRequest request = tpaManager.cancelOutgoingRequest(uuid);

    if (request == null) {
      ctx.sendMessage(CommandUtil.error("You have no pending teleport request to cancel."));
      return;
    }

    ctx.sendMessage(CommandUtil.success("Teleport request cancelled."));

    PlayerRef targetRef = findPlayerByUuid(request.target());
    if (targetRef != null) {
      targetRef.sendMessage(CommandUtil.info(playerRef.getUsername() + " cancelled their teleport request."));
    }
  }

  private PlayerRef findPlayerByUuid(UUID uuid) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    return plugin != null ? plugin.getTrackedPlayer(uuid) : null;
  }
}
