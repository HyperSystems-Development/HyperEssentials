package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.utility.UtilityModule;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /fly [player] - Toggle flight ability without changing gamemode.
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

      // Resolve target's store/ref for cross-player fly toggle
      Ref<EntityStore> targetRef = target.getReference();
      if (targetRef == null || !targetRef.isValid()) {
        ctx.sendMessage(CommandUtil.error("Player '" + parts[1] + "' is not in a world."));
        return;
      }
      Store<EntityStore> targetStore = targetRef.getStore();

      boolean nowFlying = module.getUtilityManager().toggleFly(target.getUuid());
      applyFly(targetStore, targetRef, target, nowFlying);

      ctx.sendMessage(CommandUtil.success("Flight " + (nowFlying ? "enabled" : "disabled") + " for " + target.getUsername() + "."));
      target.sendMessage(CommandUtil.success("Flight " + (nowFlying ? "enabled" : "disabled") + "."));
    } else {
      boolean nowFlying = module.getUtilityManager().toggleFly(playerRef.getUuid());
      applyFly(store, ref, playerRef, nowFlying);

      ctx.sendMessage(CommandUtil.success("Flight " + (nowFlying ? "enabled" : "disabled") + "."));
    }
  }

  private void applyFly(@NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef, boolean enable) {
    try {
      MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
      if (mm == null) {
        Logger.debug("[Utility] MovementManager not found for fly toggle");
        return;
      }

      mm.getDefaultSettings().canFly = enable;
      mm.getSettings().canFly = enable;
      mm.update(playerRef.getPacketHandler());
    } catch (Exception e) {
      Logger.debug("[Utility] Failed to toggle fly: %s", e.getMessage());
    }
  }
}
