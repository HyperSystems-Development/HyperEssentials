package com.hyperessentials.module.warps.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.modules.WarpsConfig;
import com.hyperessentials.data.Warp;
import com.hyperessentials.module.warps.WarpManager;
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

/**
 * /setwarp &lt;name&gt; [category] - Create or update a warp at your location.
 */
public class SetWarpCommand extends AbstractPlayerCommand {

  private final WarpManager warpManager;
  private final WarpsConfig config;

  public SetWarpCommand(@NotNull WarpManager warpManager, @NotNull WarpsConfig config) {
    super("setwarp", "Create a server warp at your location");
    this.warpManager = warpManager;
    this.config = config;
    addAliases("createwarp");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.WARP_SET)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Warp.SET_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Warp.SET_USAGE));
      return;
    }

    String warpName = parts[1].toLowerCase();
    String category = parts.length > 2 ? parts[2] : config.getDefaultCategory();

    if (warpName.length() < 1 || warpName.length() > 32) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Warp.SET_NAME_LENGTH));
      return;
    }

    if (!warpName.matches("[a-z0-9_-]+")) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Warp.SET_NAME_INVALID));
      return;
    }

    TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
    if (transform == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.CANNOT_GET_POSITION));
      return;
    }

    Vector3d pos = transform.getPosition();
    Vector3f rot = transform.getRotation();

    boolean isUpdate = warpManager.warpExists(warpName);

    Warp warp;
    if (isUpdate) {
      Warp existing = warpManager.getWarp(warpName);
      warp = existing.withLocation(
        currentWorld.getName(),
        currentWorld.getWorldConfig().getUuid().toString(),
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX()
      );
      if (parts.length > 2) {
        warp = warp.withCategory(category);
      }
    } else {
      warp = Warp.create(
        warpName,
        currentWorld.getName(),
        currentWorld.getWorldConfig().getUuid().toString(),
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX(),
        uuid.toString()
      );
      warp = warp.withCategory(category);
    }

    warpManager.setWarp(warp);

    ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Warp.SET_SUCCESS, warpName));
    ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Warp.SET_LOCATION, HEMessageUtil.COLOR_YELLOW,
        String.format("%.0f", pos.getX()), String.format("%.0f", pos.getY()),
        String.format("%.0f", pos.getZ()), currentWorld.getName()));
    ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Warp.SET_CATEGORY, HEMessageUtil.COLOR_YELLOW, category));
  }
}
