package com.hyperessentials.module.announcements.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.AnnouncementsConfig;
import com.hyperessentials.module.announcements.AnnouncementsModule;
import com.hyperessentials.module.announcements.data.Announcement;
import com.hyperessentials.module.announcements.data.AnnouncementType;
import com.hyperessentials.util.CommandKeys;
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

import java.util.List;
import java.util.UUID;

/**
 * /announce list|add|remove|reload - Manage announcement rotation.
 */
public class AnnounceCommand extends AbstractPlayerCommand {

  private final AnnouncementsModule module;

  public AnnounceCommand(@NotNull AnnouncementsModule module) {
    super("announce", "Manage announcements");
    this.module = module;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.ANNOUNCE_MANAGE)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Announce.NO_PERMISSION));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    if (parts.length < 2) {
      showHelp(ctx, playerRef);
      return;
    }

    String subcommand = parts[1].toLowerCase();

    switch (subcommand) {
      case "list" -> handleList(ctx, playerRef);
      case "add" -> handleAdd(ctx, playerRef, parts);
      case "remove" -> handleRemove(ctx, playerRef, parts);
      case "reload" -> handleReload(ctx, playerRef);
      default -> showHelp(ctx, playerRef);
    }
  }

  private void handleList(@NotNull CommandContext ctx, @NotNull PlayerRef playerRef) {
    AnnouncementsConfig config = ConfigManager.get().announcements();
    List<Announcement> announcements = config.getAnnouncements();

    if (announcements.isEmpty()) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Announce.LIST_EMPTY, HEMessageUtil.COLOR_YELLOW));
      return;
    }

    ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Announce.LIST_HEADER, HEMessageUtil.COLOR_YELLOW, announcements.size()));
    for (int i = 0; i < announcements.size(); i++) {
      Announcement ann = announcements.get(i);
      String statusPrefix = ann.enabled() ? "" : "[OFF] ";
      String typePrefix = "[" + ann.type().name() + "] ";
      Message line = HEMessageUtil.prefix()
        .insert(Message.raw("  " + (i + 1) + ". ").color(HEMessageUtil.COLOR_GOLD))
        .insert(Message.raw(typePrefix).color(HEMessageUtil.COLOR_CYAN))
        .insert(Message.raw(statusPrefix).color(HEMessageUtil.COLOR_RED))
        .insert(Message.raw(ann.message()).color(HEMessageUtil.COLOR_WHITE));
      ctx.sendMessage(line);
    }
  }

  private void handleAdd(@NotNull CommandContext ctx, @NotNull PlayerRef playerRef, String[] parts) {
    if (parts.length < 3) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Announce.ADD_USAGE));
      return;
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 2; i < parts.length; i++) {
      if (i > 2) sb.append(' ');
      sb.append(parts[i]);
    }
    String message = sb.toString();

    AnnouncementsConfig config = ConfigManager.get().announcements();
    int nextOrder = config.getAnnouncements().size();
    Announcement newAnn = new Announcement(
        UUID.randomUUID(), message, AnnouncementType.CHAT,
        true, 0, null, null, null, nextOrder
    );
    config.getAnnouncements().add(newAnn);
    config.save();

    // Restart scheduler to pick up changes
    module.getScheduler().restart();

    ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Announce.ADD_SUCCESS, message));
  }

  private void handleRemove(@NotNull CommandContext ctx, @NotNull PlayerRef playerRef, String[] parts) {
    if (parts.length < 3) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Announce.REMOVE_USAGE));
      return;
    }

    int index;
    try {
      index = Integer.parseInt(parts[2]) - 1;
    } catch (NumberFormatException e) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Announce.REMOVE_INVALID_INDEX));
      return;
    }

    AnnouncementsConfig config = ConfigManager.get().announcements();
    List<Announcement> announcements = config.getAnnouncements();
    if (index < 0 || index >= announcements.size()) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Announce.REMOVE_OUT_OF_RANGE, announcements.size()));
      return;
    }

    Announcement removed = announcements.remove(index);
    config.save();

    // Restart scheduler to pick up changes
    module.getScheduler().restart();

    ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Announce.REMOVE_SUCCESS, removed.message()));
  }

  private void handleReload(@NotNull CommandContext ctx, @NotNull PlayerRef playerRef) {
    ConfigManager.get().announcements().reload();
    module.getScheduler().restart();
    ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Announce.RELOAD_SUCCESS));
  }

  private void showHelp(@NotNull CommandContext ctx, @NotNull PlayerRef playerRef) {
    ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Announce.HELP_HEADER, HEMessageUtil.COLOR_YELLOW));
    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Announce.HELP_LIST, HEMessageUtil.COLOR_GRAY));
    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Announce.HELP_ADD, HEMessageUtil.COLOR_GRAY));
    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Announce.HELP_REMOVE, HEMessageUtil.COLOR_GRAY));
    ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Announce.HELP_RELOAD, HEMessageUtil.COLOR_GRAY));
  }
}
