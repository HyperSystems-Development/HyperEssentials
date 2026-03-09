package com.hyperessentials.module.warps.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Location;
import com.hyperessentials.data.Warp;
import com.hyperessentials.integration.FactionTerritoryChecker;
import com.hyperessentials.integration.HyperFactionsIntegration;
import com.hyperessentials.module.teleport.BackManager;
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
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

/**
 * /warp &lt;name&gt; - Teleport to a server warp.
 */
public class WarpCommand extends AbstractPlayerCommand {

  private final WarpManager warpManager;
  private final WarmupManager warmupManager;
  private final BackManager backManager;

  public WarpCommand(@NotNull WarpManager warpManager, @NotNull WarmupManager warmupManager,
                     @Nullable BackManager backManager) {
    super("warp", "Teleport to a server warp");
    this.warpManager = warpManager;
    this.warmupManager = warmupManager;
    this.backManager = backManager;
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

    // Zone flag check (warp destination)
    if (!CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS_WARP)
        && !CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS)) {
      FactionTerritoryChecker.Result zoneResult = FactionTerritoryChecker.checkZoneFlag(
          warp.world(), warp.x(), warp.z(), HyperFactionsIntegration.FLAG_WARPS);
      if (zoneResult != FactionTerritoryChecker.Result.ALLOWED) {
        ctx.sendMessage(CommandUtil.error("You cannot warp to this location — zone restricted."));
        return;
      }
    }

    // Check cooldown
    if (warmupManager.isOnCooldown(uuid, "warps", "warp")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "warps", "warp");
      ctx.sendMessage(CommandUtil.error("On cooldown. " + remaining + "s remaining."));
      return;
    }

    // Save back location before teleport
    saveBackLocation(uuid, store, ref, currentWorld);

    Location destination = Location.fromWarp(warp);

    WarmupTask task = warmupManager.startWarmup(uuid, "warps", "warp", () -> {
      executeTeleport(ref, destination, () -> {
        ctx.sendMessage(CommandUtil.success("Teleported to warp '" + warpName + "'!"));
      });
    });

    if (task != null) {
      ctx.sendMessage(CommandUtil.info("Teleporting in " + task.warmupSeconds() + "s... Don't move!"));
    }
  }

  private void saveBackLocation(@NotNull UUID uuid, @NotNull Store<EntityStore> store,
                                 @NotNull Ref<EntityStore> ref, @NotNull World currentWorld) {
    if (backManager == null) return;
    try {
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      if (transform != null) {
        Vector3d pos = transform.getPosition();
        Location currentLoc = new Location(currentWorld.getName(),
            currentWorld.getWorldConfig().getUuid().toString(),
            pos.getX(), pos.getY(), pos.getZ(), 0, 0);
        backManager.onTeleport(uuid, currentLoc, "warp");
      }
    } catch (Exception ignored) {}
  }

  private void executeTeleport(Ref<EntityStore> ref, Location dest, Runnable onComplete) {
    World targetWorld = Universe.get().getWorld(UUID.fromString(dest.worldUuid()));
    if (targetWorld == null) {
      return;
    }
    targetWorld.execute(() -> {
      if (!ref.isValid()) {
        return;
      }
      Store<EntityStore> store = ref.getStore();
      Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
      Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
      Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
      store.addComponent(ref, Teleport.getComponentType(), teleport);
      if (onComplete != null) {
        onComplete.run();
      }
    });
  }
}
