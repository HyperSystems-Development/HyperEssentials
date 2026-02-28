package com.hyperessentials.module.homes.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Home;
import com.hyperessentials.data.Location;
import com.hyperessentials.integration.FactionTerritoryChecker;
import com.hyperessentials.module.homes.HomeManager;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.module.warmup.WarmupTask;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

/**
 * /home [name] - Teleport to a home.
 */
public class HomeCommand extends AbstractPlayerCommand {

  private final HomeManager homeManager;
  private final WarmupManager warmupManager;

  public HomeCommand(@NotNull HomeManager homeManager, @NotNull WarmupManager warmupManager) {
    super("home", "Teleport to a home");
    this.homeManager = homeManager;
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

    if (!CommandUtil.hasPermission(uuid, Permissions.HOME_TELEPORT)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to use homes."));
      return;
    }

    // Parse home name (default: "home")
    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    String homeName = parts.length >= 2 ? parts[1] : "home";

    // No args and no homes — hint
    if (parts.length < 2) {
      Collection<Home> homes = homeManager.getHomes(uuid);
      if (homes.isEmpty()) {
        ctx.sendMessage(CommandUtil.info("You don't have any homes. Use /sethome to create one."));
        return;
      }
    }

    // Resolve home
    Home home = homeManager.getHome(uuid, homeName);
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

    Location destination = Location.fromHome(home);

    WarmupTask task = warmupManager.startWarmup(uuid, "homes", "home", () -> {
      executeTeleport(store, ref, destination);
      ctx.sendMessage(CommandUtil.success("Teleported to home '" + home.name() + "'!"));
    });

    if (task != null) {
      ctx.sendMessage(CommandUtil.info(
          "Teleporting in " + task.warmupSeconds() + "s... Don't move!"));
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
