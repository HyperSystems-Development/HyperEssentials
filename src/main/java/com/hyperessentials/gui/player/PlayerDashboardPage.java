package com.hyperessentials.gui.player;

import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.RefreshablePage;
import com.hyperessentials.gui.data.PlayerPageData;
import com.hyperessentials.module.homes.HomeManager;
import com.hyperessentials.util.GuiKeys;
import com.hyperessentials.util.HEMessages;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.module.utility.UtilityManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Player dashboard page — welcome screen with stat cards and quick actions.
 */
public class PlayerDashboardPage extends InteractiveCustomUIPage<PlayerPageData> implements RefreshablePage {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault());

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;
  @Nullable private final HomeManager homeManager;
  @Nullable private final TpaManager tpaManager;
  @Nullable private final UtilityManager utilityManager;

  public PlayerDashboardPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager,
      @Nullable HomeManager homeManager,
      @Nullable TpaManager tpaManager,
      @Nullable UtilityManager utilityManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.homeManager = homeManager;
    this.tpaManager = tpaManager;
    this.utilityManager = utilityManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.PLAYER_DASHBOARD);
    NavBarHelper.setupBar(playerRef, "dashboard", guiManager.getPlayerRegistry(), cmd, events);
    populateDashboard(cmd, events);

    guiManager.getPageTracker().register(playerRef.getUuid(), "dashboard", this);
  }

  @Override
  public void onDismiss(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store) {
    guiManager.getPageTracker().unregister(playerRef.getUuid());
  }

  @Override
  public void refreshContent() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    populateDashboard(cmd, events);
    sendUpdate(cmd, events, false);
  }

  private void populateDashboard(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    UUID uuid = playerRef.getUuid();

    // Welcome text
    cmd.set("#WelcomeText.Text", HEMessages.get(playerRef, GuiKeys.Dashboard.WELCOME, playerRef.getUsername()));

    // Stat cards
    if (homeManager != null) {
      int homeCount = homeManager.getHomes(uuid).size();
      cmd.set("#HomesCount.Text", String.valueOf(homeCount));
    }

    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      int onlineCount = plugin.getTrackedPlayers().size();
      cmd.set("#OnlineCount.Text", String.valueOf(onlineCount));
    }

    if (tpaManager != null) {
      int tpaCount = tpaManager.getIncomingRequests(uuid).size();
      cmd.set("#TpaCount.Text", String.valueOf(tpaCount));
    }

    // Quick action buttons
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#HomesBtn",
        EventData.of("Button", "NavDirect").append("Target", "homes"), false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#WarpsBtn",
        EventData.of("Button", "NavDirect").append("Target", "warps"), false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#KitsBtn",
        EventData.of("Button", "NavDirect").append("Target", "kits"), false
    );

    // Player stats
    if (utilityManager != null) {
      long playtimeMs = utilityManager.getTotalPlaytimeMs(uuid);
      cmd.set("#PlaytimeLabel.Text", HEMessages.get(playerRef, GuiKeys.Dashboard.PLAYTIME, UIHelper.formatPlaytime(playtimeMs)));

      Instant firstJoin = utilityManager.getFirstJoin(uuid);
      if (firstJoin != null) {
        cmd.set("#JoinDateLabel.Text", HEMessages.get(playerRef, GuiKeys.Dashboard.FIRST_JOINED, DATE_FORMAT.format(firstJoin)));
      }
    }
  }

  @Override
  public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                              @NotNull PlayerPageData data) {
    super.handleDataEvent(ref, store, data);

    if ("Nav".equals(data.button)) {
      NavBarHelper.handleNavEvent(
          data.navTarget != null ? data.navTarget : "",
          player, ref, store, playerRef, guiManager, GuiType.PLAYER
      );
      return;
    }

    if ("NavDirect".equals(data.button) && data.target != null) {
      guiManager.openPlayerPage(data.target, player, ref, store, playerRef);
      return;
    }

    sendUpdate();
  }
}
