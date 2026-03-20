package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.utility.UtilityModule;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.HEMessageUtil;
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.GOD_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length >= 2) {
      if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_GOD_OTHERS)) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.GOD_OTHERS_NO_PERMISSION));
        return;
      }

      PlayerRef target = CommandUtil.findOnlinePlayer(parts[1]);
      if (target == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.PLAYER_NOT_ONLINE, parts[1]));
        return;
      }

      // Resolve target's store/ref for cross-player god toggle
      Ref<EntityStore> targetRef = target.getReference();
      if (targetRef == null || !targetRef.isValid()) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.PLAYER_NOT_IN_WORLD, parts[1]));
        return;
      }
      Store<EntityStore> targetStore = targetRef.getStore();

      boolean nowGod = module.getUtilityManager().toggleGod(target.getUuid());
      applyGod(targetStore, targetRef, nowGod);

      String key = nowGod ? CommandKeys.Utility.GOD_ENABLED_OTHER : CommandKeys.Utility.GOD_DISABLED_OTHER;
      ctx.sendMessage(HEMessageUtil.success(playerRef, key, target.getUsername()));
      String selfKey = nowGod ? CommandKeys.Utility.GOD_ENABLED : CommandKeys.Utility.GOD_DISABLED;
      target.sendMessage(HEMessageUtil.success(target, selfKey));
    } else {
      boolean nowGod = module.getUtilityManager().toggleGod(playerRef.getUuid());
      applyGod(store, ref, nowGod);

      String key = nowGod ? CommandKeys.Utility.GOD_ENABLED : CommandKeys.Utility.GOD_DISABLED;
      ctx.sendMessage(HEMessageUtil.success(playerRef, key));
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
      ErrorHandler.report("[Utility] Invulnerable component toggle failed", e);
    }
  }
}
