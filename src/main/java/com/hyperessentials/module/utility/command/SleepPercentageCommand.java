package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.UtilityConfig;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
import com.hyperessentials.util.HEMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * /sleeppercentage (/sleeppct) [value] [world value] - View/set sleep skip percentage.
 */
public class SleepPercentageCommand extends AbstractPlayerCommand {

  public SleepPercentageCommand() {
    super("sleeppercentage", "View or set sleep skip percentage");
    addAliases("sleeppct", "spc");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_SLEEPPERCENTAGE)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.SLEEP_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    UtilityConfig config = ConfigManager.get().utility();

    if (parts.length <= 1) {
      // Show current values
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Utility.SLEEP_CURRENT, HEMessageUtil.COLOR_YELLOW, config.getSleepPercentage()));
      Map<String, Integer> worldPcts = config.getWorldSleepPercentages();
      if (!worldPcts.isEmpty()) {
        for (Map.Entry<String, Integer> entry : worldPcts.entrySet()) {
          ctx.sendMessage(HEMessageUtil.text("  " + entry.getKey() + ": " + entry.getValue() + "%", HEMessageUtil.COLOR_GRAY));
        }
      }
      if (config.getSleepPercentage() == 0) {
        ctx.sendMessage(HEMessageUtil.text("  " + HEMessages.get(playerRef, CommandKeys.Utility.SLEEP_DISABLED_HINT), HEMessageUtil.COLOR_DARK_GRAY));
      }
      return;
    }

    if (parts.length == 2) {
      // Set global: /sleeppercentage <number>
      try {
        int pct = Integer.parseInt(parts[1]);
        if (pct < 0 || pct > 100) {
          ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.SLEEP_INVALID_RANGE));
          return;
        }
        config.setSleepPercentage(pct);
        config.save();
        ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Utility.SLEEP_SET_GLOBAL, pct));
      } catch (NumberFormatException e) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.SLEEP_USAGE));
      }
      return;
    }

    // Set per-world: /sleeppercentage <world> <number>
    String worldName = parts[1];
    try {
      int pct = Integer.parseInt(parts[2]);
      if (pct < 0 || pct > 100) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.SLEEP_INVALID_RANGE));
        return;
      }
      config.getWorldSleepPercentages().put(worldName, pct);
      config.save();
      ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Utility.SLEEP_SET_WORLD, worldName, pct));
    } catch (NumberFormatException e) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.SLEEP_USAGE));
    }
  }
}
