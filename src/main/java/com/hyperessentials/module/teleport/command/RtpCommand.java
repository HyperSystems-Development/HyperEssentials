package com.hyperessentials.module.teleport.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Location;
import com.hyperessentials.module.teleport.RtpManager;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.module.warmup.WarmupTask;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * /rtp - Teleport to a random location.
 */
public class RtpCommand extends AbstractPlayerCommand {

  private final RtpManager rtpManager;
  private final WarmupManager warmupManager;

  public RtpCommand(@NotNull RtpManager rtpManager, @NotNull WarmupManager warmupManager) {
    super("rtp", "Teleport to a random location");
    this.rtpManager = rtpManager;
    this.warmupManager = warmupManager;
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
      ctx.sendMessage(CommandUtil.error("You don't have permission to use random teleport."));
      return;
    }

    String worldName = currentWorld.getName();

    if (rtpManager.isWorldBlacklisted(worldName)) {
      ctx.sendMessage(CommandUtil.error("Random teleport is not allowed in this world."));
      return;
    }

    if (warmupManager.isOnCooldown(uuid, "rtp", "rtp")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "rtp", "rtp");
      ctx.sendMessage(CommandUtil.error("On cooldown. " + remaining + "s remaining."));
      return;
    }

    Location destination = rtpManager.findRandomLocation(worldName);
    if (destination == null) {
      ctx.sendMessage(CommandUtil.error("Could not find a random location."));
      return;
    }

    WarmupTask task = warmupManager.startWarmup(uuid, "rtp", "rtp", () -> {
      executeTeleport(store, ref, destination);
      ctx.sendMessage(CommandUtil.success(String.format("Teleported to random location! (%.0f, %.0f, %.0f)",
        destination.x(), destination.y(), destination.z())));
    });

    if (task != null) {
      ctx.sendMessage(CommandUtil.info("Teleporting in " + task.warmupSeconds() + "s... Don't move!"));
    }
  }

  private void executeTeleport(Store<EntityStore> store, Ref<EntityStore> ref, Location dest) {
    World targetWorld = Universe.get().getWorld(dest.world());
    if (targetWorld == null) {
      return;
    }
    targetWorld.execute(() -> {
      Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
      Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
      Teleport teleport = new Teleport(targetWorld, position, rotation);
      store.addComponent(ref, Teleport.getComponentType(), teleport);
    });
  }
}
