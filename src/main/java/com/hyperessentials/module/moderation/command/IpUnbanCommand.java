package com.hyperessentials.module.moderation.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /ipunban <ip> - Unban an IP address.
 */
public class IpUnbanCommand extends AbstractPlayerCommand {

  private final ModerationModule module;

  public IpUnbanCommand(@NotNull ModerationModule module) {
    super("ipunban", "Unban an IP address");
    this.module = module;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.MODERATION_IPBAN)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to manage IP bans."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /ipunban <ip>"));
      return;
    }

    String ip = parts[1];

    if (module.getModerationManager().ipUnban(ip)) {
      ctx.sendMessage(CommandUtil.success("Unbanned IP: " + ip));
    } else {
      ctx.sendMessage(CommandUtil.error("IP '" + ip + "' is not banned."));
    }
  }
}
