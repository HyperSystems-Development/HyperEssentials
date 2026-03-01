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
      ctx.sendMessage(CommandUtil.error("You don't have permission to deny requests."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    String requesterName = parts.length > 1 ? parts[1] : null;

    TeleportRequest request;
    if (requesterName != null) {
      PlayerRef requesterRef = findPlayer(requesterName);
      if (requesterRef == null) {
        ctx.sendMessage(CommandUtil.error("Player '" + requesterName + "' not found or offline."));
        return;
      }
      request = tpaManager.getIncomingRequest(uuid, requesterRef.getUuid());
      if (request == null) {
        ctx.sendMessage(CommandUtil.error("No pending request from " + requesterName + "."));
        return;
      }
    } else {
      request = tpaManager.getMostRecentIncomingRequest(uuid);
      if (request == null) {
        ctx.sendMessage(CommandUtil.error("You have no pending teleport requests."));
        return;
      }
    }

    tpaManager.denyRequest(request);

    ctx.sendMessage(CommandUtil.success("Teleport request denied."));

    PlayerRef requesterRef = findPlayerByUuid(request.requester());
    if (requesterRef != null) {
      requesterRef.sendMessage(CommandUtil.error(playerRef.getUsername() + " denied your teleport request."));
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
