package com.hyperessentials.module.announcements.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.AnnouncementsConfig;
import com.hyperessentials.module.announcements.AnnouncementsModule;
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
            ctx.sendMessage(CommandUtil.error("You don't have permission to manage announcements."));
            return;
        }

        String input = ctx.getInputString();
        String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

        if (parts.length < 2) {
            showHelp(ctx);
            return;
        }

        String subcommand = parts[1].toLowerCase();

        switch (subcommand) {
            case "list" -> handleList(ctx);
            case "add" -> handleAdd(ctx, parts);
            case "remove" -> handleRemove(ctx, parts);
            case "reload" -> handleReload(ctx);
            default -> showHelp(ctx);
        }
    }

    private void handleList(@NotNull CommandContext ctx) {
        List<String> messages = ConfigManager.get().announcements().getMessages();

        if (messages.isEmpty()) {
            ctx.sendMessage(CommandUtil.info("No announcements configured."));
            return;
        }

        ctx.sendMessage(CommandUtil.info("Announcements (" + messages.size() + "):"));
        for (int i = 0; i < messages.size(); i++) {
            Message line = CommandUtil.prefix()
                .insert(Message.raw("  " + (i + 1) + ". ").color(CommandUtil.COLOR_GOLD))
                .insert(Message.raw(messages.get(i)).color(CommandUtil.COLOR_WHITE));
            ctx.sendMessage(line);
        }
    }

    private void handleAdd(@NotNull CommandContext ctx, String[] parts) {
        if (parts.length < 3) {
            ctx.sendMessage(CommandUtil.error("Usage: /announce add <message>"));
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            if (i > 2) sb.append(' ');
            sb.append(parts[i]);
        }
        String message = sb.toString();

        AnnouncementsConfig config = ConfigManager.get().announcements();
        config.getMessages().add(message);
        config.save();

        ctx.sendMessage(CommandUtil.success("Added announcement: " + message));
    }

    private void handleRemove(@NotNull CommandContext ctx, String[] parts) {
        if (parts.length < 3) {
            ctx.sendMessage(CommandUtil.error("Usage: /announce remove <index>"));
            return;
        }

        int index;
        try {
            index = Integer.parseInt(parts[2]) - 1;
        } catch (NumberFormatException e) {
            ctx.sendMessage(CommandUtil.error("Invalid index."));
            return;
        }

        List<String> messages = ConfigManager.get().announcements().getMessages();
        if (index < 0 || index >= messages.size()) {
            ctx.sendMessage(CommandUtil.error("Index out of range (1-" + messages.size() + ")."));
            return;
        }

        String removed = messages.remove(index);
        ConfigManager.get().announcements().save();

        ctx.sendMessage(CommandUtil.success("Removed announcement: " + removed));
    }

    private void handleReload(@NotNull CommandContext ctx) {
        ConfigManager.get().announcements().reload();
        module.getScheduler().restart();
        ctx.sendMessage(CommandUtil.success("Announcements reloaded."));
    }

    private void showHelp(@NotNull CommandContext ctx) {
        ctx.sendMessage(CommandUtil.info("Announcement Commands:"));
        ctx.sendMessage(CommandUtil.msg("  /announce list - List announcements", CommandUtil.COLOR_GRAY));
        ctx.sendMessage(CommandUtil.msg("  /announce add <message> - Add announcement", CommandUtil.COLOR_GRAY));
        ctx.sendMessage(CommandUtil.msg("  /announce remove <index> - Remove announcement", CommandUtil.COLOR_GRAY));
        ctx.sendMessage(CommandUtil.msg("  /announce reload - Reload announcements", CommandUtil.COLOR_GRAY));
    }
}
