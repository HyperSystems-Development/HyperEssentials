package com.hyperessentials.module.warps.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Warp;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.module.warps.WarpManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /warps [category] - List all warps or warps in a category.
 * Opens GUI page when available (no category filter), falls back to text list.
 */
public class WarpsCommand extends AbstractPlayerCommand {

  private final WarpManager warpManager;

  public WarpsCommand(@NotNull WarpManager warpManager) {
    super("warps", "List server warps");
    this.warpManager = warpManager;
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.WARP_LIST)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to list warps."));
      return;
    }

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];
    String category = parts.length > 1 ? parts[1].toLowerCase() : null;

    // Try GUI page for unfiltered /warps
    if (category == null && tryOpenGui(store, ref, playerRef)) {
      return;
    }

    // Text fallback
    List<Warp> warps;
    if (category != null) {
      warps = warpManager.getAccessibleWarpsByCategory(uuid, category);
      if (warps.isEmpty()) {
        ctx.sendMessage(CommandUtil.info("No warps found in category '" + category + "'."));
        return;
      }
      ctx.sendMessage(CommandUtil.msg("--- Warps in '" + category + "' ---", CommandUtil.COLOR_GOLD));
    } else {
      warps = warpManager.getAccessibleWarps(uuid);
      if (warps.isEmpty()) {
        ctx.sendMessage(CommandUtil.info("No warps available."));
        return;
      }
      ctx.sendMessage(CommandUtil.msg("--- Server Warps ---", CommandUtil.COLOR_GOLD));
    }

    Map<String, List<Warp>> grouped = warps.stream()
      .collect(Collectors.groupingBy(Warp::category));

    for (Map.Entry<String, List<Warp>> entry : grouped.entrySet()) {
      String cat = entry.getKey();
      List<Warp> catWarps = entry.getValue();

      if (category == null) {
        ctx.sendMessage(CommandUtil.msg("[" + cat + "]", CommandUtil.COLOR_GRAY));
      }

      StringBuilder sb = new StringBuilder();
      for (Warp warp : catWarps) {
        if (!sb.isEmpty()) sb.append(", ");
        sb.append(warp.displayName());
      }
      ctx.sendMessage(CommandUtil.msg("  " + sb, CommandUtil.COLOR_WHITE));
    }

    if (category == null) {
      Set<String> categories = warpManager.getCategories();
      if (categories.size() > 1) {
        ctx.sendMessage(CommandUtil.msg("Categories: " + String.join(", ", categories), CommandUtil.COLOR_GRAY));
        ctx.sendMessage(CommandUtil.msg("Use /warps <category> to filter.", CommandUtil.COLOR_GRAY));
      }
    }

    ctx.sendMessage(CommandUtil.msg("Use /warp <name> to teleport.", CommandUtil.COLOR_GRAY));
  }

  private boolean tryOpenGui(@NotNull Store<EntityStore> store,
                              @NotNull Ref<EntityStore> ref,
                              @NotNull PlayerRef playerRef) {
    if (!HyperEssentialsAPI.isAvailable()) return false;

    GuiManager guiManager = HyperEssentialsAPI.getInstance().getGuiManager();
    if (guiManager.getPlayerRegistry().getEntry("warps") == null) return false;

    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) return false;

    return guiManager.openPlayerPage("warps", player, ref, store, playerRef);
  }
}
