package com.hyperessentials.module.teleport.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.modules.TeleportConfig;
import com.hyperessentials.data.TeleportRequest;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
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

import java.util.UUID;

/**
 * /tpa &lt;player&gt; - Request to teleport to another player.
 */
public class TpaCommand extends AbstractPlayerCommand {

  private final TpaManager tpaManager;
  private final TeleportConfig config;

  public TpaCommand(@NotNull TpaManager tpaManager, @NotNull TeleportConfig config) {
    super("tpa", "Request to teleport to a player");
    this.tpaManager = tpaManager;
    this.config = config;
    addAliases("tpr");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.TPA)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.USAGE));
      return;
    }

    String targetName = parts[1];

    PlayerRef targetRef = findPlayer(targetName);
    if (targetRef == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.PLAYER_NOT_FOUND, targetName));
      return;
    }

    UUID targetUuid = targetRef.getUuid();

    if (uuid.equals(targetUuid)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.CANNOT_SELF));
      return;
    }

    long cooldown = tpaManager.getRemainingTpaCooldown(uuid);
    if (cooldown > 0) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.COOLDOWN, CommandUtil.formatTime(cooldown)));
      return;
    }

    TeleportRequest request = tpaManager.createRequest(uuid, targetUuid, TeleportRequest.Type.TPA);

    if (request == null) {
      if (!tpaManager.isAcceptingRequests(targetUuid)) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.TARGET_NOT_ACCEPTING, targetRef.getUsername()));
      } else {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Tpa.SEND_FAILED));
      }
      return;
    }

    ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Tpa.SENT, targetRef.getUsername()));
    ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Tpa.EXPIRES_IN, HEMessageUtil.COLOR_YELLOW, config.getTpaTimeout()));

    targetRef.sendMessage(HEMessageUtil.info(targetRef, CommandKeys.Tpa.RECEIVED_TPA, HEMessageUtil.COLOR_YELLOW, playerRef.getUsername()));
    targetRef.sendMessage(HEMessageUtil.info(targetRef, CommandKeys.Tpa.RESPOND_HINT, HEMessageUtil.COLOR_YELLOW));
  }

  private PlayerRef findPlayer(String name) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    return plugin != null ? plugin.findOnlinePlayer(name) : null;
  }
}
