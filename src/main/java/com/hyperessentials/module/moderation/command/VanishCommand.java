package com.hyperessentials.module.moderation.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /vanish - Toggle vanish mode (self only).
 */
public class VanishCommand extends AbstractPlayerCommand {

  private final ModerationModule module;

  public VanishCommand(@NotNull ModerationModule module) {
    super("vanish", "Toggle vanish mode");
    this.module = module;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.MODERATION_VANISH)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to vanish."));
      return;
    }

    boolean nowVanished = module.getVanishManager().toggleVanish(playerRef.getUuid(), playerRef);

    if (nowVanished) {
      ctx.sendMessage(CommandUtil.success(ConfigManager.get().vanish().getVanishEnableMessage()));
    } else {
      ctx.sendMessage(CommandUtil.success(ConfigManager.get().vanish().getVanishDisableMessage()));
    }
  }
}
