package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatsModule;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /heal [player] - Heal self or another player to full health.
 */
public class HealCommand extends AbstractPlayerCommand {

  public HealCommand() {
    super("heal", "Heal a player to full health");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_HEAL)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to heal."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length >= 2) {
      if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_HEAL_OTHERS)) {
        ctx.sendMessage(CommandUtil.error("You don't have permission to heal others."));
        return;
      }

      PlayerRef target = CommandUtil.findOnlinePlayer(parts[1]);
      if (target == null) {
        ctx.sendMessage(CommandUtil.error("Player '" + parts[1] + "' is not online."));
        return;
      }

      // Resolve target's store/ref for cross-player healing
      Ref<EntityStore> targetRef = target.getReference();
      if (targetRef == null || !targetRef.isValid()) {
        ctx.sendMessage(CommandUtil.error("Player '" + parts[1] + "' is not in a world."));
        return;
      }
      Store<EntityStore> targetStore = targetRef.getStore();

      healPlayer(targetStore, targetRef);
      ctx.sendMessage(CommandUtil.success("Healed " + target.getUsername() + "."));
      target.sendMessage(CommandUtil.success("You have been healed."));
    } else {
      healPlayer(store, ref);
      ctx.sendMessage(CommandUtil.success("You have been healed."));
    }
  }

  private void healPlayer(@NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref) {
    try {
      EntityStatMap statMap = store.getComponent(ref,
        EntityStatsModule.get().getEntityStatMapComponentType());
      if (statMap != null) {
        statMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
      }
    } catch (Exception e) {
      Logger.debug("[Utility] Failed to heal via EntityStatsModule: %s", e.getMessage());
    }
  }
}
