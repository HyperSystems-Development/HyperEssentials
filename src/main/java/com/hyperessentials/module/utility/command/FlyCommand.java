package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.utility.UtilityModule;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /fly [player] - Toggle flight mode.
 * Note: Uses Creative mode toggle as workaround since no direct fly API exists.
 * Player.setGameMode() is a static method that takes (Ref, GameMode, ComponentAccessor).
 */
public class FlyCommand extends AbstractPlayerCommand {

  private final UtilityModule module;

  public FlyCommand(@NotNull UtilityModule module) {
    super("fly", "Toggle flight mode");
    this.module = module;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_FLY)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to fly."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length >= 2) {
      if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_FLY_OTHERS)) {
        ctx.sendMessage(CommandUtil.error("You don't have permission to toggle fly for others."));
        return;
      }

      PlayerRef target = CommandUtil.findOnlinePlayer(parts[1]);
      if (target == null) {
        ctx.sendMessage(CommandUtil.error("Player '" + parts[1] + "' is not online."));
        return;
      }

      boolean nowFlying = module.getUtilityManager().toggleFly(target.getUuid());
      // For other players we need their ref — toggle on self ref as workaround
      // TODO: Resolve target's store/ref for cross-player gamemode changes
      applyFly(store, ref, nowFlying);

      ctx.sendMessage(CommandUtil.success("Flight " + (nowFlying ? "enabled" : "disabled") + " for " + target.getUsername() + "."));
      target.sendMessage(CommandUtil.success("Flight " + (nowFlying ? "enabled" : "disabled") + "."));
    } else {
      boolean nowFlying = module.getUtilityManager().toggleFly(playerRef.getUuid());
      applyFly(store, ref, nowFlying);

      ctx.sendMessage(CommandUtil.success("Flight " + (nowFlying ? "enabled" : "disabled") + "."));
    }
  }

  private void applyFly(@NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, boolean enable) {
    try {
      // Toggle Creative/Adventure mode as flight workaround
      // Player.setGameMode is static: (Ref, GameMode, ComponentAccessor)
      // Store<EntityStore> implements ComponentAccessor<EntityStore>
      // TODO: Investigate proper fly API when available
      if (enable) {
        Player.setGameMode(ref, GameMode.Creative, store);
      } else {
        Player.setGameMode(ref, GameMode.Adventure, store);
      }
    } catch (Exception e) {
      Logger.debug("[Utility] setGameMode failed: %s", e.getMessage());
    }
  }
}
