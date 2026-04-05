package com.hyperessentials.module.teleport.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Location;
import com.hyperessentials.data.TeleportRequest;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.HEMessageUtil;
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.ACCEPT_NO_PERMISSION));
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
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.ACCEPT_NO_REQUEST_FROM, requesterName));
        return;
      }
    } else {
      request = tpaManager.getMostRecentIncomingRequest(uuid);
      if (request == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.ACCEPT_NO_PENDING));
        return;
      }
    }

    if (request.isExpired()) {
      tpaManager.denyRequest(request);
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.ACCEPT_EXPIRED));
      return;
    }

    tpaManager.acceptRequest(request);

    PlayerRef teleportingRef = findPlayerByUuid(request.getTeleportingPlayer());
    PlayerRef destinationRef = findPlayerByUuid(request.getDestinationPlayer());

    if (teleportingRef == null || destinationRef == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.ACCEPT_PLAYER_OFFLINE));
      return;
    }

    ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Tpa.ACCEPT_SUCCESS));
    teleportingRef.sendMessage(HEMessageUtil.success(teleportingRef, CommandKeys.Tpa.ACCEPT_TELEPORTING));

    if (request.type() == TeleportRequest.Type.TPA) {
      // TPA: Requester teleports to us (the acceptor)
      // Save requester's current location as back
      saveBackForPlayer(teleportingRef);

      // Our location is the destination
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      if (transform == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.CANNOT_GET_POSITION));
        return;
      }
      Vector3d pos = transform.getPosition();
      Location dest = new Location(currentWorld.getName(),
          currentWorld.getWorldConfig().getUuid().toString(),
          pos.getX(), pos.getY(), pos.getZ(), 0, 0);

      executeTeleportPlayer(teleportingRef, dest);
    } else {
      // TPAHERE: We (acceptor/target) teleport to requester
      // Save our current location as back
      if (backManager != null) {
        TransformComponent ourTransform = store.getComponent(ref, TransformComponent.getComponentType());
        if (ourTransform != null) {
          Vector3d ourPos = ourTransform.getPosition();
          Location ourLoc = new Location(currentWorld.getName(),
              currentWorld.getWorldConfig().getUuid().toString(),
              ourPos.getX(), ourPos.getY(), ourPos.getZ(), 0, 0);
          backManager.onTeleport(uuid, ourLoc, "tpa");
        }
      }

      // Get requester's current position
      Ref<EntityStore> requesterEntityRef = destinationRef.getReference();
      if (requesterEntityRef == null || !requesterEntityRef.isValid()) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.CANNOT_ACCESS_PLAYER));
        return;
      }
      Store<EntityStore> requesterStore = requesterEntityRef.getStore();
      TransformComponent requesterTransform = requesterStore.getComponent(
          requesterEntityRef, TransformComponent.getComponentType());
      if (requesterTransform == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.CANNOT_GET_POSITION));
        return;
      }

      Vector3d reqPos = requesterTransform.getPosition();
      World requesterWorld = requesterStore.getExternalData().getWorld();
      Location dest = new Location(requesterWorld.getName(),
          requesterWorld.getWorldConfig().getUuid().toString(),
          reqPos.getX(), reqPos.getY(), reqPos.getZ(), 0, 0);

      // Teleport us (the acceptor) to the requester
      executeTeleportPlayer(playerRef, dest);

      destinationRef.sendMessage(HEMessageUtil.info(destinationRef, CommandKeys.Tpa.ACCEPT_TELEPORTING_TO_YOU, HEMessageUtil.COLOR_YELLOW, playerRef.getUsername()));
    }
  }

  private void executeTeleportPlayer(PlayerRef teleportingPlayer, Location dest) {
    World targetWorld = Universe.get().getWorld(UUID.fromString(dest.worldUuid()));
    if (targetWorld == null) return;

    targetWorld.execute(() -> {
      Ref<EntityStore> tpRef = teleportingPlayer.getReference();
      if (tpRef == null || !tpRef.isValid()) return;
      Store<EntityStore> tpStore = tpRef.getStore();

      Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
      Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
      Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
      tpStore.addComponent(tpRef, Teleport.getComponentType(), teleport);
    });
  }

  private void saveBackForPlayer(PlayerRef player) {
    if (backManager == null) return;
    try {
      Ref<EntityStore> playerEntityRef = player.getReference();
      if (playerEntityRef == null || !playerEntityRef.isValid()) return;
      Store<EntityStore> playerStore = playerEntityRef.getStore();
      TransformComponent transform = playerStore.getComponent(
          playerEntityRef, TransformComponent.getComponentType());
      if (transform == null) return;

      Vector3d pos = transform.getPosition();
      World world = playerStore.getExternalData().getWorld();
      Location loc = new Location(world.getName(),
          world.getWorldConfig().getUuid().toString(),
          pos.getX(), pos.getY(), pos.getZ(), 0, 0);
      backManager.onTeleport(player.getUuid(), loc, "tpa");
    } catch (Exception e) {
      ErrorHandler.report("[TPA] Failed to save back location", e);
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
