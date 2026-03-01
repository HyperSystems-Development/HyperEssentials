package com.hyperessentials.gui.player;

import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.PlayerPageData;
import com.hyperessentials.module.kits.KitManager;
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
import java.util.List;
import java.util.UUID;

/**
 * Player kits page — browse available kits, claim with cooldown, preview items.
 */
public class KitsPage extends InteractiveCustomUIPage<PlayerPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final KitManager kitManager;
  private final GuiManager guiManager;

  private Ref<EntityStore> lastRef;
  private Store<EntityStore> lastStore;

  public KitsPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull KitManager kitManager,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.kitManager = kitManager;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    this.lastRef = ref;
    this.lastStore = store;

    cmd.append(UIPaths.KITS_PAGE);
    NavBarHelper.setupBar(playerRef, "kits", guiManager.getPlayerRegistry(), cmd, events);
    buildKitList(ref, store, cmd, events);
  }

  private void buildKitList(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                            @NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    UUID uuid = playerRef.getUuid();
    List<Kit> kits = kitManager.getAvailableKits(uuid);

    cmd.set("#KitCount.Text", kits.size() + " kits available");

    cmd.clear("#KitList");
    cmd.appendInline("#KitList", "Group #IndexCards { LayoutMode: Top; }");

    if (kits.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", "No Kits");
      cmd.set("#IndexCards[0] #EmptyMessage.Text", "No kits are currently available to you.");
      return;
    }

    List<Kit> sorted = new ArrayList<>(kits);
    sorted.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));

    int i = 0;
    for (Kit kit : sorted) {
      cmd.append("#IndexCards", UIPaths.KIT_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #KitName.Text", kit.displayName());
      cmd.set(idx + " #KitItems.Text", kit.items().size() + " items");

      // Cooldown info
      long remainingMs = kitManager.getRemainingCooldown(uuid, kit.name());
      if (remainingMs > 0) {
        int remainingSecs = (int) (remainingMs / 1000);
        cmd.set(idx + " #KitCooldown.Text", "Cooldown: " + UIHelper.formatDuration(remainingSecs));
      } else if (kit.oneTime()) {
        cmd.set(idx + " #KitCooldown.Text", "One-time kit");
      } else if (kit.cooldownSeconds() > 0) {
        cmd.set(idx + " #KitCooldown.Text", "Ready");
      }

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #ClaimBtn",
          EventData.of("Button", "Claim").append("Target", kit.name()),
          false
      );

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #PreviewBtn",
          EventData.of("Button", "Preview").append("Target", kit.name()),
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
      case "Claim" -> handleClaim(ref, store, data.target);
      case "Preview" -> handlePreview(data.target);
      default -> sendUpdate();
    }
  }

  private void handleClaim(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                           String kitName) {
    if (kitName == null) return;

    UUID uuid = playerRef.getUuid();
    Kit kit = kitManager.getKit(kitName);
    if (kit == null) {
      rebuildList(ref, store);
      return;
    }

    KitManager.ClaimResult result = kitManager.claimKit(uuid, playerRef, store, ref, kit);

    // Rebuild list to update cooldown status after claim attempt
    rebuildList(ref, store);
  }

  private void handlePreview(String kitName) {
    if (kitName == null) return;

    Kit kit = kitManager.getKit(kitName);
    if (kit == null) return;

    // Preview shows kit items in chat (keeps GUI open)
    // The PreviewKitCommand already handles this via text
    // For GUI, we just rebuild with current state
    rebuildList(lastRef, lastStore);
  }

  private void rebuildList(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store) {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildKitList(ref, store, cmd, events);
    sendUpdate(cmd, events, false);
  }
}
