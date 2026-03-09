package com.hyperessentials.module.spawns.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hyperessentials.util.TimeUtil;
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
 * /spawninfo [world] - Display detailed information about a world's spawn.
 */
public class SpawnInfoCommand extends AbstractPlayerCommand {

  private final SpawnManager spawnManager;

  public SpawnInfoCommand(@NotNull SpawnManager spawnManager) {
    super("spawninfo", "Display spawn information");
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

    if (!CommandUtil.hasPermission(uuid, Permissions.SPAWN_INFO)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to view spawn info."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    Spawn spawn;
    if (parts.length >= 2) {
      String worldArg = parts[1];
      spawn = null;
      for (Spawn s : spawnManager.getAllSpawns()) {
        if (s.worldName().equalsIgnoreCase(worldArg)) {
          spawn = s;
          break;
        }
      }
      if (spawn == null) {
        ctx.sendMessage(CommandUtil.error("No spawn found for world '" + worldArg + "'."));
        return;
      }
    } else {
      String worldUuid = currentWorld.getWorldConfig().getUuid().toString();
      spawn = spawnManager.getSpawnForWorld(worldUuid);
      if (spawn == null) {
        ctx.sendMessage(CommandUtil.error("No spawn set for this world."));
        return;
      }
    }

    ctx.sendMessage(CommandUtil.msg("--- Spawn: " + spawn.worldName() + " ---", CommandUtil.COLOR_GOLD));
    ctx.sendMessage(CommandUtil.msg(String.format("Location: %.1f, %.1f, %.1f",
      spawn.x(), spawn.y(), spawn.z()), CommandUtil.COLOR_GRAY));
    ctx.sendMessage(CommandUtil.msg("Global: " + (spawn.isGlobal() ? "Yes" : "No"), CommandUtil.COLOR_GRAY));
    if (spawn.createdBy() != null) {
      ctx.sendMessage(CommandUtil.msg("Created by: " + spawn.createdBy(), CommandUtil.COLOR_GRAY));
    }
    ctx.sendMessage(CommandUtil.msg("Created: " + TimeUtil.formatRelativeTime(spawn.createdAt()), CommandUtil.COLOR_GRAY));
  }
}
