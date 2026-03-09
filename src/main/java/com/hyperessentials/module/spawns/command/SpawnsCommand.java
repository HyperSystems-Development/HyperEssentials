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

import java.util.Collection;
import java.util.UUID;

/**
 * /spawns - List all world spawns.
 */
public class SpawnsCommand extends AbstractPlayerCommand {

  private final SpawnManager spawnManager;

  public SpawnsCommand(@NotNull SpawnManager spawnManager) {
    super("spawns", "List server spawns");
    this.spawnManager = spawnManager;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.SPAWN_LIST)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to list spawns."));
      return;
    }

    Collection<Spawn> spawns = spawnManager.getAllSpawns();

    if (spawns.isEmpty()) {
      ctx.sendMessage(CommandUtil.info("No spawns have been set."));
      ctx.sendMessage(CommandUtil.msg("Use /setspawn to create one.", CommandUtil.COLOR_GRAY));
      return;
    }

    ctx.sendMessage(CommandUtil.msg("--- Server Spawns ---", CommandUtil.COLOR_GOLD));

    for (Spawn spawn : spawns) {
      StringBuilder sb = new StringBuilder();
      sb.append(spawn.worldName());
      if (spawn.isGlobal()) {
        sb.append(" (global)");
      }
      sb.append(String.format(" (%.0f, %.0f, %.0f)", spawn.x(), spawn.y(), spawn.z()));

      ctx.sendMessage(CommandUtil.msg("  " + sb, CommandUtil.COLOR_GRAY));
    }

    ctx.sendMessage(CommandUtil.msg("Use /spawn [world] to teleport.", CommandUtil.COLOR_GRAY));
  }
}
