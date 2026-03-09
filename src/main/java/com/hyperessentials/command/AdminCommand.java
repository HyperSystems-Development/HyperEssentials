package com.hyperessentials.command;

import com.hyperessentials.BuildInfo;
import com.hyperessentials.Permissions;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hyperessentials.module.spawns.SpawnsModule;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * Main admin command for HyperEssentials (/hessentials).
 */
public class AdminCommand extends AbstractPlayerCommand {

  public AdminCommand() {
    super("hessentials", "HyperEssentials admin command");
    addAliases("he", "hyperessentials");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {

    String input = ctx.getInputString();
    String subcommand = "";
    if (input != null && !input.isEmpty()) {
      String[] parts = input.trim().split("\\s+");
      if (parts.length > 1) {
        subcommand = parts[1].toLowerCase();
      }
    }

    switch (subcommand) {
      case "reload" -> handleReload(ctx, playerRef);
      case "importspawns" -> handleImportSpawns(ctx, playerRef);
      case "version", "ver" -> showVersion(ctx);
      case "help" -> showFullHelp(ctx);
      case "admin" -> openAdminGui(ctx, store, ref, playerRef);
      default -> openPlayerGui(ctx, store, ref, playerRef);
    }
  }

  private void handleReload(@NotNull CommandContext ctx, @NotNull PlayerRef playerRef) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.ADMIN_RELOAD)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to reload."));
      return;
    }

    ConfigManager.get().reloadAll();
    ctx.sendMessage(CommandUtil.success("Configuration reloaded."));
  }

  private void handleImportSpawns(@NotNull CommandContext ctx, @NotNull PlayerRef playerRef) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.ADMIN)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to import spawns."));
      return;
    }

    if (!HyperEssentialsAPI.isAvailable()) {
      ctx.sendMessage(CommandUtil.error("HyperEssentials is not initialized."));
      return;
    }

    SpawnsModule spawnsModule = HyperEssentialsAPI.getInstance().getSpawnsModule();
    if (spawnsModule == null || spawnsModule.getSpawnManager() == null) {
      ctx.sendMessage(CommandUtil.error("Spawns module is not enabled."));
      return;
    }

    SpawnManager spawnManager = spawnsModule.getSpawnManager();
    int imported = spawnManager.importWorldSpawns();

    if (imported > 0) {
      ctx.sendMessage(CommandUtil.success("Imported " + imported + " world spawn(s) from server config."));
    } else {
      ctx.sendMessage(CommandUtil.error("No world spawns could be imported."));
    }
  }

  private void showVersion(@NotNull CommandContext ctx) {
    ctx.sendMessage(CommandUtil.info("HyperEssentials v" + BuildInfo.VERSION));
  }

  private void showHelp(@NotNull CommandContext ctx) {
    ctx.sendMessage(CommandUtil.info("HyperEssentials v" + BuildInfo.VERSION));
    ctx.sendMessage(CommandUtil.msg("/he reload - Reload configuration", CommandUtil.COLOR_GRAY));
    ctx.sendMessage(CommandUtil.msg("/he importspawns - Import world spawns", CommandUtil.COLOR_GRAY));
    ctx.sendMessage(CommandUtil.msg("/he version - Show version", CommandUtil.COLOR_GRAY));
    ctx.sendMessage(CommandUtil.msg("/he help - Full command listing", CommandUtil.COLOR_GRAY));
  }

  private void showFullHelp(@NotNull CommandContext ctx) {
    ctx.sendMessage(CommandUtil.msg("=== HyperEssentials v" + BuildInfo.VERSION + " ===", CommandUtil.COLOR_GOLD));

    if (isModuleEnabled("homes")) {
      ctx.sendMessage(CommandUtil.msg("[Homes] /sethome, /home, /delhome, /homes", CommandUtil.COLOR_AQUA));
    }
    if (isModuleEnabled("warps")) {
      ctx.sendMessage(CommandUtil.msg("[Warps] /warp, /setwarp, /delwarp, /warps, /warpinfo", CommandUtil.COLOR_AQUA));
    }
    if (isModuleEnabled("spawns")) {
      ctx.sendMessage(CommandUtil.msg("[Spawns] /spawn, /setspawn, /delspawn, /spawns, /spawninfo", CommandUtil.COLOR_AQUA));
    }
    if (isModuleEnabled("teleport")) {
      ctx.sendMessage(CommandUtil.msg("[Teleport] /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel, /tptoggle, /back, /rtp", CommandUtil.COLOR_AQUA));
    }
    if (isModuleEnabled("kits")) {
      ctx.sendMessage(CommandUtil.msg("[Kits] /kit, /kits, /createkit, /deletekit, /previewkit", CommandUtil.COLOR_AQUA));
    }
    if (isModuleEnabled("moderation")) {
      ctx.sendMessage(CommandUtil.msg("[Moderation] /ban, /unban, /mute, /unmute, /kick, /freeze, /vanish, /punishments, /ipban, /ipunban", CommandUtil.COLOR_AQUA));
    }
    if (isModuleEnabled("utility")) {
      ctx.sendMessage(CommandUtil.msg("[Utility] /heal, /fly, /god, /stamina, /repair, /repairmax, /durability, /maxstack,", CommandUtil.COLOR_AQUA));
      ctx.sendMessage(CommandUtil.msg("  /near, /clearchat, /clearinventory, /motd, /rules, /discord, /list,", CommandUtil.COLOR_AQUA));
      ctx.sendMessage(CommandUtil.msg("  /playtime, /joindate, /afk, /invsee, /trash, /sleeppercentage", CommandUtil.COLOR_AQUA));
    }
    if (isModuleEnabled("announcements")) {
      ctx.sendMessage(CommandUtil.msg("[Announcements] /broadcast, /announce", CommandUtil.COLOR_AQUA));
    }
    ctx.sendMessage(CommandUtil.msg("[Admin] /he reload | version | help | importspawns", CommandUtil.COLOR_AQUA));
  }

  private void openPlayerGui(@NotNull CommandContext ctx,
                             @NotNull Store<EntityStore> store,
                             @NotNull Ref<EntityStore> ref,
                             @NotNull PlayerRef playerRef) {
    if (!HyperEssentialsAPI.isAvailable()) {
      showHelp(ctx);
      return;
    }

    GuiManager guiManager = HyperEssentialsAPI.getInstance().getGuiManager();
    if (guiManager.getPlayerRegistry().getEntries().isEmpty()) {
      showHelp(ctx);
      return;
    }

    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      showHelp(ctx);
      return;
    }

    if (!guiManager.openPlayerPage("dashboard", player, ref, store, playerRef)) {
      showHelp(ctx);
    }
  }

  private void openAdminGui(@NotNull CommandContext ctx,
                            @NotNull Store<EntityStore> store,
                            @NotNull Ref<EntityStore> ref,
                            @NotNull PlayerRef playerRef) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.ADMIN_GUI)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to access the admin panel."));
      return;
    }

    if (!HyperEssentialsAPI.isAvailable()) {
      ctx.sendMessage(CommandUtil.error("HyperEssentials is not initialized."));
      return;
    }

    GuiManager guiManager = HyperEssentialsAPI.getInstance().getGuiManager();
    if (guiManager.getAdminRegistry().getEntries().isEmpty()) {
      ctx.sendMessage(CommandUtil.error("No admin pages are registered."));
      return;
    }

    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) {
      ctx.sendMessage(CommandUtil.error("Could not resolve player."));
      return;
    }

    if (!guiManager.openAdminPage("dashboard", player, ref, store, playerRef)) {
      ctx.sendMessage(CommandUtil.error("Could not open admin dashboard."));
    }
  }

  private boolean isModuleEnabled(@NotNull String name) {
    return HyperEssentialsAPI.isAvailable() && HyperEssentialsAPI.getInstance().isModuleEnabled(name);
  }
}
