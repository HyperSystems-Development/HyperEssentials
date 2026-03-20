package com.hyperessentials.module.homes.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Home;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.integration.HyperFactionsIntegration;
import com.hyperessentials.module.homes.HomeManager;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

/**
 * /homes - List all homes with count and limit.
 * Opens GUI page when available, falls back to text list.
 */
public class HomesCommand extends AbstractPlayerCommand {

  private final HomeManager homeManager;

  public HomesCommand(@NotNull HomeManager homeManager) {
    super("homes", "List your homes");
    this.homeManager = homeManager;
    addAliases("listhomes", "homelist");
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
                          @NotNull Store<EntityStore> store,
                          @NotNull Ref<EntityStore> ref,
                          @NotNull PlayerRef playerRef,
                          @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.HOME_LIST)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Home.LIST_NO_PERMISSION));
      return;
    }

    // Try GUI page first
    if (tryOpenGui(store, ref, playerRef)) {
      return;
    }

    // Text fallback
    Collection<Home> homes = homeManager.getHomes(uuid);
    int count = homes.size();
    int limit = homeManager.getHomeLimit(uuid);

    String limitStr = limit < 0 ? "unlimited" : String.valueOf(limit);

    if (homes.isEmpty()) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Home.LIST_EMPTY, HEMessageUtil.COLOR_YELLOW, "0/" + limitStr));
      return;
    }

    ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Home.LIST_HEADER, HEMessageUtil.COLOR_YELLOW, count + "/" + limitStr));

    // Show faction home if available
    if (HyperFactionsIntegration.isAvailable() && HyperFactionsIntegration.hasFactionHome(uuid)) {
      String factionWorld = HyperFactionsIntegration.getFactionHomeWorld(uuid);
      double[] coords = HyperFactionsIntegration.getFactionHomeCoords(uuid);
      if (factionWorld != null && coords != null) {
        ctx.sendMessage(HEMessageUtil.text(playerRef, CommandKeys.Home.LIST_FACTION_HOME, HEMessageUtil.COLOR_GOLD,
            factionWorld, String.format("%.0f, %.0f, %.0f", coords[0], coords[1], coords[2])));
      }
    }

    StringBuilder sb = new StringBuilder();
    for (Home home : homes) {
      if (!sb.isEmpty()) sb.append(", ");
      sb.append(home.name());
    }
    ctx.sendMessage(HEMessageUtil.text(sb.toString(), HEMessageUtil.COLOR_GRAY));
  }

  private boolean tryOpenGui(@NotNull Store<EntityStore> store,
                              @NotNull Ref<EntityStore> ref,
                              @NotNull PlayerRef playerRef) {
    if (!HyperEssentialsAPI.isAvailable()) return false;

    GuiManager guiManager = HyperEssentialsAPI.getInstance().getGuiManager();
    if (guiManager.getPlayerRegistry().getEntry("homes") == null) return false;

    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) return false;

    return guiManager.openPlayerPage("homes", player, ref, store, playerRef);
  }
}
