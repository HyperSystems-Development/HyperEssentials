package com.hyperessentials.gui.player;

import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.BackEntry;
import com.hyperessentials.data.Location;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.PlayerPageData;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.module.warmup.WarmupTask;
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

import java.util.List;
import java.util.UUID;

/**
 * Back history page — shows all stored back locations with teleport buttons.
 */
public class BackPage extends InteractiveCustomUIPage<PlayerPageData> {

  private final Player player;
  private final PlayerRef playerRef;
  private final BackManager backManager;
  private final WarmupManager warmupManager;
  private final GuiManager guiManager;

  private Ref<EntityStore> lastRef;
  private Store<EntityStore> lastStore;

  public BackPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull BackManager backManager,
      @NotNull WarmupManager warmupManager,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.backManager = backManager;
    this.warmupManager = warmupManager;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    this.lastRef = ref;
    this.lastStore = store;

    cmd.append(UIPaths.BACK_PAGE);
    buildEntryList(cmd, events);
  }

  @Override
  public void onDismiss(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store) {
    // No cleanup needed
  }

  private void buildEntryList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    UUID uuid = playerRef.getUuid();
    List<BackEntry> history = backManager.getBackHistory(uuid);

    cmd.set("#BackCount.Text", history.size() + " location(s)");
    cmd.clear("#BackList");
    cmd.appendInline("#BackList", "Group #IndexCards { LayoutMode: Top; }");

    boolean onCooldown = warmupManager.isOnCooldown(uuid, "teleport", "back");

    for (int i = 0; i < history.size(); i++) {
      BackEntry entry = history.get(i);
      Location loc = entry.location();
      cmd.append("#IndexCards", UIPaths.BACK_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #BackLabel.Text", entry.sourceLabel());
      cmd.set(idx + " #BackWorld.Text", UIHelper.formatWorldName(loc.world()));
      cmd.set(idx + " #BackCoords.Text", UIHelper.formatCoords(loc.x(), loc.y(), loc.z()));

      if (onCooldown) {
        cmd.set(idx + " #TeleportBtn.Disabled", true);
        cmd.set(idx + " #TeleportBtn.Text", "Cooldown");
      }

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #TeleportBtn",
          EventData.of("Button", "Back").append("Target", String.valueOf(i)),
          false
      );
    }

    if (history.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", "No Back Locations");
      cmd.set("#IndexCards[0] #EmptyMessage.Text", "Teleport or die to create a back location.");
    }
  }

  @Override
  public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                              @NotNull PlayerPageData data) {
    super.handleDataEvent(ref, store, data);

    if (!"Back".equals(data.button) || data.target == null) {
      sendUpdate();
      return;
    }

    try {
      int index = Integer.parseInt(data.target);
      handleBackTeleport(ref, index);
    } catch (NumberFormatException e) {
      sendUpdate();
    }
  }

  private void handleBackTeleport(@NotNull Ref<EntityStore> ref, int index) {
    UUID uuid = playerRef.getUuid();

    if (warmupManager.isOnCooldown(uuid, "teleport", "back")) {
      rebuildList();
      return;
    }

    BackEntry entry = backManager.removeBackEntry(uuid, index);
    if (entry == null) {
      rebuildList();
      return;
    }

    Location dest = entry.location();
    WarmupTask task = warmupManager.startWarmup(uuid, "teleport", "back", () -> {
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

    // Close GUI
    player.getPageManager().setPage(ref, lastStore, Page.None);

    if (task != null) {
      playerRef.sendMessage(CommandUtil.info(
          "Teleporting in " + task.warmupSeconds() + "s... Don't move!"));
    }
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildEntryList(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
