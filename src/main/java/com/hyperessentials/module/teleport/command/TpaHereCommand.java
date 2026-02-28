package com.hyperessentials.module.teleport.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.modules.TeleportConfig;
import com.hyperessentials.data.TeleportRequest;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
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
 * /tpahere &lt;player&gt; - Request another player to teleport to you.
 */
public class TpaHereCommand extends AbstractPlayerCommand {

  private final TpaManager tpaManager;
  private final TeleportConfig config;

  public TpaHereCommand(@NotNull TpaManager tpaManager, @NotNull TeleportConfig config) {
    super("tpahere", "Request a player to teleport to you");
    this.tpaManager = tpaManager;
    this.config = config;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.TPAHERE)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to use TPAHere."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /tpahere <player>"));
      return;
    }

    String targetName = parts[1];

    PlayerRef targetRef = findPlayer(targetName);
    if (targetRef == null) {
      ctx.sendMessage(CommandUtil.error("Player '" + targetName + "' not found or offline."));
      return;
    }

    UUID targetUuid = targetRef.getUuid();

    if (uuid.equals(targetUuid)) {
      ctx.sendMessage(CommandUtil.error("You cannot request teleport from yourself."));
      return;
    }

    long cooldown = tpaManager.getRemainingTpaCooldown(uuid);
    if (cooldown > 0) {
      ctx.sendMessage(CommandUtil.error("You must wait " + CommandUtil.formatTime(cooldown) + " before sending another request."));
      return;
    }

    TeleportRequest request = tpaManager.createRequest(uuid, targetUuid, TeleportRequest.Type.TPAHERE);

    if (request == null) {
      if (!tpaManager.isAcceptingRequests(targetUuid)) {
        ctx.sendMessage(CommandUtil.error(targetRef.getUsername() + " is not accepting teleport requests."));
      } else {
        ctx.sendMessage(CommandUtil.error("Could not send request. Target may have too many pending requests."));
      }
      return;
    }

    ctx.sendMessage(CommandUtil.success("Teleport request sent to " + targetRef.getUsername() + "."));
    ctx.sendMessage(CommandUtil.info("Request expires in " + config.getTpaTimeout() + " seconds."));

    targetRef.sendMessage(CommandUtil.info(playerRef.getUsername() + " wants you to teleport to them."));
    targetRef.sendMessage(CommandUtil.info("Type /tpaccept or /tpdeny to respond."));
  }

  private PlayerRef findPlayer(String name) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    return plugin != null ? plugin.findOnlinePlayer(name) : null;
  }
}
