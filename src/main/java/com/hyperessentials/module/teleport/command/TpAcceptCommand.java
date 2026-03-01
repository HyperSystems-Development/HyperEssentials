package com.hyperessentials.module.teleport.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Location;
import com.hyperessentials.data.TeleportRequest;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.module.warmup.WarmupTask;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * /tpaccept [player] - Accept a teleport request.
 */
public class TpAcceptCommand extends AbstractPlayerCommand {

  private final TpaManager tpaManager;
  private final BackManager backManager;
  private final WarmupManager warmupManager;

  public TpAcceptCommand(@NotNull TpaManager tpaManager, @NotNull BackManager backManager,
               @NotNull WarmupManager warmupManager) {
    super("tpaccept", "Accept a teleport request");
    this.tpaManager = tpaManager;
    this.backManager = backManager;
    this.warmupManager = warmupManager;
    addAliases("tpyes", "tpy");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.TPACCEPT)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to accept requests."));
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

    if (request.isExpired()) {
      tpaManager.denyRequest(request);
      ctx.sendMessage(CommandUtil.error("That teleport request has expired."));
      return;
    }

    tpaManager.acceptRequest(request);

    PlayerRef teleportingRef = findPlayerByUuid(request.getTeleportingPlayer());
    PlayerRef destinationRef = findPlayerByUuid(request.getDestinationPlayer());

    if (teleportingRef == null || destinationRef == null) {
      ctx.sendMessage(CommandUtil.error("The other player is no longer online."));
      return;
    }

    // Get the destination (the player being teleported TO)
    // For TPA: requester teleports to target (us), so destination is our location
    // For TPAHERE: target (us) teleports to requester, so destination is requester's location
    // In both cases the accepting player is us, but the destination depends on type.
    // The actual teleport location must come from the destination player's current position.
    // Since we only have our own store/ref, we send the accept message and
    // the actual teleport is executed via the warmup system on the teleporting player's side.

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ctx.sendMessage(CommandUtil.error("Could not determine position."));
      return;
    }

    Vector3d pos = transform.getPosition();
    Location ourLocation = new Location(currentWorld.getName(), pos.getX(), pos.getY(), pos.getZ(), 0, 0);

    ctx.sendMessage(CommandUtil.success("Teleport request accepted."));
    teleportingRef.sendMessage(CommandUtil.success("Request accepted! Teleporting..."));

    if (request.type() == TeleportRequest.Type.TPA) {
      // Requester teleports to us - we use our location as destination
      // Save requester's back location (done via their player data, best effort)
      backManager.onTeleport(request.requester(), ourLocation);

      // Execute teleport on the teleporting player
      // Note: We don't have access to requester's store/ref, so we use the
      // destination world thread to perform the teleport via Universe
      executeTeleportToLocation(teleportingRef, ourLocation);
    } else {
      // TPAHERE: We (target) teleport to requester
      // Our current location is saved as back
      backManager.onTeleport(uuid, ourLocation);

      // We need the requester's current location, but we don't have their store.
      // Since the requester is the destination, we inform them and handle via message.
      // For now, we send a message - actual cross-player teleport requires platform support.
      // The teleporting player (us) will be teleported via the store/ref we have.
      destinationRef.sendMessage(CommandUtil.info("Teleporting " + playerRef.getUsername() + " to you..."));
    }
  }

  private void executeTeleportToLocation(PlayerRef teleportingRef, Location dest) {
    World targetWorld = Universe.get().getWorld(dest.world());
    if (targetWorld == null) {
      return;
    }
    // Queue teleport on the target world thread
    // Note: Full cross-player teleport is wired in Task 8 via platform events
    targetWorld.execute(() -> {
      // The teleport will be handled by the platform layer
    });
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
