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
      ctx.sendMessage(CommandUtil.error("You don't have permission to use homes."));
      return;
    }

    // Parse home name
    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    boolean hasExplicitName = parts.length >= 2;

    // Check for faction home shortcut
    if (hasExplicitName && (parts[1].equalsIgnoreCase("factionhome") || parts[1].equalsIgnoreCase("faction"))) {
      handleFactionHome(ctx, uuid, ref, store, currentWorld);
      return;
    }

    // No args — resolve default home
    Home home;
    if (!hasExplicitName) {
      Collection<Home> homes = homeManager.getHomes(uuid);
      if (homes.isEmpty()) {
        ctx.sendMessage(CommandUtil.info("You don't have any homes. Use /sethome to create one."));
        return;
      }

      home = homeManager.resolveDefaultHome(uuid);
      if (home == null) {
        ctx.sendMessage(CommandUtil.error("No default home set. Specify a name: /home <name>"));
        StringBuilder sb = new StringBuilder();
        for (Home h : homes) {
          if (!sb.isEmpty()) sb.append(", ");
          sb.append(h.name());
        }
        ctx.sendMessage(CommandUtil.info("Your homes: " + sb));
        return;
      }
    } else {
      String homeName = parts[1];
      home = homeManager.getHome(uuid, homeName);
      if (home == null) {
        ctx.sendMessage(CommandUtil.error("Home '" + homeName + "' not found."));
        Collection<Home> homes = homeManager.getHomes(uuid);
        if (!homes.isEmpty()) {
          StringBuilder sb = new StringBuilder();
          for (Home h : homes) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(h.name());
          }
          ctx.sendMessage(CommandUtil.info("Your homes: " + sb));
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
      ctx.sendMessage(CommandUtil.error("On cooldown. " + remaining + "s remaining."));
      return;
    }

    // Save back location before teleport
    saveBackLocation(uuid, store, ref, currentWorld, "home");

    Location destination = Location.fromHome(home);

    WarmupTask task = warmupManager.startWarmup(uuid, "homes", "home", () -> {
      executeTeleport(ref, destination, () -> {
        ctx.sendMessage(CommandUtil.success("Teleported to home '" + home.name() + "'!"));
      });
    });

    if (task != null) {
      ctx.sendMessage(CommandUtil.info(
          "Teleporting in " + task.warmupSeconds() + "s... Don't move!"));
    }
  }

  private void handleFactionHome(@NotNull CommandContext ctx, @NotNull UUID uuid,
                                  @NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                                  @NotNull World currentWorld) {
    if (!HyperFactionsIntegration.isAvailable() || !HyperFactionsIntegration.hasFactionHome(uuid)) {
      ctx.sendMessage(CommandUtil.error("Your faction does not have a home set."));
      return;
    }

    String world = HyperFactionsIntegration.getFactionHomeWorld(uuid);
    double[] coords = HyperFactionsIntegration.getFactionHomeCoords(uuid);
    if (world == null || coords == null) {
      ctx.sendMessage(CommandUtil.error("Could not resolve faction home location."));
      return;
    }

    // Zone flag check
    if (!CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS_HOME)
        && !CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS)) {
      FactionTerritoryChecker.Result zoneResult = FactionTerritoryChecker.checkZoneFlag(
          world, coords[0], coords[2], HyperFactionsIntegration.FLAG_HOMES);
      if (zoneResult != FactionTerritoryChecker.Result.ALLOWED) {
        ctx.sendMessage(CommandUtil.error("You cannot teleport to your faction home — zone restricted."));
        return;
      }
    }

    // Check cooldown (uses separate factionhome module)
    if (warmupManager.isOnCooldown(uuid, "factionhome", "factionhome")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "factionhome", "factionhome");
      ctx.sendMessage(CommandUtil.error("On cooldown. " + remaining + "s remaining."));
      return;
    }

    // Resolve world UUID
    World resolvedWorld = Universe.get().getWorld(world);
    if (resolvedWorld == null) {
      ctx.sendMessage(CommandUtil.error("Faction home world not found."));
      return;
    }
    String worldUuid = resolvedWorld.getWorldConfig().getUuid().toString();

    // Save back location
    saveBackLocation(uuid, store, ref, currentWorld, "factionhome");

    Location destination = new Location(world, worldUuid, coords[0], coords[1], coords[2],
        (float) coords[3], (float) coords[4]);

    WarmupTask task = warmupManager.startWarmup(uuid, "factionhome", "factionhome", () -> {
      executeTeleport(ref, destination, () -> {
        ctx.sendMessage(CommandUtil.success("Teleported to faction home!"));
      });
    });

    if (task != null) {
      ctx.sendMessage(CommandUtil.info(
          "Teleporting in " + task.warmupSeconds() + "s... Don't move!"));
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
