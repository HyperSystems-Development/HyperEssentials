package com.hyperessentials.module.teleport.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Location;
import com.hyperessentials.module.teleport.BackManager;
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
 * /back - Return to your previous location.
 */
public class BackCommand extends AbstractPlayerCommand {

  private final BackManager backManager;
  private final WarmupManager warmupManager;

  public BackCommand(@NotNull BackManager backManager, @NotNull WarmupManager warmupManager) {
    super("back", "Return to your previous location");
    this.backManager = backManager;
    this.warmupManager = warmupManager;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.BACK)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to use /back."));
      return;
    }

    Location backLocation = backManager.getBackLocation(uuid);
    if (backLocation == null) {
      ctx.sendMessage(CommandUtil.error("No back location found."));
      return;
    }

    if (warmupManager.isOnCooldown(uuid, "teleport", "back")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "teleport", "back");
      ctx.sendMessage(CommandUtil.error("On cooldown. " + remaining + "s remaining."));
      return;
    }

    // Pop the back location (remove from history since we're using it)
    backManager.popBackLocation(uuid);

    Location destination = backLocation;

    WarmupTask task = warmupManager.startWarmup(uuid, "teleport", "back", () -> {
      executeTeleport(ref, destination, () -> {
        ctx.sendMessage(CommandUtil.success("Teleported to previous location!"));
      });
    });

    if (task != null) {
      ctx.sendMessage(CommandUtil.info("Teleporting in " + task.warmupSeconds() + "s... Don't move!"));
    }
  }

  private void executeTeleport(Ref<EntityStore> ref, Location dest, Runnable onComplete) {
    World targetWorld = Universe.get().getWorld(dest.world());
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
