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
 * /spawninfo &lt;name&gt; - Display detailed information about a spawn.
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

        if (parts.length < 2) {
            ctx.sendMessage(CommandUtil.error("Usage: /spawninfo <name>"));
            return;
        }

        String spawnName = parts[1].toLowerCase();

        Spawn spawn = spawnManager.getSpawn(spawnName);
        if (spawn == null) {
            ctx.sendMessage(CommandUtil.error("Spawn '" + spawnName + "' not found."));
            return;
        }

        ctx.sendMessage(CommandUtil.msg("--- Spawn: " + spawn.name() + " ---", CommandUtil.COLOR_GOLD));
        ctx.sendMessage(CommandUtil.msg("World: " + spawn.world(), CommandUtil.COLOR_GRAY));
        ctx.sendMessage(CommandUtil.msg(String.format("Location: %.1f, %.1f, %.1f",
            spawn.x(), spawn.y(), spawn.z()), CommandUtil.COLOR_GRAY));
        ctx.sendMessage(CommandUtil.msg("Default: " + (spawn.isDefault() ? "Yes" : "No"), CommandUtil.COLOR_GRAY));

        if (spawn.permission() != null && !spawn.permission().isEmpty()) {
            ctx.sendMessage(CommandUtil.msg("Permission: " + spawn.permission(), CommandUtil.COLOR_GRAY));
            boolean hasAccess = spawnManager.canAccess(uuid, spawn);
            ctx.sendMessage(CommandUtil.msg("You have access: " + (hasAccess ? "Yes" : "No"), CommandUtil.COLOR_GRAY));
        }

        if (spawn.groupPermission() != null && !spawn.groupPermission().isEmpty()) {
            ctx.sendMessage(CommandUtil.msg("Group Permission: " + spawn.groupPermission(), CommandUtil.COLOR_GRAY));
        }

        ctx.sendMessage(CommandUtil.msg("Created: " + TimeUtil.formatRelativeTime(spawn.createdAt()), CommandUtil.COLOR_GRAY));
    }
}
