package com.hyperessentials.gui.admin;

import com.hyperessentials.data.Spawn;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.module.spawns.SpawnManager;
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
 * Admin spawns page — list all spawns with create/delete.
 */
public class AdminSpawnsPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final SpawnManager spawnManager;
  private final GuiManager guiManager;

  public AdminSpawnsPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull SpawnManager spawnManager,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.spawnManager = spawnManager;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_SPAWNS);
    NavBarHelper.setupAdminBar(playerRef, "spawns", guiManager.getAdminRegistry(), cmd, events);
    buildSpawnList(cmd, events);
  }

  private void buildSpawnList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    Collection<Spawn> allSpawns = spawnManager.getAllSpawns();
    cmd.set("#SpawnCount.Text", allSpawns.size() + " spawn" + (allSpawns.size() != 1 ? "s" : ""));

    // Create button
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#CreateBtn",
        EventData.of("Button", "Create"), false
    );

    // Build spawn list sorted by default status then name
    List<Spawn> sorted = new ArrayList<>(allSpawns);
    sorted.sort(Comparator.comparing(Spawn::isDefault).reversed().thenComparing(Spawn::name));

    cmd.clear("#SpawnList");
    cmd.appendInline("#SpawnList", "Group #IndexCards { LayoutMode: Top; }");

    if (sorted.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", "No Spawns");
      cmd.set("#IndexCards[0] #EmptyMessage.Text", "Set a spawn at your current location.");
      return;
    }

    int i = 0;
    for (Spawn spawn : sorted) {
      cmd.append("#IndexCards", UIPaths.ADMIN_SPAWN_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #SpawnName.Text", spawn.name());
      cmd.set(idx + " #DefaultBadge.Text", spawn.isDefault() ? "DEFAULT" : "");
      cmd.set(idx + " #SpawnWorld.Text", UIHelper.formatWorldName(spawn.world()));
      cmd.set(idx + " #SpawnCoords.Text", UIHelper.formatCoords(spawn.x(), spawn.y(), spawn.z()));

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #DeleteBtn",
          EventData.of("Button", "Delete").append("Target", spawn.name()),
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

    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      var worldUuid = playerRef.getWorldUuid();
      if (worldUuid != null) {
        var world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldUuid);
        if (world != null) {
          worldName = world.getName();
        }
      }
    }

    String name = "spawn_" + System.currentTimeMillis() % 100000;
    Spawn spawn = Spawn.create(name, worldName,
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX(),
        playerRef.getUuid().toString());
    spawnManager.setSpawn(spawn);

    rebuildList();
  }

  private void handleDelete(String spawnName) {
    if (spawnName == null) return;
    spawnManager.deleteSpawn(spawnName);
    rebuildList();
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildSpawnList(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
