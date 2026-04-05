package com.hyperessentials.module.kits.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.kits.KitManager;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.module.kits.data.Kit;
import com.hyperessentials.module.kits.data.KitItem;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.DurationParser;
import com.hyperessentials.util.HEMessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /previewkit <name> - Preview the contents of a kit.
 */
public class PreviewKitCommand extends AbstractPlayerCommand {

  private final KitsModule module;

  public PreviewKitCommand(@NotNull KitsModule module) {
    super("previewkit", "Preview the contents of a kit");
    this.module = module;
    addAliases("vkit", "viewkit");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.KIT_USE)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.PREVIEW_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.PREVIEW_USAGE));
      return;
    }

    String kitName = parts[1].toLowerCase();
    KitManager manager = module.getKitManager();
    Kit kit = manager.getKit(kitName);

    if (kit == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.NOT_FOUND, kitName));
      return;
    }

    ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Kit.PREVIEW_HEADER, HEMessageUtil.COLOR_GOLD, kit.displayName()));

    if (kit.items().isEmpty()) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Kit.PREVIEW_EMPTY, HEMessageUtil.COLOR_GRAY));
    } else {
      for (KitItem item : kit.items()) {
        String line = "  " + item.itemId() + " x" + item.quantity() + " [" + item.section() + "]";
        Message msg = HEMessageUtil.prefix()
          .insert(Message.raw(line).color(HEMessageUtil.COLOR_GRAY));
        ctx.sendMessage(msg);
      }
    }

    if (kit.cooldownSeconds() > 0) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Kit.PREVIEW_COOLDOWN, HEMessageUtil.COLOR_GRAY,
        DurationParser.formatHuman(kit.cooldownSeconds() * 1000L)));
    }
    if (kit.oneTime()) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Kit.PREVIEW_ONE_TIME, HEMessageUtil.COLOR_GRAY));
    }
    if (kit.permission() != null) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Kit.PREVIEW_PERMISSION, HEMessageUtil.COLOR_GRAY, kit.permission()));
    }
  }
}
