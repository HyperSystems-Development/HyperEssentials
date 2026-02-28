package com.hyperessentials.module.spawns.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.modules.SpawnsConfig;
import com.hyperessentials.data.Location;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.module.warmup.WarmupTask;
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
 * /spawn [name] - Teleport to spawn.
 */
public class SpawnCommand extends AbstractPlayerCommand {

  private final SpawnManager spawnManager;
  private final SpawnsConfig config;
  private final WarmupManager warmupManager;

  public SpawnCommand(@NotNull SpawnManager spawnManager, @NotNull SpawnsConfig config,
            @NotNull WarmupManager warmupManager) {
    super("spawn", "Teleport to spawn");
    this.spawnManager = spawnManager;
    this.config = config;
    this.warmupManager = warmupManager;
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
      ctx.sendMessage(CommandUtil.error("You don't have permission to use spawn."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    String spawnName = parts.length > 1 ? parts[1].toLowerCase() : null;

    Spawn spawn;
    if (spawnName != null) {
      spawn = spawnManager.getSpawn(spawnName);
      if (spawn == null) {
        ctx.sendMessage(CommandUtil.error("Spawn '" + spawnName + "' not found."));
        return;
      }
      if (!spawnManager.canAccess(uuid, spawn)) {
        ctx.sendMessage(CommandUtil.error("You don't have permission to use this spawn."));
        return;
      }
    } else if (config.isPerWorldSpawns()) {
      spawn = spawnManager.getSpawnForWorld(currentWorld.getName());
      if (spawn == null) {
        spawn = spawnManager.getSpawnForPlayer(uuid);
      }
    } else {
      spawn = spawnManager.getSpawnForPlayer(uuid);
    }

    if (spawn == null) {
      ctx.sendMessage(CommandUtil.error("No spawn point has been set."));
      return;
    }

    if (warmupManager.isOnCooldown(uuid, "spawns", "spawn")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "spawns", "spawn");
      ctx.sendMessage(CommandUtil.error("On cooldown. " + remaining + "s remaining."));
      return;
    }

    Location destination = Location.fromSpawn(spawn);

    WarmupTask task = warmupManager.startWarmup(uuid, "spawns", "spawn", () -> {
      executeTeleport(store, ref, destination);
      ctx.sendMessage(CommandUtil.success("Teleported to spawn!"));
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
