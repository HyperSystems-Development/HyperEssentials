package com.hyperessentials.module.kits.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.kits.KitManager;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.module.kits.data.Kit;
import com.hyperessentials.module.kits.data.KitItem;
import com.hyperessentials.util.DurationParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
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
      ctx.sendMessage(CommandUtil.error("You don't have permission to preview kits."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /previewkit <name>"));
      return;
    }

    String kitName = parts[1].toLowerCase();
    KitManager manager = module.getKitManager();
    Kit kit = manager.getKit(kitName);

    if (kit == null) {
      ctx.sendMessage(CommandUtil.error("Kit '" + kitName + "' not found."));
      return;
    }

    ctx.sendMessage(CommandUtil.msg("--- Kit: " + kit.displayName() + " ---", CommandUtil.COLOR_GOLD));

    if (kit.items().isEmpty()) {
      ctx.sendMessage(CommandUtil.msg("  (empty)", CommandUtil.COLOR_GRAY));
    } else {
      for (KitItem item : kit.items()) {
        String line = "  " + item.itemId() + " x" + item.quantity() + " [" + item.section() + "]";
        ctx.sendMessage(CommandUtil.msg(line, CommandUtil.COLOR_GRAY));
      }
    }

    if (kit.cooldownSeconds() > 0) {
      ctx.sendMessage(CommandUtil.msg("Cooldown: " + DurationParser.formatHuman(kit.cooldownSeconds() * 1000L), CommandUtil.COLOR_GRAY));
    }
    if (kit.oneTime()) {
      ctx.sendMessage(CommandUtil.msg("One-time use only", CommandUtil.COLOR_GRAY));
    }
    if (kit.permission() != null) {
      ctx.sendMessage(CommandUtil.msg("Permission: " + kit.permission(), CommandUtil.COLOR_GRAY));
    }
  }
}
