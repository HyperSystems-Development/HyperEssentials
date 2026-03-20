package com.hyperessentials.gui.admin;

import com.hyperessentials.data.Warp;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.module.warps.WarpManager;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.HEMessages;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Admin warps page — list all warps with create/delete.
 */
public class AdminWarpsPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final WarpManager warpManager;
  private final GuiManager guiManager;

  public AdminWarpsPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull WarpManager warpManager,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.warpManager = warpManager;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_WARPS);
    NavBarHelper.setupAdminBar(playerRef, "warps", guiManager.getAdminRegistry(), cmd, events);
    buildWarpList(cmd, events);
  }

  private void buildWarpList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    Collection<Warp> allWarps = warpManager.getAllWarps();
    cmd.set("#WarpCount.Text", allWarps.size() + " warp" + (allWarps.size() != 1 ? "s" : ""));

    // Create button
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#CreateBtn",
        EventData.of("Button", "Create"), false
    );

    // Build warp list sorted by category then name
    List<Warp> sorted = new ArrayList<>(allWarps);
    sorted.sort(Comparator.comparing(Warp::category).thenComparing(Warp::name));

    cmd.clear("#WarpList");
    cmd.appendInline("#WarpList", "Group #IndexCards { LayoutMode: Top; }");

    if (sorted.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", HEMessages.get(playerRef, AdminKeys.Warps.EMPTY_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, AdminKeys.Warps.EMPTY_MESSAGE));
      return;
    }

    int i = 0;
    for (Warp warp : sorted) {
      cmd.append("#IndexCards", UIPaths.ADMIN_WARP_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #WarpName.Text", warp.displayName());
      cmd.set(idx + " #WarpCategory.Text", warp.category());
      cmd.set(idx + " #WarpWorld.Text", UIHelper.formatWorldName(warp.world()));
      cmd.set(idx + " #WarpCoords.Text", UIHelper.formatCoords(warp.x(), warp.y(), warp.z()));

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #DeleteBtn",
          EventData.of("Button", "Delete").append("Target", warp.name()),
          false
      );

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

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "Create" -> handleCreate();
      case "Delete" -> handleDelete(data.target);
      default -> sendUpdate();
    }
  }

  private void handleCreate() {
    var pos = playerRef.getTransform().getPosition();
    var rot = playerRef.getTransform().getRotation();
    String worldName = "";
    String worldUuidStr = "";

    var worldUuid = playerRef.getWorldUuid();
    if (worldUuid != null) {
      worldUuidStr = worldUuid.toString();
      var world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldUuid);
      if (world != null) {
        worldName = world.getName();
      }
    }

    String name = "warp_" + System.currentTimeMillis() % 100000;
    Warp warp = Warp.create(name, worldName, worldUuidStr,
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX(),
        playerRef.getUuid().toString());
    warpManager.setWarp(warp);

    rebuildList();
  }

  private void handleDelete(String warpName) {
    if (warpName == null) return;
    warpManager.deleteWarp(warpName);
    rebuildList();
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildWarpList(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
