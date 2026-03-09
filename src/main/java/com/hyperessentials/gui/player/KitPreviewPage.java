package com.hyperessentials.gui.player;

import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.PlayerPageData;
import com.hyperessentials.module.kits.KitManager;
import com.hyperessentials.module.kits.data.Kit;
import com.hyperessentials.module.kits.data.KitItem;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kit preview page — shows kit items with icons, grouped by inventory section.
 * Sub-page opened from KitsPage (no NavBar).
 */
public class KitPreviewPage extends InteractiveCustomUIPage<PlayerPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final KitManager kitManager;
  private final Kit kit;
  private final Runnable onBack;

  public KitPreviewPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull KitManager kitManager,
      @NotNull Kit kit,
      @NotNull Runnable onBack
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.kitManager = kitManager;
    this.kit = kit;
    this.onBack = onBack;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.KIT_PREVIEW_PAGE);

    cmd.set("#KitTitle.Text", kit.displayName());
    cmd.set("#KitDescription.Text", kit.items().size() + " items"
        + (kit.oneTime() ? " (one-time)" : kit.cooldownSeconds() > 0
            ? " (cooldown: " + UIHelper.formatDuration(kit.cooldownSeconds()) + ")" : ""));

    buildItemList(cmd);
    buildClaimButton(cmd, events, ref, store);

    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#BackBtn",
        EventData.of("Button", "Back"), false
    );
  }

  private void buildItemList(@NotNull UICommandBuilder cmd) {
    cmd.clear("#ItemList");
    cmd.appendInline("#ItemList", "Group #IndexCards { LayoutMode: Top; }");

    if (kit.items().isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", "Empty Kit");
      cmd.set("#IndexCards[0] #EmptyMessage.Text", "This kit has no items.");
      return;
    }

    // Group items by section, preserving order: armor, hotbar, storage, utility
    Map<String, List<KitItem>> grouped = kit.items().stream()
        .collect(Collectors.groupingBy(KitItem::section, LinkedHashMap::new, Collectors.toList()));

    // Sort sections in a logical order
    String[] sectionOrder = { KitItem.ARMOR, KitItem.HOTBAR, KitItem.STORAGE, KitItem.UTILITY };

    int i = 0;
    for (String section : sectionOrder) {
      List<KitItem> items = grouped.get(section);
      if (items == null || items.isEmpty()) continue;

      // Section header
      cmd.appendInline("#IndexCards", "Group { Anchor: (Height: 22, Top: " + (i > 0 ? "6" : "0") + "); "
          + "Label { Text: \"" + formatSectionName(section) + "\"; "
          + "Style: (FontSize: 11, TextColor: #44cc44, RenderBold: true, VerticalAlignment: Center); } }");
      i++;

      for (KitItem item : items) {
        cmd.append("#IndexCards", UIPaths.KIT_PREVIEW_ITEM);
        String idx = "#IndexCards[" + i + "]";

        cmd.set(idx + " #ItemIconEl.ItemId", item.itemId());
        cmd.set(idx + " #ItemName.Text", formatItemName(item.itemId()));
        cmd.set(idx + " #ItemQty.Text", "x" + item.quantity());
        cmd.set(idx + " #ItemSection.Text", formatSectionName(item.section()));

        i++;
      }
    }
  }

  private void buildClaimButton(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events,
                                @NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store) {
    UUID uuid = playerRef.getUuid();

    // Check cooldown and one-time claim status
    long remainingMs = kitManager.getRemainingCooldown(uuid, kit.name());
    boolean oneTimeClaimed = kit.oneTime() && kitManager.hasClaimedOneTimeKit(uuid, kit.name());

    if (remainingMs > 0) {
      int secs = (int) (remainingMs / 1000);
      cmd.set("#ClaimBtn.Disabled", true);
      cmd.set("#ClaimBtn.Text", UIHelper.formatDuration(secs));
    } else if (oneTimeClaimed) {
      cmd.set("#ClaimBtn.Disabled", true);
      cmd.set("#ClaimBtn.Text", "CLAIMED");
    } else {
      events.addEventBinding(
          CustomUIEventBindingType.Activating, "#ClaimBtn",
          EventData.of("Button", "Claim"), false
      );
    }
  }

  @Override
  public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                              @NotNull PlayerPageData data) {
    super.handleDataEvent(ref, store, data);

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "Back" -> onBack.run();
      case "Claim" -> {
        kitManager.claimKit(playerRef.getUuid(), playerRef, store, ref, kit);
        // Rebuild to update button state
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder events = new UIEventBuilder();
        buildClaimButton(cmd, events, ref, store);
        sendUpdate(cmd, events, false);
      }
      default -> sendUpdate();
    }
  }

  private static String formatSectionName(String section) {
    return switch (section) {
      case KitItem.ARMOR -> "Armor";
      case KitItem.HOTBAR -> "Hotbar";
      case KitItem.STORAGE -> "Storage";
      case KitItem.UTILITY -> "Utility";
      default -> section;
    };
  }

  private static String formatItemName(String itemId) {
    // Convert "Weapon_Sword_Adamantite" → "Sword Adamantite"
    // Strip category prefix if present
    String name = itemId;
    int lastUnderscore = name.indexOf('_');
    if (lastUnderscore > 0 && lastUnderscore < name.length() - 1) {
      name = name.substring(lastUnderscore + 1);
    }
    return name.replace('_', ' ');
  }
}
