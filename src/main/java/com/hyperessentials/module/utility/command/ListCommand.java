package com.hyperessentials.module.utility.command;

import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /list (/online, /players) - Show online players.
 */
public class ListCommand extends AbstractPlayerCommand {

  public ListCommand() {
    super("list", "Show online players");
    addAliases("online", "players");
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    Map<UUID, PlayerRef> tracked = plugin.getTrackedPlayers();
    List<String> names = tracked.values().stream()
      .map(PlayerRef::getUsername)
      .sorted(String.CASE_INSENSITIVE_ORDER)
      .toList();

    ctx.sendMessage(CommandUtil.msg("Online Players (" + names.size() + "): " + String.join(", ", names), CommandUtil.COLOR_GOLD));
  }
}
