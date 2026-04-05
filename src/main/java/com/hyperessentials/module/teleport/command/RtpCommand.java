package com.hyperessentials.module.teleport.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Location;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.teleport.RtpManager;
import com.hyperessentials.module.teleport.RtpManager.RtpResult;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.module.warmup.WarmupTask;
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
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * /rtp - Teleport to a random location.
 */
public class RtpCommand extends AbstractPlayerCommand {

  private final RtpManager rtpManager;
  private final WarmupManager warmupManager;
  private final BackManager backManager;

  public RtpCommand(@NotNull RtpManager rtpManager, @NotNull WarmupManager warmupManager,
                    @Nullable BackManager backManager) {
    super("rtp", "Teleport to a random location");
    this.rtpManager = rtpManager;
    this.warmupManager = warmupManager;
    this.backManager = backManager;
    addAliases("randomtp", "randomteleport");
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
                         @NotNull Store<EntityStore> store,
                         @NotNull Ref<EntityStore> ref,
                         @NotNull PlayerRef playerRef,
                         @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.RTP)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Rtp.NO_PERMISSION));
      return;
    }

    String worldName = currentWorld.getName();

    if (rtpManager.isWorldBlacklisted(worldName)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Rtp.WORLD_BLACKLISTED));
      return;
    }

    if (warmupManager.isOnCooldown(uuid, "rtp", "rtp")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "rtp", "rtp");
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.ON_COOLDOWN, remaining));
      return;
    }

    // Get player position for player-relative RTP
    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.CANNOT_GET_POSITION));
      return;
    }

    Vector3d pos = transform.getPosition();
    double playerX = pos.x;
    double playerZ = pos.z;

    // Save back location before teleport
    saveBackLocation(uuid, pos, currentWorld);

    boolean bypassFactions = CommandUtil.hasPermission(uuid, Permissions.RTP_BYPASS_FACTIONS);

    ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Rtp.SEARCHING, HEMessageUtil.COLOR_YELLOW));

    // Run search on world thread (chunk/block access required)
    currentWorld.execute(() -> {
      RtpResult result = rtpManager.findSafeRandomLocation(
        currentWorld, worldName, playerX, playerZ, bypassFactions);

      if (result instanceof RtpResult.Failure failure) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.CANNOT_GET_POSITION));
        return;
      }

      Location destination = ((RtpResult.Success) result).location();

      WarmupTask task = warmupManager.startWarmup(uuid, "rtp", "rtp", () -> {
        executeTeleport(ref, destination, () -> {
          ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Rtp.TELEPORTED,
            String.format("%.0f", destination.x()),
            String.format("%.0f", destination.y()),
            String.format("%.0f", destination.z())));
        });
      });

      if (task != null) {
        ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Rtp.FOUND_LOCATION, HEMessageUtil.COLOR_YELLOW,
          String.format("%.0f", destination.x()),
          String.format("%.0f", destination.y()),
          String.format("%.0f", destination.z()),
          task.warmupSeconds()));
      }
    });
  }

  private void saveBackLocation(@NotNull UUID uuid, @NotNull Vector3d pos, @NotNull World currentWorld) {
    if (backManager == null) return;
    try {
      Location currentLoc = new Location(currentWorld.getName(),
          currentWorld.getWorldConfig().getUuid().toString(),
          pos.getX(), pos.getY(), pos.getZ(), 0, 0);
      backManager.onTeleport(uuid, currentLoc, "rtp");
    } catch (Exception e) {
      ErrorHandler.report("[RTP] Failed to save back location", e);
    }
  }

  private void executeTeleport(Ref<EntityStore> ref, Location dest, Runnable onComplete) {
    World targetWorld = Universe.get().getWorld(UUID.fromString(dest.worldUuid()));
    if (targetWorld == null) {
      return;
    }
    targetWorld.execute(() -> {
      if (!ref.isValid()) {
        return;
      }
      Store<EntityStore> store = ref.getStore();
      Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
      Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
      Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
      store.addComponent(ref, Teleport.getComponentType(), teleport);
      if (onComplete != null) {
        onComplete.run();
      }
    });
  }
}
