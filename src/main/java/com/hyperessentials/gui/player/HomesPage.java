package com.hyperessentials.gui.player;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Home;
import com.hyperessentials.data.Location;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.PlayerPageData;
import com.hyperessentials.module.homes.HomeManager;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Player homes page — browse, teleport to, and delete homes.
 */
public class HomesPage extends InteractiveCustomUIPage<PlayerPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final HomeManager homeManager;
  private final WarmupManager warmupManager;
  private final GuiManager guiManager;

  private Ref<EntityStore> lastRef;
  private Store<EntityStore> lastStore;

  public HomesPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull HomeManager homeManager,
      @NotNull WarmupManager warmupManager,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.homeManager = homeManager;
    this.warmupManager = warmupManager;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    this.lastRef = ref;
    this.lastStore = store;

    cmd.append(UIPaths.HOMES_PAGE);
    NavBarHelper.setupBar(playerRef, "homes", guiManager.getPlayerRegistry(), cmd, events);
    buildHomeList(cmd, events);
  }

  private void buildHomeList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    UUID uuid = playerRef.getUuid();
    Collection<Home> homes = homeManager.getHomes(uuid);
    int limit = homeManager.getHomeLimit(uuid);

    cmd.set("#HomeCount.Text", homes.size() + " / " + UIHelper.formatLimit(limit) + " homes");

    cmd.clear("#HomeList");
    cmd.appendInline("#HomeList", "Group #IndexCards { LayoutMode: Top; }");

    if (homes.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", "No Homes");
      cmd.set("#IndexCards[0] #EmptyMessage.Text", "Use /sethome to create your first home.");
      return;
    }

    List<Home> sorted = new ArrayList<>(homes);
    sorted.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

    int i = 0;
    for (Home home : sorted) {
      cmd.append("#IndexCards", UIPaths.HOME_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #HomeName.Text", home.name());
      cmd.set(idx + " #HomeWorld.Text", UIHelper.formatWorldName(home.world()));
      cmd.set(idx + " #HomeCoords.Text", UIHelper.formatCoords(home.x(), home.y(), home.z()));

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #TeleportBtn",
          EventData.of("Button", "Teleport").append("Target", home.name()),
          false
      );

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #DeleteBtn",
          EventData.of("Button", "Delete").append("Target", home.name()),
          false
      );

      i++;
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

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "Teleport" -> handleTeleport(ref, data.target);
      case "Delete" -> handleDelete(ref, store, data.target);
      default -> sendUpdate();
    }
  }

  private void handleTeleport(@NotNull Ref<EntityStore> ref, String homeName) {
    if (homeName == null) return;

    UUID uuid = playerRef.getUuid();
    Home home = homeManager.getHome(uuid, homeName);
    if (home == null) {
      rebuildList();
      return;
    }

    if (warmupManager.isOnCooldown(uuid, "homes", "home")) {
      rebuildList();
      return;
    }

    Location dest = Location.fromHome(home);
    warmupManager.startWarmup(uuid, "homes", "home", () -> {
      World targetWorld = Universe.get().getWorld(dest.world());
      if (targetWorld == null) return;
      targetWorld.execute(() -> {
        if (!ref.isValid()) return;
        Store<EntityStore> s = ref.getStore();
        Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
        Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
        Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
        s.addComponent(ref, Teleport.getComponentType(), teleport);
      });
    });

    // Close GUI after starting teleport
    player.getPageManager().setPage(ref, lastStore, Page.None);
  }

  private void handleDelete(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                            String homeName) {
    if (homeName == null) return;

    UUID uuid = playerRef.getUuid();
    boolean deleted = homeManager.deleteHome(uuid, homeName);
    if (deleted) {
      rebuildList();
    }
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildHomeList(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
