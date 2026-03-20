package com.hyperessentials.gui.admin;

import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.module.kits.KitManager;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.HEMessages;
import com.hyperessentials.module.kits.data.Kit;
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
 * Admin kits page — list all kits with create from inventory/delete.
 */
public class AdminKitsPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final Ref<EntityStore> pageRef;
  private final Store<EntityStore> pageStore;
  private final KitManager kitManager;
  private final GuiManager guiManager;

  public AdminKitsPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull Ref<EntityStore> ref,
      @NotNull Store<EntityStore> store,
      @NotNull KitManager kitManager,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.pageRef = ref;
    this.pageStore = store;
    this.kitManager = kitManager;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_KITS);
    NavBarHelper.setupAdminBar(playerRef, "kits", guiManager.getAdminRegistry(), cmd, events);
    buildKitList(cmd, events);
  }

  private void buildKitList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    Collection<Kit> allKits = kitManager.getAllKits();
    cmd.set("#KitCount.Text", allKits.size() + " kit" + (allKits.size() != 1 ? "s" : ""));

    // Create button
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#CreateBtn",
        EventData.of("Button", "Create"), false
    );

    // Sort kits by name
    List<Kit> sorted = new ArrayList<>(allKits);
    sorted.sort(Comparator.comparing(Kit::name));

    cmd.clear("#KitList");
    cmd.appendInline("#KitList", "Group #IndexCards { LayoutMode: Top; }");

    if (sorted.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", HEMessages.get(playerRef, AdminKeys.Kits.EMPTY_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, AdminKeys.Kits.EMPTY_MESSAGE));
      return;
    }

    int i = 0;
    for (Kit kit : sorted) {
      cmd.append("#IndexCards", UIPaths.ADMIN_KIT_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #KitName.Text", kit.displayName());
      cmd.set(idx + " #KitItems.Text", kit.items().size() + " items");

      // Cooldown info
      if (kit.cooldownSeconds() > 0) {
        cmd.set(idx + " #KitCooldown.Text", formatCooldown(kit.cooldownSeconds()));
      }

      // One-time badge
      if (kit.oneTime()) {
        cmd.set(idx + " #KitOneTime.Text", HEMessages.get(playerRef, AdminKeys.Kits.ONE_TIME));
      }

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #DeleteBtn",
          EventData.of("Button", "Delete").append("Target", kit.name()),
          false
      );

      i++;
    }
  }

  private String formatCooldown(int seconds) {
    if (seconds < 60) return seconds + "s cooldown";
    if (seconds < 3600) return (seconds / 60) + "m cooldown";
    if (seconds < 86400) return (seconds / 3600) + "h cooldown";
    return (seconds / 86400) + "d cooldown";
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
    // Capture kit from player's current inventory
    String name = "kit_" + System.currentTimeMillis() % 100000;
    kitManager.captureFromInventory(playerRef, pageStore, pageRef, name);
    rebuildList();
  }

  private void handleDelete(String kitName) {
    if (kitName == null) return;
    kitManager.deleteKit(kitName);
    rebuildList();
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildKitList(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
