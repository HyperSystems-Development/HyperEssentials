package com.hyperessentials.module.kits.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /deletekit <name> - Delete a kit.
 */
public class DeleteKitCommand extends AbstractPlayerCommand {

  private final KitsModule module;

  public DeleteKitCommand(@NotNull KitsModule module) {
    super("deletekit", "Delete a kit");
    this.module = module;
    addAliases("dkit");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.KIT_DELETE)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.DELETE_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.DELETE_USAGE));
      return;
    }

    String kitName = parts[1].toLowerCase();

    if (module.getKitManager().deleteKit(kitName)) {
      ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Kit.DELETE_SUCCESS, kitName));
    } else {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.DELETE_NOT_FOUND, kitName));
    }
  }
}
