package com.hyperessentials.module.homes.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Home;
import com.hyperessentials.data.Location;
import com.hyperessentials.integration.FactionTerritoryChecker;
import com.hyperessentials.integration.HyperFactionsIntegration;
import com.hyperessentials.module.homes.HomeManager;
import com.hyperessentials.module.teleport.BackManager;
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
 * /home [name] - Teleport to a home.
 * Supports "factionhome" / "faction" as special home names.
 */
public class HomeCommand extends AbstractPlayerCommand {

  private final HomeManager homeManager;
  private final WarmupManager warmupManager;
  private final BackManager backManager;

  public HomeCommand(@NotNull HomeManager homeManager, @NotNull WarmupManager warmupManager,
                     @Nullable BackManager backManager) {
    super("home", "Teleport to a home");
    this.homeManager = homeManager;
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

    if (!CommandUtil.hasPermission(uuid, Permissions.HOME_TELEPORT)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Home.NO_PERMISSION));
      return;
    }

    // Parse home name
    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    boolean hasExplicitName = parts.length >= 2;

    // Check for faction home shortcut
    if (hasExplicitName && (parts[1].equalsIgnoreCase("factionhome") || parts[1].equalsIgnoreCase("faction"))) {
      handleFactionHome(ctx, uuid, playerRef, ref, store, currentWorld);
      return;
    }

    // No args — resolve default home
    Home home;
    if (!hasExplicitName) {
      Collection<Home> homes = homeManager.getHomes(uuid);
      if (homes.isEmpty()) {
        ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Home.NO_HOMES, HEMessageUtil.COLOR_YELLOW));
        return;
      }

      home = homeManager.resolveDefaultHome(uuid);
      if (home == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Home.NO_DEFAULT));
        StringBuilder sb = new StringBuilder();
        for (Home h : homes) {
          if (!sb.isEmpty()) sb.append(", ");
          sb.append(h.name());
        }
        ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Home.YOUR_HOMES, HEMessageUtil.COLOR_YELLOW, sb.toString()));
        return;
      }
    } else {
      String homeName = parts[1];
      home = homeManager.getHome(uuid, homeName);
      if (home == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Home.NOT_FOUND, homeName));
        Collection<Home> homes = homeManager.getHomes(uuid);
        if (!homes.isEmpty()) {
          StringBuilder sb = new StringBuilder();
          for (Home h : homes) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(h.name());
          }
          ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Home.YOUR_HOMES, HEMessageUtil.COLOR_YELLOW, sb.toString()));
        }
        return;
      }
    }

    // Faction territory check on DESTINATION (unless bypassed)
    if (!CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS_HOME)
        && !CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS)) {
      FactionTerritoryChecker.Result result = FactionTerritoryChecker.canUseHome(
          uuid, home.world(), home.x(), home.z());
      if (result != FactionTerritoryChecker.Result.ALLOWED) {
        ctx.sendMessage(CommandUtil.error(result.getDenialMessage()));
        return;
      }
    }

    // Check cooldown
    if (warmupManager.isOnCooldown(uuid, "homes", "home")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "homes", "home");
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.ON_COOLDOWN, remaining));
      return;
    }

    // Save back location before teleport
    saveBackLocation(uuid, store, ref, currentWorld, "home");

    Location destination = Location.fromHome(home);

    WarmupTask task = warmupManager.startWarmup(uuid, "homes", "home", () -> {
      executeTeleport(ref, destination, () -> {
        ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Home.TELEPORTED, home.name()));
      });
    });

    if (task != null) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Common.WARMUP_STARTING, HEMessageUtil.COLOR_YELLOW, task.warmupSeconds()));
    }
  }

  private void handleFactionHome(@NotNull CommandContext ctx, @NotNull UUID uuid,
                                  @NotNull PlayerRef playerRef,
                                  @NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                                  @NotNull World currentWorld) {
    if (!HyperFactionsIntegration.isAvailable() || !HyperFactionsIntegration.hasFactionHome(uuid)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Home.FACTION_NO_HOME));
      return;
    }

    String world = HyperFactionsIntegration.getFactionHomeWorld(uuid);
    double[] coords = HyperFactionsIntegration.getFactionHomeCoords(uuid);
    if (world == null || coords == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Home.FACTION_RESOLVE_FAILED));
      return;
    }

    // Zone flag check
    if (!CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS_HOME)
        && !CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS)) {
      FactionTerritoryChecker.Result zoneResult = FactionTerritoryChecker.checkZoneFlag(
          world, coords[0], coords[2], HyperFactionsIntegration.FLAG_HOMES);
      if (zoneResult != FactionTerritoryChecker.Result.ALLOWED) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Home.FACTION_ZONE_RESTRICTED));
        return;
      }
    }

    // Check cooldown (uses separate factionhome module)
    if (warmupManager.isOnCooldown(uuid, "factionhome", "factionhome")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "factionhome", "factionhome");
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.ON_COOLDOWN, remaining));
      return;
    }

    // Resolve world UUID
    World resolvedWorld = Universe.get().getWorld(world);
    if (resolvedWorld == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Home.FACTION_WORLD_NOT_FOUND));
      return;
    }
    String worldUuid = resolvedWorld.getWorldConfig().getUuid().toString();

    // Save back location
    saveBackLocation(uuid, store, ref, currentWorld, "factionhome");

    Location destination = new Location(world, worldUuid, coords[0], coords[1], coords[2],
        (float) coords[3], (float) coords[4]);

    WarmupTask task = warmupManager.startWarmup(uuid, "factionhome", "factionhome", () -> {
      executeTeleport(ref, destination, () -> {
        ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Home.FACTION_TELEPORTED));
      });
    });

    if (task != null) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Common.WARMUP_STARTING, HEMessageUtil.COLOR_YELLOW, task.warmupSeconds()));
    }
  }

  private void saveBackLocation(@NotNull UUID uuid, @NotNull Store<EntityStore> store,
                                 @NotNull Ref<EntityStore> ref, @NotNull World currentWorld,
                                 @NotNull String source) {
    if (backManager == null) return;
    try {
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      if (transform != null) {
        Vector3d pos = transform.getPosition();
        Location currentLoc = new Location(currentWorld.getName(),
            currentWorld.getWorldConfig().getUuid().toString(),
            pos.getX(), pos.getY(), pos.getZ(), 0, 0);
        backManager.onTeleport(uuid, currentLoc, source);
      }
    } catch (Exception e) {
      ErrorHandler.report("[Homes] Failed to save back location", e);
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
