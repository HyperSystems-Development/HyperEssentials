package com.hyperessentials.module.moderation.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.module.moderation.data.Punishment;
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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * /punishments <player> - View a player's punishment history.
 */
public class PunishmentsCommand extends AbstractPlayerCommand {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    .withZone(ZoneId.systemDefault());

  private final ModerationModule module;

  public PunishmentsCommand(@NotNull ModerationModule module) {
    super("punishments", "View punishment history");
    this.module = module;
    addAliases("pun");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.MODERATION_HISTORY)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.HISTORY_NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Moderation.HISTORY_USAGE));
      return;
    }

    String targetName = parts[1];
    UUID targetUuid = module.getModerationManager().findPlayerUuid(targetName);
    if (targetUuid == null) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.PLAYER_NOT_FOUND, targetName));
      return;
    }

    List<Punishment> history = module.getModerationManager().getHistory(targetUuid);
    if (history.isEmpty()) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Moderation.HISTORY_EMPTY, HEMessageUtil.COLOR_YELLOW, targetName));
      return;
    }

    ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Moderation.HISTORY_HEADER, HEMessageUtil.COLOR_YELLOW, targetName, history.size()));

    int shown = 0;
    for (int i = history.size() - 1; i >= 0 && shown < 10; i--, shown++) {
      Punishment p = history.get(i);
      String status = p.isEffective() ? "[ACTIVE]" : (p.active() ? "[EXPIRED]" : "[REVOKED]");
      String statusColor = p.isEffective() ? HEMessageUtil.COLOR_RED : HEMessageUtil.COLOR_GRAY;
      String duration = p.isPermanent() ? "permanent" : DurationParser.formatCompact(
        p.expiresAt().toEpochMilli() - p.issuedAt().toEpochMilli());

      Message line = HEMessageUtil.prefix()
        .insert(Message.raw("  " + status + " ").color(statusColor))
        .insert(Message.raw(p.type().name() + " ").color(HEMessageUtil.COLOR_YELLOW))
        .insert(Message.raw("by " + p.issuerName() + " ").color(HEMessageUtil.COLOR_WHITE))
        .insert(Message.raw("(" + duration + ") ").color(HEMessageUtil.COLOR_GRAY))
        .insert(Message.raw(DATE_FMT.format(p.issuedAt())).color(HEMessageUtil.COLOR_DARK_GRAY));

      ctx.sendMessage(line);

      if (p.reason() != null) {
        ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Moderation.HISTORY_REASON, HEMessageUtil.COLOR_GRAY, p.reason()));
      }
    }

    if (history.size() > 10) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Moderation.HISTORY_MORE, HEMessageUtil.COLOR_DARK_GRAY, history.size() - 10));
    }
  }
}
