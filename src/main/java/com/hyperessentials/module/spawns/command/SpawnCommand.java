package com.hyperessentials.module.spawns.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.modules.SpawnsConfig;
import com.hyperessentials.data.Location;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hyperessentials.module.teleport.BackManager;
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
 * /spawn [world] - Teleport to a world's spawn or the global spawn.
 */
public class SpawnCommand extends AbstractPlayerCommand {

  private final SpawnManager spawnManager;
  private final WarmupManager warmupManager;
  private final BackManager backManager;

  public SpawnCommand(@NotNull SpawnManager spawnManager, @NotNull SpawnsConfig config,
            @NotNull WarmupManager warmupManager, @Nullable BackManager backManager) {
    super("spawn", "Teleport to spawn");
    this.spawnManager = spawnManager;
    this.warmupManager = warmupManager;
    this.backManager = backManager;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.SPAWN)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Spawn.NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    String worldArg = parts.length > 1 ? parts[1] : null;

    Spawn spawn;
    if (worldArg != null) {
      spawn = findSpawnByWorldName(worldArg);
      if (spawn == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Spawn.NOT_FOUND_WORLD, worldArg));
        return;
      }
    } else {
      // Try current world first, then fall back to global spawn
      String currentWorldUuid = currentWorld.getWorldConfig().getUuid().toString();
      spawn = spawnManager.getSpawnForWorld(currentWorldUuid);
      if (spawn == null) {
        spawn = spawnManager.getGlobalSpawn();
      }
    }

    if (spawn == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Spawn.NOT_SET));
      return;
    }

    if (warmupManager.isOnCooldown(uuid, "spawns", "spawn")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "spawns", "spawn");
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.ON_COOLDOWN, remaining));
      return;
    }

    // Save back location before teleport
    saveBackLocation(uuid, store, ref, currentWorld);

    Location destination = Location.fromSpawn(spawn);

    WarmupTask task = warmupManager.startWarmup(uuid, "spawns", "spawn", () -> {
      executeTeleport(ref, destination, () -> {
        ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Spawn.TELEPORTED));
      });
    });

    if (task != null) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Common.WARMUP_STARTING, HEMessageUtil.COLOR_YELLOW, task.warmupSeconds()));
    }
  }

  private Spawn findSpawnByWorldName(String worldName) {
    for (Spawn s : spawnManager.getAllSpawns()) {
      if (s.worldName().equalsIgnoreCase(worldName)) {
        return s;
      }
    }
    return null;
  }

  private void saveBackLocation(@NotNull UUID uuid, @NotNull Store<EntityStore> store,
                                 @NotNull Ref<EntityStore> ref, @NotNull World currentWorld) {
    if (backManager == null) return;
    try {
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      if (transform != null) {
        Vector3d pos = transform.getPosition();
        Location currentLoc = new Location(currentWorld.getName(),
            currentWorld.getWorldConfig().getUuid().toString(),
            pos.getX(), pos.getY(), pos.getZ(), 0, 0);
        backManager.onTeleport(uuid, currentLoc, "spawn");
      }
    } catch (Exception e) {
      ErrorHandler.report("[Spawns] Failed to save back location", e);
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
