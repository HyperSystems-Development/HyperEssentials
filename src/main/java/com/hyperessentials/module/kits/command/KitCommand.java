package com.hyperessentials.module.kits.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.integration.FactionTerritoryChecker;
import com.hyperessentials.integration.HyperFactionsIntegration;
import com.hyperessentials.module.kits.KitManager;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.module.kits.data.Kit;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.DurationParser;
import com.hyperessentials.util.HEMessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.NO_PERMISSION));
      return;
    }

    // Zone flag check (player's current location)
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.BYPASS_FACTIONS_KIT)
        && !CommandUtil.hasPermission(playerRef.getUuid(), Permissions.BYPASS_FACTIONS)) {
      TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
      if (transform != null) {
        Vector3d pos = transform.getPosition();
        FactionTerritoryChecker.Result zoneResult = FactionTerritoryChecker.checkZoneFlag(
            world.getName(), pos.getX(), pos.getZ(), HyperFactionsIntegration.FLAG_KITS);
        if (zoneResult != FactionTerritoryChecker.Result.ALLOWED) {
          ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.ZONE_RESTRICTED));
          return;
        }
      }
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.USAGE));
      return;
    }

    String kitName = parts[1].toLowerCase();
    KitManager manager = module.getKitManager();
    Kit kit = manager.getKit(kitName);

    if (kit == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.NOT_FOUND, kitName));
      return;
    }

    KitManager.ClaimResult result = manager.claimKit(
      playerRef.getUuid(), playerRef, store, ref, kit
    );

    switch (result) {
      case SUCCESS -> ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Kit.CLAIMED, kit.displayName()));
      case ON_COOLDOWN -> {
        long remaining = manager.getRemainingCooldown(playerRef.getUuid(), kitName);
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.ON_COOLDOWN, DurationParser.formatHuman(remaining)));
      }
      case ALREADY_CLAIMED -> ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.ALREADY_CLAIMED));
      case NO_PERMISSION -> ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.KIT_NO_PERMISSION));
      case KIT_NOT_FOUND -> ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.NOT_FOUND, kitName));
      case INSUFFICIENT_SPACE -> ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Kit.INSUFFICIENT_SPACE));
    }
  }
}
