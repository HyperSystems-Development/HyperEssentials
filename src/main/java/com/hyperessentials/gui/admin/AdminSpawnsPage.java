package com.hyperessentials.gui.admin;

import com.hyperessentials.data.Spawn;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.HEMessages;
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
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Admin spawns page — list all world spawns with create/delete.
 * Shows every loaded world: custom spawns first, then unconfigured worlds with default badge.
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

  /**
   * Represents a row in the spawn list: either a custom spawn or a default (unconfigured) world.
   */
  private record SpawnEntry(
      Spawn spawn,
      boolean isCustom  // true = saved in SpawnManager, false = default world spawn
  ) {}

  private void buildSpawnList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    // Build combined list: custom spawns + unconfigured worlds
    List<SpawnEntry> entries = new ArrayList<>();

    // 1. Add all custom spawns
    for (Spawn spawn : spawnManager.getAllSpawns()) {
      entries.add(new SpawnEntry(spawn, true));
    }

    // 2. Add unconfigured worlds (worlds without a custom spawn)
    Map<String, World> loadedWorlds = spawnManager.getLoadedWorlds();
    for (Map.Entry<String, World> worldEntry : loadedWorlds.entrySet()) {
      String worldUuid = worldEntry.getKey();
      if (spawnManager.getSpawnForWorld(worldUuid) == null) {
        Spawn defaultSpawn = spawnManager.createDefaultSpawnForWorld(worldEntry.getValue());
        if (defaultSpawn != null) {
          entries.add(new SpawnEntry(defaultSpawn, false));
        }
      }
    }

    // Sort: global first, then custom spawns, then default world spawns, then by name
    entries.sort(Comparator
        .<SpawnEntry, Boolean>comparing(e -> e.spawn().isGlobal(), Comparator.reverseOrder())
        .thenComparing(e -> e.isCustom(), Comparator.reverseOrder())
        .thenComparing(e -> e.spawn().worldName()));

    // Update header count (only count custom spawns)
    long customCount = entries.stream().filter(SpawnEntry::isCustom).count();
    cmd.set("#SpawnCount.Text", customCount + " spawn" + (customCount != 1 ? "s" : "")
        + " / " + entries.size() + " world" + (entries.size() != 1 ? "s" : ""));

    // Create button
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#CreateBtn",
        EventData.of("Button", "Create"), false
    );

    // Build spawn list
    cmd.clear("#SpawnList");
    cmd.appendInline("#SpawnList", "Group #IndexCards { LayoutMode: Top; }");

    if (entries.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", HEMessages.get(playerRef, AdminKeys.Spawns.EMPTY_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, AdminKeys.Spawns.EMPTY_MESSAGE));
      return;
    }

    int i = 0;
    for (SpawnEntry entry : entries) {
      Spawn spawn = entry.spawn();
      boolean isCustom = entry.isCustom();

      cmd.append("#IndexCards", UIPaths.ADMIN_SPAWN_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #SpawnName.Text", UIHelper.formatWorldName(spawn.worldName()));

      // Badge: GLOBAL > DEFAULT (for unconfigured worlds)
      if (spawn.isGlobal()) {
        cmd.set(idx + " #DefaultBadge.Text", HEMessages.get(playerRef, AdminKeys.Spawns.GLOBAL_BADGE));
      } else if (!isCustom) {
        cmd.set(idx + " #DefaultBadge.Text", HEMessages.get(playerRef, AdminKeys.Spawns.DEFAULT_BADGE));
        cmd.set(idx + " #DefaultBadge.Style.TextColor", "#AAAAAA");
      } else {
        cmd.set(idx + " #DefaultBadge.Text", "");
      }

      cmd.set(idx + " #SpawnWorld.Text", UIHelper.formatWorldName(spawn.worldName()));

      if (isCustom) {
        cmd.set(idx + " #SpawnCoords.Text", UIHelper.formatCoords(spawn.x(), spawn.y(), spawn.z()));
      } else {
        cmd.set(idx + " #SpawnCoords.Text", HEMessages.get(playerRef, AdminKeys.Spawns.DEFAULT_COORDS));
      }

      // Indicator bar color: gold for global, green for custom, gray for default
      if (spawn.isGlobal()) {
        cmd.set(idx + " #IndicatorBar.Background.Color", "#FFAA00");
      } else if (isCustom) {
        cmd.set(idx + " #IndicatorBar.Background.Color", "#44cc44");
      } else {
        cmd.set(idx + " #IndicatorBar.Background.Color", "#555555");
      }

      if (isCustom) {
        // "Set Global" button (hidden if already global)
        if (!spawn.isGlobal()) {
          events.addEventBinding(
              CustomUIEventBindingType.Activating,
              idx + " #SetGlobalBtn",
              EventData.of("Button", "SetGlobal").append("Target", spawn.worldUuid()),
              false
          );
        } else {
          // Hide "Set Global" for the current global spawn
          cmd.set(idx + " #SetGlobalBtn.Text", "");
          cmd.set(idx + " #SetGlobalBtn.Disabled", true);
        }

        // Delete button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #DeleteBtn",
            EventData.of("Button", "Delete").append("Target", spawn.worldUuid()),
            false
        );
      } else {
        // For unconfigured worlds, replace Delete with "Set Custom" (creates spawn at world default)
        cmd.set(idx + " #DeleteBtn.Text", HEMessages.get(playerRef, AdminKeys.Spawns.SET_CUSTOM));
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #DeleteBtn",
            EventData.of("Button", "SetCustom").append("Target", spawn.worldUuid()),
            false
        );

        // Hide "Set Global" for unconfigured worlds
        cmd.set(idx + " #SetGlobalBtn.Text", "");
        cmd.set(idx + " #SetGlobalBtn.Disabled", true);
      }

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
      case "SetGlobal" -> handleSetGlobal(data.target);
      case "SetCustom" -> handleSetCustom(data.target);
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

    Spawn spawn = Spawn.create(worldUuidStr, worldName,
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX(),
        playerRef.getUuid().toString());
    spawnManager.setSpawn(spawn);

    rebuildList();
  }

  private void handleDelete(String worldUuid) {
    if (worldUuid == null) return;
    spawnManager.deleteSpawn(worldUuid);
    rebuildList();
  }

  private void handleSetGlobal(String worldUuid) {
    if (worldUuid == null) return;
    spawnManager.setGlobalSpawn(worldUuid);
    rebuildList();
  }

  private void handleSetCustom(String worldUuid) {
    if (worldUuid == null) return;

    // Create a custom spawn from the world's default spawn point
    Map<String, World> loadedWorlds = spawnManager.getLoadedWorlds();
    World world = loadedWorlds.get(worldUuid);
    if (world == null) return;

    Spawn defaultSpawn = spawnManager.createDefaultSpawnForWorld(world);
    if (defaultSpawn == null) return;

    // Save with createdBy = the admin who clicked
    Spawn customSpawn = new Spawn(
        defaultSpawn.worldUuid(), defaultSpawn.worldName(),
        defaultSpawn.x(), defaultSpawn.y(), defaultSpawn.z(),
        defaultSpawn.yaw(), defaultSpawn.pitch(),
        false, System.currentTimeMillis(),
        playerRef.getUuid().toString()
    );
    spawnManager.setSpawn(customSpawn);

    rebuildList();
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildSpawnList(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
