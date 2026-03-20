package com.hyperessentials.module.teleport.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.TeleportRequest;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
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
    addAliases("tpc");
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.TPCANCEL)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.CANCEL_NO_PERMISSION));
      return;
    }

    TeleportRequest request = tpaManager.cancelOutgoingRequest(uuid);

    if (request == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.CANCEL_NO_PENDING));
      return;
    }

    ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Tpa.CANCEL_SUCCESS));

    PlayerRef targetRef = findPlayerByUuid(request.target());
    if (targetRef != null) {
      targetRef.sendMessage(HEMessageUtil.info(targetRef, CommandKeys.Tpa.CANCEL_NOTIFY, HEMessageUtil.COLOR_YELLOW, playerRef.getUsername()));
    }
  }

  private PlayerRef findPlayerByUuid(UUID uuid) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    return plugin != null ? plugin.getTrackedPlayer(uuid) : null;
  }
}
