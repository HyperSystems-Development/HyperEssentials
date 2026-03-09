package com.hyperessentials.module.spawns.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.module.spawns.SpawnManager;
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
 * /delspawn [world] - Delete spawn for a world (default: current world).
 */
public class DelSpawnCommand extends AbstractPlayerCommand {

  private final SpawnManager spawnManager;

  public DelSpawnCommand(@NotNull SpawnManager spawnManager) {
    super("delspawn", "Delete a spawn");
    this.spawnManager = spawnManager;
    addAliases("deletespawn", "rmspawn", "removespawn");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.SPAWN_DELETE)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to delete spawns."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    String worldUuid;
    String worldName;
    if (parts.length >= 2) {
      String arg = parts[1];
      Spawn found = null;
      for (Spawn s : spawnManager.getAllSpawns()) {
        if (s.worldName().equalsIgnoreCase(arg)) {
          found = s;
          break;
        }
      }
      if (found == null) {
        ctx.sendMessage(CommandUtil.error("No spawn found for world '" + arg + "'."));
        return;
      }
      worldUuid = found.worldUuid();
      worldName = found.worldName();
    } else {
      worldUuid = currentWorld.getWorldConfig().getUuid().toString();
      worldName = currentWorld.getName();
    }

    if (spawnManager.deleteSpawn(worldUuid)) {
      ctx.sendMessage(CommandUtil.success("Spawn for world '" + worldName + "' has been deleted."));
    } else {
      ctx.sendMessage(CommandUtil.error("No spawn set for world '" + worldName + "'."));
    }
  }
}
