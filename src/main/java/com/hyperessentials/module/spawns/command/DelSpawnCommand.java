package com.hyperessentials.module.spawns.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
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
 * /delspawn &lt;name&gt; - Delete a spawn.
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

        if (parts.length < 2) {
            ctx.sendMessage(CommandUtil.error("Usage: /delspawn <name>"));
            return;
        }

        String spawnName = parts[1].toLowerCase();

        if (spawnManager.deleteSpawn(spawnName)) {
            ctx.sendMessage(CommandUtil.success("Spawn '" + spawnName + "' has been deleted."));
        } else {
            ctx.sendMessage(CommandUtil.error("Spawn '" + spawnName + "' not found."));
        }
    }
}
