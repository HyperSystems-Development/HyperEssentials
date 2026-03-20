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
 * /tpdeny [player] - Deny a teleport request.
 */
public class TpDenyCommand extends AbstractPlayerCommand {

  private final TpaManager tpaManager;

  public TpDenyCommand(@NotNull TpaManager tpaManager) {
    super("tpdeny", "Deny a teleport request");
    this.tpaManager = tpaManager;
    addAliases("tpno", "tpn");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.TPDENY)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.DENY_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    String requesterName = parts.length > 1 ? parts[1] : null;

    TeleportRequest request;
    if (requesterName != null) {
      PlayerRef requesterRef = findPlayer(requesterName);
      if (requesterRef == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.PLAYER_NOT_FOUND, requesterName));
        return;
      }
      request = tpaManager.getIncomingRequest(uuid, requesterRef.getUuid());
      if (request == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.DENY_NO_REQUEST_FROM, requesterName));
        return;
      }
    } else {
      request = tpaManager.getMostRecentIncomingRequest(uuid);
      if (request == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.DENY_NO_PENDING));
        return;
      }
    }

    tpaManager.denyRequest(request);

    ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Tpa.DENY_SUCCESS));

    PlayerRef requesterRef = findPlayerByUuid(request.requester());
    if (requesterRef != null) {
      requesterRef.sendMessage(HEMessageUtil.error(requesterRef, CommandKeys.Tpa.DENY_NOTIFY, playerRef.getUsername()));
    }
  }

  private PlayerRef findPlayer(String name) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    return plugin != null ? plugin.findOnlinePlayer(name) : null;
  }

  private PlayerRef findPlayerByUuid(UUID uuid) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    return plugin != null ? plugin.getTrackedPlayer(uuid) : null;
  }
}
