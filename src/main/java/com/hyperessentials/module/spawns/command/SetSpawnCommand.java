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
 * /setspawn [name] [--default] - Create or update a spawn at your location.
 */
public class SetSpawnCommand extends AbstractPlayerCommand {

  private final SpawnManager spawnManager;
  private final SpawnsConfig config;

  public SetSpawnCommand(@NotNull SpawnManager spawnManager, @NotNull SpawnsConfig config) {
    super("setspawn", "Create a spawn at your location");
    this.spawnManager = spawnManager;
    this.config = config;
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
      ctx.sendMessage(CommandUtil.error("You don't have permission to create spawns."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    String spawnName = parts.length > 1 ? parts[1].toLowerCase() : config.getDefaultSpawnName();

    boolean setAsDefault = false;
    for (String part : parts) {
      if (part.equalsIgnoreCase("--default") || part.equalsIgnoreCase("-d")) {
        setAsDefault = true;
        break;
      }
    }

    if (spawnName.equals("--default") || spawnName.equals("-d")) {
      spawnName = config.getDefaultSpawnName();
      setAsDefault = true;
    }

    if (spawnName.length() < 1 || spawnName.length() > 32) {
      ctx.sendMessage(CommandUtil.error("Spawn name must be 1-32 characters."));
      return;
    }

    if (!spawnName.matches("[a-z0-9_-]+")) {
      ctx.sendMessage(CommandUtil.error("Spawn name can only contain letters, numbers, underscore, and dash."));
      return;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ctx.sendMessage(CommandUtil.error("Could not get your position."));
      return;
    }

    Vector3d pos = transform.getPosition();
    Vector3f rot = transform.getRotation();

    boolean isUpdate = spawnManager.spawnExists(spawnName);

    Spawn spawn;
    if (isUpdate) {
      Spawn existing = spawnManager.getSpawn(spawnName);
      spawn = existing.withLocation(
        currentWorld.getName(),
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX()
      );
      if (setAsDefault) {
        spawn = spawn.withDefault(true);
      }
    } else {
      spawn = Spawn.create(
        spawnName,
        currentWorld.getName(),
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX(),
        uuid.toString()
      );
      if (spawnManager.getSpawnCount() == 0 || setAsDefault) {
        spawn = spawn.withDefault(true);
      }
    }

    spawnManager.setSpawn(spawn);

    ctx.sendMessage(CommandUtil.success("Spawn '" + spawnName + "' has been set!"));
    ctx.sendMessage(CommandUtil.info(String.format("Location: %.0f, %.0f, %.0f in %s",
      pos.getX(), pos.getY(), pos.getZ(), currentWorld.getName())));
    if (spawn.isDefault()) {
      ctx.sendMessage(CommandUtil.info("(Set as default spawn)"));
    }
  }
}
