package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.utility.UtilityModule;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /god [player] - Toggle god mode (invulnerability).
 */
public class GodCommand extends AbstractPlayerCommand {

  private final UtilityModule module;

  public GodCommand(@NotNull UtilityModule module) {
    super("god", "Toggle god mode");
    this.module = module;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_GOD)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to use god mode."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length >= 2) {
      if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_GOD_OTHERS)) {
        ctx.sendMessage(CommandUtil.error("You don't have permission to toggle god mode for others."));
        return;
      }

      PlayerRef target = CommandUtil.findOnlinePlayer(parts[1]);
      if (target == null) {
        ctx.sendMessage(CommandUtil.error("Player '" + parts[1] + "' is not online."));
        return;
      }

      boolean nowGod = module.getUtilityManager().toggleGod(target.getUuid());
      applyGod(store, ref, nowGod);

      ctx.sendMessage(CommandUtil.success("God mode " + (nowGod ? "enabled" : "disabled") + " for " + target.getUsername() + "."));
      target.sendMessage(CommandUtil.success("God mode " + (nowGod ? "enabled" : "disabled") + "."));
    } else {
      boolean nowGod = module.getUtilityManager().toggleGod(playerRef.getUuid());
      applyGod(store, ref, nowGod);

      ctx.sendMessage(CommandUtil.success("God mode " + (nowGod ? "enabled" : "disabled") + "."));
    }
  }

  private void applyGod(@NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, boolean enable) {
    try {
      if (enable) {
        store.addComponent(ref, Invulnerable.getComponentType());
      } else {
        store.removeComponent(ref, Invulnerable.getComponentType());
      }
    } catch (Exception e) {
      Logger.debug("[Utility] Invulnerable component toggle failed: %s", e.getMessage());
    }
  }
}
