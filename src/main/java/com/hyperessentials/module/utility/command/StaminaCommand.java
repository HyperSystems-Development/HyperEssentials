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
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /stamina (/stam) [player] - Toggle infinite stamina.
 */
public class StaminaCommand extends AbstractPlayerCommand {

  private final UtilityModule module;

  public StaminaCommand(@NotNull UtilityModule module) {
    super("stamina", "Toggle infinite stamina");
    addAliases("stam");
    this.module = module;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_STAMINA)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.STAMINA_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length >= 2) {
      if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_STAMINA_OTHERS)) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.STAMINA_OTHERS_NO_PERMISSION));
        return;
      }

      PlayerRef target = CommandUtil.findOnlinePlayer(parts[1]);
      if (target == null) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.PLAYER_NOT_ONLINE, parts[1]));
        return;
      }

      boolean nowEnabled = module.getUtilityManager().toggleInfiniteStamina(target.getUuid());
      if (nowEnabled) maximizeStamina(store, ref);

      String key = nowEnabled ? CommandKeys.Utility.STAMINA_ENABLED_OTHER : CommandKeys.Utility.STAMINA_DISABLED_OTHER;
      ctx.sendMessage(HEMessageUtil.success(playerRef, key, target.getUsername()));
      String selfKey = nowEnabled ? CommandKeys.Utility.STAMINA_ENABLED : CommandKeys.Utility.STAMINA_DISABLED;
      target.sendMessage(HEMessageUtil.success(target, selfKey));
    } else {
      boolean nowEnabled = module.getUtilityManager().toggleInfiniteStamina(playerRef.getUuid());
      if (nowEnabled) maximizeStamina(store, ref);

      String key = nowEnabled ? CommandKeys.Utility.STAMINA_ENABLED : CommandKeys.Utility.STAMINA_DISABLED;
      ctx.sendMessage(HEMessageUtil.success(playerRef, key));
    }
  }

  private void maximizeStamina(@NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref) {
    try {
      EntityStatMap statMap = store.getComponent(ref,
        EntityStatsModule.get().getEntityStatMapComponentType());
      if (statMap != null) {
        statMap.maximizeStatValue(DefaultEntityStatTypes.getStamina());
      }
    } catch (Exception e) {
      ErrorHandler.report("[Utility] Stamina maximize failed", e);
    }
  }
}
