package com.hyperessentials.gui.admin;

import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.HEMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin players page — list online players with status info.
 */
public class AdminPlayersPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;

  public AdminPlayersPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_PLAYERS);
    NavBarHelper.setupAdminBar(playerRef, "players", guiManager.getAdminRegistry(), cmd, events);
    buildPlayerList(cmd);
  }

  private void buildPlayerList(@NotNull UICommandBuilder cmd) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    Map<UUID, PlayerRef> tracked = plugin != null ? plugin.getTrackedPlayers() : Map.of();

    cmd.set("#PlayerCount.Text", tracked.size() + " player" + (tracked.size() != 1 ? "s" : "") + " online");

    // Sort players by username
    List<PlayerRef> sorted = new ArrayList<>(tracked.values());
    sorted.sort(Comparator.comparing(PlayerRef::getUsername, String.CASE_INSENSITIVE_ORDER));

    cmd.clear("#PlayerList");
    cmd.appendInline("#PlayerList", "Group #IndexCards { LayoutMode: Top; }");

    if (sorted.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", HEMessages.get(playerRef, AdminKeys.Players.EMPTY_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, AdminKeys.Players.EMPTY_MESSAGE));
      return;
    }

    int i = 0;
    for (PlayerRef p : sorted) {
      cmd.append("#IndexCards", UIPaths.ADMIN_PLAYER_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #PlayerName.Text", p.getUsername());
      cmd.set(idx + " #PlayerInfo.Text", HEMessages.get(playerRef, AdminKeys.Players.PLAYER_INFO, p.getUuid().toString().substring(0, 8)));

      i++;
    }
  }

  @Override
  public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                              @NotNull AdminPageData data) {
    super.handleDataEvent(ref, store, data);

    if ("Nav".equals(data.button)) {
      NavBarHelper.handleNavEvent(
          data.navTarget != null ? data.navTarget : "",
          player, ref, store, playerRef, guiManager, GuiType.ADMIN
      );
      return;
    }

    sendUpdate();
  }
}
