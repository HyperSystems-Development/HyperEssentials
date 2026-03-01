package com.hyperessentials.module.moderation.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.module.moderation.ModerationModule;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.util.DurationParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
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
      ctx.sendMessage(CommandUtil.error("You don't have permission to view punishment history."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      ctx.sendMessage(CommandUtil.error("Usage: /punishments <player>"));
      return;
    }

    String targetName = parts[1];
    UUID targetUuid = module.getModerationManager().findPlayerUuid(targetName);
    if (targetUuid == null) {
      ctx.sendMessage(CommandUtil.error("Player '" + targetName + "' not found."));
      return;
    }

    List<Punishment> history = module.getModerationManager().getHistory(targetUuid);
    if (history.isEmpty()) {
      ctx.sendMessage(CommandUtil.info("No punishments found for " + targetName + "."));
      return;
    }

    ctx.sendMessage(CommandUtil.info("Punishment History for " + targetName + " (" + history.size() + "):"));

    int shown = 0;
    for (int i = history.size() - 1; i >= 0 && shown < 10; i--, shown++) {
      Punishment p = history.get(i);
      String status = p.isEffective() ? "[ACTIVE]" : (p.active() ? "[EXPIRED]" : "[REVOKED]");
      String statusColor = p.isEffective() ? CommandUtil.COLOR_RED : CommandUtil.COLOR_GRAY;
      String duration = p.isPermanent() ? "permanent" : DurationParser.formatCompact(
        p.expiresAt().toEpochMilli() - p.issuedAt().toEpochMilli());

      Message line = CommandUtil.prefix()
        .insert(Message.raw("  " + status + " ").color(statusColor))
        .insert(Message.raw(p.type().name() + " ").color(CommandUtil.COLOR_YELLOW))
        .insert(Message.raw("by " + p.issuerName() + " ").color(CommandUtil.COLOR_WHITE))
        .insert(Message.raw("(" + duration + ") ").color(CommandUtil.COLOR_GRAY))
        .insert(Message.raw(DATE_FMT.format(p.issuedAt())).color(CommandUtil.COLOR_DARK_GRAY));

      ctx.sendMessage(line);

      if (p.reason() != null) {
        ctx.sendMessage(CommandUtil.msg("    Reason: " + p.reason(), CommandUtil.COLOR_GRAY));
      }
    }

    if (history.size() > 10) {
      ctx.sendMessage(CommandUtil.msg("  ... and " + (history.size() - 10) + " more", CommandUtil.COLOR_DARK_GRAY));
    }
  }
}
