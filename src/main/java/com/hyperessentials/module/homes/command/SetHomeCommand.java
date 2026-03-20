package com.hyperessentials.module.homes.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Home;
import com.hyperessentials.integration.FactionTerritoryChecker;
import com.hyperessentials.module.homes.HomeManager;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Home.SET_NO_PERMISSION));
      return;
    }

    // Parse home name (default: "home")
    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    String homeName = parts.length >= 2 ? parts[1] : "home";

    // Validate name
    if (!NAME_PATTERN.matcher(homeName).matches()) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Home.SET_INVALID_NAME));
      return;
    }

    // Get player position
    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.CANNOT_GET_POSITION));
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Home.SET_LIMIT_REACHED, limit));
      return;
    }

    Home home = Home.create(homeName, currentWorld.getName(),
        currentWorld.getWorldConfig().getUuid().toString(),
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX());

    homeManager.setHome(uuid, home);

    String key = isNew ? CommandKeys.Home.SET_SUCCESS : CommandKeys.Home.SET_UPDATED;
    ctx.sendMessage(HEMessageUtil.success(playerRef, key, homeName));
    ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Home.SET_LOCATION, HEMessageUtil.COLOR_YELLOW,
        String.format("%.0f", pos.getX()), String.format("%.0f", pos.getY()),
        String.format("%.0f", pos.getZ()), currentWorld.getName()));
  }
}
