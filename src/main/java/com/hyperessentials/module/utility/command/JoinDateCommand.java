package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.utility.UtilityModule;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * /joindate (/firstjoin) - Show when you first joined.
 */
public class JoinDateCommand extends AbstractPlayerCommand {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault());

  private final UtilityModule module;

  public JoinDateCommand(@NotNull UtilityModule module) {
    super("joindate", "Show your first join date");
    addAliases("firstjoin");
    this.module = module;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_JOINDATE)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to view join date."));
      return;
    }

    Instant firstJoin = module.getUtilityManager().getFirstJoin(playerRef.getUuid());
    if (firstJoin == null) {
      ctx.sendMessage(CommandUtil.error("No join data available."));
      return;
    }

    ctx.sendMessage(CommandUtil.success("You first joined: " + FORMATTER.format(firstJoin)));
  }
}
