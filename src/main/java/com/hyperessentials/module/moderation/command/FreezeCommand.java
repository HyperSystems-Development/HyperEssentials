package com.hyperessentials.module.moderation.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.data.Location;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /freeze <player> - Toggle freeze on a player.
 */
public class FreezeCommand extends AbstractPlayerCommand {

  private final ModerationModule module;

  public FreezeCommand(@NotNull ModerationModule module) {
    super("freeze", "Toggle freeze on a player");
    this.module = module;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.MODERATION_FREEZE)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.FREEZE_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.FREEZE_USAGE));
      return;
    }

    String targetName = parts[1];
    PlayerRef target = CommandUtil.findOnlinePlayer(targetName);
    if (target == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.PLAYER_NOT_ONLINE, targetName));
      return;
    }

    if (CommandUtil.hasPermission(target.getUuid(), Permissions.BYPASS_FREEZE)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.FREEZE_CANNOT_FREEZE));
      return;
    }

    if (module.getFreezeManager().isFrozen(target.getUuid())) {
      module.getFreezeManager().unfreeze(target.getUuid());
      target.sendMessage(HEMessageUtil.success(target, CommandKeys.Moderation.FREEZE_YOU_UNFROZEN));
      ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Moderation.FREEZE_UNFROZEN, target.getUsername()));
    } else {
      Transform transform = target.getTransform();
      Vector3d pos = transform.getPosition();
      Location loc = new Location(
        world.getName(),
        world.getWorldConfig().getUuid().toString(),
        pos.getX(),
        pos.getY(),
        pos.getZ(),
        0, 0
      );
      module.getFreezeManager().freeze(target.getUuid(), loc);

      target.sendMessage(HEMessageUtil.error(target, CommandKeys.Moderation.FREEZE_YOU_FROZEN));
      ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Moderation.FREEZE_FROZEN, target.getUsername()));
    }
  }
}
