package com.hyperessentials.module.spawns.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.modules.SpawnsConfig;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * /setspawn [global] - Set spawn for current world. Use 'global' to mark as global spawn.
 */
public class SetSpawnCommand extends AbstractPlayerCommand {

  private final SpawnManager spawnManager;

  public SetSpawnCommand(@NotNull SpawnManager spawnManager, @NotNull SpawnsConfig config) {
    super("setspawn", "Set spawn for current world");
    this.spawnManager = spawnManager;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.SPAWN_SET)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to set spawns."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    boolean makeGlobal = parts.length > 1 && parts[1].equalsIgnoreCase("global");

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ctx.sendMessage(CommandUtil.error("Could not get your position."));
      return;
    }

    Vector3d pos = transform.getPosition();
    Vector3f rot = transform.getRotation();

    String worldUuid = currentWorld.getWorldConfig().getUuid().toString();
    Spawn existing = spawnManager.getSpawnForWorld(worldUuid);
    boolean isNew = existing == null;

    Spawn spawn;
    if (isNew) {
      spawn = Spawn.create(worldUuid, currentWorld.getName(),
          pos.getX(), pos.getY(), pos.getZ(),
          rot.getY(), rot.getX(),
          uuid.toString());
      if (spawnManager.getSpawnCount() == 0 || makeGlobal) {
        spawn = spawn.withGlobal(true);
      }
    } else {
      spawn = existing.withLocation(pos.getX(), pos.getY(), pos.getZ(), rot.getY(), rot.getX());
      if (makeGlobal) {
        spawn = spawn.withGlobal(true);
      }
    }

    spawnManager.setSpawn(spawn);

    String action = isNew ? "set" : "updated";
    ctx.sendMessage(CommandUtil.success("Spawn " + action + " for world '" + currentWorld.getName() + "'!"));
    ctx.sendMessage(CommandUtil.info(String.format("Location: %.0f, %.0f, %.0f",
        pos.getX(), pos.getY(), pos.getZ())));
    if (spawn.isGlobal()) {
      ctx.sendMessage(CommandUtil.info("(Set as global spawn)"));
    }
  }
}
