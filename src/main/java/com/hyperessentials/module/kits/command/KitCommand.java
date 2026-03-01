package com.hyperessentials.module.kits.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.kits.KitManager;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.module.kits.data.Kit;
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
 * /kit <name> - Claim a kit.
 */
public class KitCommand extends AbstractPlayerCommand {

  private final KitsModule module;

  public KitCommand(@NotNull KitsModule module) {
    super("kit", "Claim a kit");
    this.module = module;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.KIT_USE)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to use kits."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /kit <name>"));
      return;
    }

    String kitName = parts[1].toLowerCase();
    KitManager manager = module.getKitManager();
    Kit kit = manager.getKit(kitName);

    if (kit == null) {
      ctx.sendMessage(CommandUtil.error("Kit '" + kitName + "' not found."));
      return;
    }

    KitManager.ClaimResult result = manager.claimKit(
      playerRef.getUuid(), playerRef, store, ref, kit
    );

    switch (result) {
      case SUCCESS -> ctx.sendMessage(CommandUtil.success("Kit '" + kit.displayName() + "' claimed!"));
      case ON_COOLDOWN -> {
        long remaining = manager.getRemainingCooldown(playerRef.getUuid(), kitName);
        ctx.sendMessage(CommandUtil.error("Kit on cooldown. " + DurationParser.formatHuman(remaining) + " remaining."));
      }
      case ALREADY_CLAIMED -> ctx.sendMessage(CommandUtil.error("You have already claimed this one-time kit."));
      case NO_PERMISSION -> ctx.sendMessage(CommandUtil.error("You don't have permission to use this kit."));
      case KIT_NOT_FOUND -> ctx.sendMessage(CommandUtil.error("Kit '" + kitName + "' not found."));
      case INSUFFICIENT_SPACE -> ctx.sendMessage(CommandUtil.error("Not enough inventory space to claim this kit."));
    }
  }
}
