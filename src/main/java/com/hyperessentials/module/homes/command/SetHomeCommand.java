package com.hyperessentials.module.homes.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Home;
import com.hyperessentials.integration.FactionTerritoryChecker;
import com.hyperessentials.module.homes.HomeManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * /sethome [name] - Set a home at your current location.
 */
public class SetHomeCommand extends AbstractPlayerCommand {

  private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,32}$");

  private final HomeManager homeManager;

  public SetHomeCommand(@NotNull HomeManager homeManager) {
    super("sethome", "Set a home at your current location");
    this.homeManager = homeManager;
    addAliases("createhome");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef playerRef,
                          @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.HOME_SET)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to set homes."));
      return;
    }

    // Parse home name (default: "home")
    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    String homeName = parts.length >= 2 ? parts[1] : "home";

    // Validate name
    if (!NAME_PATTERN.matcher(homeName).matches()) {
      ctx.sendMessage(CommandUtil.error(
          "Home name must be 1-32 characters (letters, numbers, _ and - only)."));
      return;
    }

    // Get player position
    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ctx.sendMessage(CommandUtil.error("Could not get your position."));
      return;
    }

    Vector3d pos = transform.getPosition();
    Vector3f rot = transform.getRotation();

    // Faction territory check (unless bypassed)
    if (!CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS_SETHOME)
        && !CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS)) {
      FactionTerritoryChecker.Result result = FactionTerritoryChecker.canUseHome(
          uuid, currentWorld.getName(), pos.getX(), pos.getZ());
      if (result != FactionTerritoryChecker.Result.ALLOWED) {
        ctx.sendMessage(CommandUtil.error(result.getDenialMessage()));
        return;
      }
    }

    // Check limit (overwriting existing home is always allowed)
    boolean isNew = homeManager.getHome(uuid, homeName) == null;
    if (isNew && homeManager.isAtLimit(uuid)) {
      int limit = homeManager.getHomeLimit(uuid);
      ctx.sendMessage(CommandUtil.error("You have reached your home limit (" + limit + ")."));
      return;
    }

    Home home = Home.create(homeName, currentWorld.getName(),
        currentWorld.getWorldConfig().getUuid().toString(),
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX());

    homeManager.setHome(uuid, home);

    String action = isNew ? "set" : "updated";
    ctx.sendMessage(CommandUtil.success("Home '" + homeName + "' has been " + action + "!"));
    ctx.sendMessage(CommandUtil.info(String.format("Location: %.0f, %.0f, %.0f in %s",
        pos.getX(), pos.getY(), pos.getZ(), currentWorld.getName())));
  }
}
