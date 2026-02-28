package com.hyperessentials.module.moderation.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.data.Location;
import com.hyperessentials.module.moderation.ModerationModule;
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
      ctx.sendMessage(CommandUtil.error("You don't have permission to freeze players."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /freeze <player>"));
      return;
    }

    String targetName = parts[1];
    PlayerRef target = CommandUtil.findOnlinePlayer(targetName);
    if (target == null) {
      ctx.sendMessage(CommandUtil.error("Player '" + targetName + "' is not online."));
      return;
    }

    if (CommandUtil.hasPermission(target.getUuid(), Permissions.BYPASS_FREEZE)) {
      ctx.sendMessage(CommandUtil.error("That player cannot be frozen."));
      return;
    }

    if (module.getFreezeManager().isFrozen(target.getUuid())) {
      module.getFreezeManager().unfreeze(target.getUuid());
      target.sendMessage(CommandUtil.success("You have been unfrozen."));
      ctx.sendMessage(CommandUtil.success("Unfroze " + target.getUsername() + "."));
    } else {
      Transform transform = target.getTransform();
      Vector3d pos = transform.getPosition();
      Location loc = new Location(
        world.getName(),
        pos.getX(),
        pos.getY(),
        pos.getZ(),
        0, 0
      );
      module.getFreezeManager().freeze(target.getUuid(), loc);

      String freezeMsg = ConfigManager.get().moderation().getFreezeMessage();
      target.sendMessage(CommandUtil.error(freezeMsg));
      ctx.sendMessage(CommandUtil.success("Froze " + target.getUsername() + "."));
    }
  }
}
