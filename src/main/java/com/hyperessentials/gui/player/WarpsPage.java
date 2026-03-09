package com.hyperessentials.gui.player;

import com.hyperessentials.data.Location;
import com.hyperessentials.data.Warp;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.integration.FactionTerritoryChecker;
import com.hyperessentials.integration.HyperFactionsIntegration;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.RefreshablePage;
import com.hyperessentials.gui.data.PlayerPageData;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.module.warps.WarpManager;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Player warps page — browse warps grouped by category, teleport to warps.
 */
public class WarpsPage extends InteractiveCustomUIPage<PlayerPageData> implements RefreshablePage {

  private final PlayerRef playerRef;
  private final Player player;
  private final WarpManager warpManager;
  private final WarmupManager warmupManager;
  private final GuiManager guiManager;

  private Ref<EntityStore> lastRef;
  private Store<EntityStore> lastStore;

  public WarpsPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull WarpManager warpManager,
      @NotNull WarmupManager warmupManager,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.warpManager = warpManager;
    this.warmupManager = warmupManager;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    this.lastRef = ref;
    this.lastStore = store;

    cmd.append(UIPaths.WARPS_PAGE);
    NavBarHelper.setupBar(playerRef, "warps", guiManager.getPlayerRegistry(), cmd, events);
    buildWarpList(cmd, events);

    guiManager.getPageTracker().register(playerRef.getUuid(), "warps", this);
  }

  @Override
  public void onDismiss(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store) {
    guiManager.getPageTracker().unregister(playerRef.getUuid());
  }

  @Override
  public void refreshContent() {
    rebuildList();
  }

  private void buildWarpList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    UUID uuid = playerRef.getUuid();
    List<Warp> warps = warpManager.getAccessibleWarps(uuid);

    cmd.set("#WarpCount.Text", warps.size() + " warps available");

    cmd.clear("#WarpList");
    cmd.appendInline("#WarpList", "Group #IndexCards { LayoutMode: Top; }");

    if (warps.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", "No Warps");
      cmd.set("#IndexCards[0] #EmptyMessage.Text", "No warps are currently available.");
      return;
    }

    // Group by category
    Map<String, List<Warp>> grouped = warps.stream()
        .collect(Collectors.groupingBy(Warp::category, LinkedHashMap::new, Collectors.toList()));

    // Sort each category's warps alphabetically
    for (List<Warp> catWarps : grouped.values()) {
      catWarps.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
    }

    // Cooldown is module-level (same for all warps) — check once
    int cooldownSecs = warmupManager.getRemainingCooldown(uuid, "warps", "warp");

    int i = 0;
    for (Map.Entry<String, List<Warp>> entry : grouped.entrySet()) {
      String category = entry.getKey();
      List<Warp> catWarps = entry.getValue();

      // Category header
      cmd.append("#IndexCards", UIPaths.WARP_CATEGORY_HEADER);
      String catIdx = "#IndexCards[" + i + "]";
      cmd.set(catIdx + " #CategoryName.Text", category.substring(0, 1).toUpperCase() + category.substring(1));
      i++;

      // Warp entries
      for (Warp warp : catWarps) {
        cmd.append("#IndexCards", UIPaths.WARP_ENTRY);
        String idx = "#IndexCards[" + i + "]";

        cmd.set(idx + " #WarpName.Text", warp.displayName());
        cmd.set(idx + " #WarpCategory.Text", warp.category());
        cmd.set(idx + " #WarpWorld.Text", UIHelper.formatWorldName(warp.world()));
        cmd.set(idx + " #WarpCoords.Text", UIHelper.formatCoords(warp.x(), warp.y(), warp.z()));

        // Zone flag check (warp destination)
        boolean warpZoneBlocked = FactionTerritoryChecker.checkZoneFlag(
            warp.world(), warp.x(), warp.z(), HyperFactionsIntegration.FLAG_WARPS)
            != FactionTerritoryChecker.Result.ALLOWED;

        // Disable teleport button if zone-restricted or on cooldown
        if (warpZoneBlocked) {
          cmd.set(idx + " #TeleportBtn.Disabled", true);
          cmd.set(idx + " #TeleportBtn.Text", "Zone Restricted");
        } else if (cooldownSecs > 0) {
          cmd.set(idx + " #TeleportBtn.Disabled", true);
          cmd.set(idx + " #TeleportBtn.Text", UIHelper.formatDuration(cooldownSecs));
        }

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #TeleportBtn",
            EventData.of("Button", "Teleport").append("Target", warp.name()),
            false
        );

        i++;
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

    if (data.button == null) {
      sendUpdate();
      return;
    }

    if ("Teleport".equals(data.button)) {
      handleTeleport(ref, data.target);
    } else {
      sendUpdate();
    }
  }

  private void handleTeleport(@NotNull Ref<EntityStore> ref, String warpName) {
    if (warpName == null) return;

    UUID uuid = playerRef.getUuid();
    Warp warp = warpManager.getWarp(warpName);
    if (warp == null) {
      rebuildList();
      return;
    }

    // Zone flag check (warp destination)
    if (FactionTerritoryChecker.checkZoneFlag(warp.world(), warp.x(), warp.z(),
        HyperFactionsIntegration.FLAG_WARPS) != FactionTerritoryChecker.Result.ALLOWED) {
      rebuildList();
      return;
    }

    if (warmupManager.isOnCooldown(uuid, "warps", "warp")) {
      rebuildList();
      return;
    }

    Location dest = Location.fromWarp(warp);
    warmupManager.startWarmup(uuid, "warps", "warp", () -> {
      World targetWorld = Universe.get().getWorld(UUID.fromString(dest.worldUuid()));
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

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildWarpList(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
