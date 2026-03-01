package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.utility.UtilityModule;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
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
      ctx.sendMessage(CommandUtil.error("You don't have permission to toggle infinite stamina."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length >= 2) {
      if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_STAMINA_OTHERS)) {
        ctx.sendMessage(CommandUtil.error("You don't have permission to toggle stamina for others."));
        return;
      }

      PlayerRef target = CommandUtil.findOnlinePlayer(parts[1]);
      if (target == null) {
        ctx.sendMessage(CommandUtil.error("Player '" + parts[1] + "' is not online."));
        return;
      }

      boolean nowEnabled = module.getUtilityManager().toggleInfiniteStamina(target.getUuid());
      if (nowEnabled) maximizeStamina(store, ref);

      ctx.sendMessage(CommandUtil.success("Infinite stamina " + (nowEnabled ? "enabled" : "disabled") + " for " + target.getUsername() + "."));
      target.sendMessage(CommandUtil.success("Infinite stamina " + (nowEnabled ? "enabled" : "disabled") + "."));
    } else {
      boolean nowEnabled = module.getUtilityManager().toggleInfiniteStamina(playerRef.getUuid());
      if (nowEnabled) maximizeStamina(store, ref);

      ctx.sendMessage(CommandUtil.success("Infinite stamina " + (nowEnabled ? "enabled" : "disabled") + "."));
    }
  }

  private void maximizeStamina(@NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref) {
    try {
      EntityStatMap statMap = store.getComponent(ref,
        EntityStatsModule.get().getEntityStatMapComponentType());
      if (statMap != null) {
        for (int i = 0; i < statMap.size(); i++) {
          try {
            statMap.maximizeStatValue(i);
          } catch (Exception ignored) {}
        }
      }
    } catch (Exception e) {
      Logger.debug("[Utility] Stamina maximize failed: %s", e.getMessage());
    }
  }
}
