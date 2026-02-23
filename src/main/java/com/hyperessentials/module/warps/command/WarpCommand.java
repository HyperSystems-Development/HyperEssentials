package com.hyperessentials.module.warps.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Location;
import com.hyperessentials.data.Warp;
import com.hyperessentials.module.warps.WarpManager;
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

import java.util.Collection;
import java.util.UUID;

/**
 * /warp &lt;name&gt; - Teleport to a server warp.
 */
public class WarpCommand extends AbstractPlayerCommand {

    private final WarpManager warpManager;
    private final WarmupManager warmupManager;

    public WarpCommand(@NotNull WarpManager warpManager, @NotNull WarmupManager warmupManager) {
        super("warp", "Teleport to a server warp");
        this.warpManager = warpManager;
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

        if (!CommandUtil.hasPermission(uuid, Permissions.WARP)) {
            ctx.sendMessage(CommandUtil.error("You don't have permission to use warps."));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

        if (parts.length < 2) {
            Collection<Warp> warps = warpManager.getAccessibleWarps(uuid);
            if (warps.isEmpty()) {
                ctx.sendMessage(CommandUtil.info("No warps available."));
            } else {
                ctx.sendMessage(CommandUtil.info("Available warps:"));
                StringBuilder sb = new StringBuilder();
                for (Warp warp : warps) {
                    if (!sb.isEmpty()) sb.append(", ");
                    sb.append(warp.name());
                }
                ctx.sendMessage(CommandUtil.msg(sb.toString(), CommandUtil.COLOR_GRAY));
                ctx.sendMessage(CommandUtil.msg("Use /warp <name> to teleport.", CommandUtil.COLOR_GRAY));
            }
            return;
        }

        String warpName = parts[1].toLowerCase();

        Warp warp = warpManager.getWarp(warpName);
        if (warp == null) {
            ctx.sendMessage(CommandUtil.error("Warp '" + warpName + "' not found."));
            return;
        }

        if (!warpManager.canAccess(uuid, warp)) {
            ctx.sendMessage(CommandUtil.error("You don't have permission to use this warp."));
            return;
        }

        // Check cooldown
        if (warmupManager.isOnCooldown(uuid, "warps", "warp")) {
            int remaining = warmupManager.getRemainingCooldown(uuid, "warps", "warp");
            ctx.sendMessage(CommandUtil.error("On cooldown. " + remaining + "s remaining."));
            return;
        }

        Location destination = Location.fromWarp(warp);

        WarmupTask task = warmupManager.startWarmup(uuid, "warps", "warp", () -> {
            executeTeleport(store, ref, destination);
            ctx.sendMessage(CommandUtil.success("Teleported to warp '" + warpName + "'!"));
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
