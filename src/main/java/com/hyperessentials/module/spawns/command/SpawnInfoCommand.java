package com.hyperessentials.module.spawns.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Spawn.INFO_NO_PERMISSION));
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
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Spawn.INFO_NOT_FOUND, worldArg));
        return;
      }
    } else {
      String worldUuid = currentWorld.getWorldConfig().getUuid().toString();
      spawn = spawnManager.getSpawnForWorld(worldUuid);
      if (spawn == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Spawn.INFO_NO_SPAWN));
        return;
      }
    }

    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Spawn.INFO_HEADER, HEMessageUtil.COLOR_GOLD, spawn.worldName()));
    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Spawn.INFO_LOCATION, HEMessageUtil.COLOR_GRAY,
        String.format("%.1f", spawn.x()), String.format("%.1f", spawn.y()), String.format("%.1f", spawn.z())));
    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Spawn.INFO_GLOBAL, HEMessageUtil.COLOR_GRAY, spawn.isGlobal() ? "Yes" : "No"));
    if (spawn.createdBy() != null) {
      ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Spawn.INFO_CREATED_BY, HEMessageUtil.COLOR_GRAY, spawn.createdBy()));
    }
    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Spawn.INFO_CREATED, HEMessageUtil.COLOR_GRAY, TimeUtil.formatRelativeTime(spawn.createdAt())));
  }
}
