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
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.HEMessageUtil;
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Warp.NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      Collection<Warp> warps = warpManager.getAccessibleWarps(uuid);
      if (warps.isEmpty()) {
        ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Warp.NO_WARPS, HEMessageUtil.COLOR_YELLOW));
      } else {
        ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Warp.AVAILABLE_WARPS, HEMessageUtil.COLOR_YELLOW));
        StringBuilder sb = new StringBuilder();
        for (Warp warp : warps) {
          if (!sb.isEmpty()) sb.append(", ");
          sb.append(warp.name());
        }
        ctx.sendMessage(HEMessageUtil.text(sb.toString(), HEMessageUtil.COLOR_GRAY));
        ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Warp.USE_WARP_HINT, HEMessageUtil.COLOR_GRAY));
      }
      return;
    }

    String warpName = parts[1].toLowerCase();

    Warp warp = warpManager.getWarp(warpName);
    if (warp == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Warp.NOT_FOUND, warpName));
      return;
    }

    if (!warpManager.canAccess(uuid, warp)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Warp.ACCESS_DENIED));
      return;
    }

    // Zone flag check (warp destination)
    if (!CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS_WARP)
        && !CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS)) {
      FactionTerritoryChecker.Result zoneResult = FactionTerritoryChecker.checkZoneFlag(
          warp.world(), warp.x(), warp.z(), HyperFactionsIntegration.FLAG_WARPS);
      if (zoneResult != FactionTerritoryChecker.Result.ALLOWED) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Warp.ZONE_RESTRICTED));
        return;
      }
    }

    // Check cooldown
    if (warmupManager.isOnCooldown(uuid, "warps", "warp")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "warps", "warp");
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.ON_COOLDOWN, remaining));
      return;
    }

    // Save back location before teleport
    saveBackLocation(uuid, store, ref, currentWorld);

    Location destination = Location.fromWarp(warp);

    WarmupTask task = warmupManager.startWarmup(uuid, "warps", "warp", () -> {
      executeTeleport(ref, destination, () -> {
        ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Warp.TELEPORTED, warpName));
      });
    });

    if (task != null) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Common.WARMUP_STARTING, HEMessageUtil.COLOR_YELLOW, task.warmupSeconds()));
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
    } catch (Exception e) {
      ErrorHandler.report("[Warps] Failed to save back location", e);
    }
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
