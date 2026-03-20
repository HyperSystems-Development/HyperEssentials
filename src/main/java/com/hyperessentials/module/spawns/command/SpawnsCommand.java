package com.hyperessentials.module.spawns.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Spawn;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Spawn.LIST_NO_PERMISSION));
      return;
    }

    Collection<Spawn> spawns = spawnManager.getAllSpawns();

    if (spawns.isEmpty()) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Spawn.LIST_EMPTY, HEMessageUtil.COLOR_YELLOW));
      ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Spawn.LIST_HINT, HEMessageUtil.COLOR_GRAY));
      return;
    }

    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Spawn.LIST_HEADER, HEMessageUtil.COLOR_GOLD));

    for (Spawn spawn : spawns) {
      StringBuilder sb = new StringBuilder();
      sb.append(spawn.worldName());
      if (spawn.isGlobal()) {
        sb.append(" (global)");
      }
      sb.append(String.format(" (%.0f, %.0f, %.0f)", spawn.x(), spawn.y(), spawn.z()));

      ctx.sendMessage(HEMessageUtil.text("  " + sb, HEMessageUtil.COLOR_GRAY));
    }

    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Spawn.LIST_USE_HINT, HEMessageUtil.COLOR_GRAY));
  }
}
