package com.hyperessentials.gui.admin;

import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.module.kits.KitManager;
import com.hyperessentials.module.kits.data.Kit;
import com.hyperessentials.module.kits.data.KitItem;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Admin kits page — list all kits with create from inventory/delete/preview/edit.
 * Supports list, preview, and edit modes.
 */
public class AdminKitsPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final Ref<EntityStore> pageRef;
  private final Store<EntityStore> pageStore;
  private final KitManager kitManager;
  private final GuiManager guiManager;

  /** Name of the kit being previewed, null when not in preview mode. */
  @Nullable
  private String previewingKit;

  /** Name of the kit being edited, null when not in edit mode. */
  @Nullable
  private String editingKit;

  /** Tracks the one-time toggle state during editing. */
  private boolean editOneTimeState;

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
    buildContent(cmd, events);
  }

  private void buildContent(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    if (previewingKit != null) {
      buildPreviewView(cmd, events);
    } else if (editingKit != null) {
      buildEditView(cmd, events);
    } else {
      buildKitList(cmd, events);
    }
  }

  // =====================================================================
  // List Mode
  // =====================================================================

  private void buildKitList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    Collection<Kit> allKits = kitManager.getAllKits();
    cmd.set("#KitCount.Text", allKits.size() + " kit" + (allKits.size() != 1 ? "s" : ""));

    // Set placeholder text on the name input
    cmd.set("#KitNameInput.PlaceholderText",
        HEMessages.get(playerRef, AdminKeys.Kits.NAME_PLACEHOLDER));

    // Create button — reads the name input value
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#CreateBtn",
        EventData.of("Button", "Create")
            .append("@InputName", "#KitNameInput.Value"),
        false
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

      // Preview button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #PreviewBtn",
          EventData.of("Button", "Preview").append("Target", kit.name()),
          false
      );

      // Edit button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #EditBtn",
          EventData.of("Button", "Edit").append("Target", kit.name()),
          false
      );

      // Delete button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #DeleteBtn",
          EventData.of("Button", "Delete").append("Target", kit.name()),
          false
      );

      i++;
    }
  }

  // =====================================================================
  // Preview Mode
  // =====================================================================

  private void buildPreviewView(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    if (previewingKit == null) return;

    Kit kit = kitManager.getKit(previewingKit);
    if (kit == null) {
      previewingKit = null;
      buildKitList(cmd, events);
      return;
    }

    cmd.set("#KitCount.Text", HEMessages.get(playerRef, AdminKeys.Kits.PREVIEW_TITLE));

    // Replace list area with preview template
    cmd.clear("#KitList");
    cmd.appendInline("#KitList", "Group #IndexCards { LayoutMode: Top; }");
    cmd.append("#IndexCards", UIPaths.ADMIN_KIT_PREVIEW);
    String preview = "#IndexCards[0]";

    // Populate header
    cmd.set(preview + " #PreviewTitle.Text", kit.displayName());

    if (kit.cooldownSeconds() > 0) {
      cmd.set(preview + " #PreviewCooldown.Text", formatCooldown(kit.cooldownSeconds()));
    }
    if (kit.oneTime()) {
      cmd.set(preview + " #PreviewOneTime.Text", HEMessages.get(playerRef, AdminKeys.Kits.ONE_TIME));
    }
    if (kit.permission() != null) {
      cmd.set(preview + " #PreviewPermission.Text", kit.permission());
    }

    // Item count
    List<KitItem> items = kit.items();
    cmd.set(preview + " #PreviewItemCount.Text", items.size() + " item" + (items.size() != 1 ? "s" : ""));

    // Build item list
    if (items.isEmpty()) {
      cmd.appendInline(preview + " #PreviewItems",
          "Group { Anchor: (Height: 40); LayoutMode: Top; "
          + "Label { Text: \"" + HEMessages.get(playerRef, AdminKeys.Kits.PREVIEW_NO_ITEMS)
              + "\"; Style: (FontSize: 12, TextColor: #7c8b99, HorizontalAlignment: Center, VerticalAlignment: Center); "
          + "Anchor: (Height: 30); } }");
    } else {
      for (int i = 0; i < items.size(); i++) {
        KitItem item = items.get(i);
        String itemName = formatItemName(item.itemId());

        // Build each item row inline: ItemIcon + name + quantity + section + remove button
        String rowUi = "Group { Anchor: (Height: 34, Bottom: 2); Background: (Color: #141c26); LayoutMode: Left; "
            + "Group { Anchor: (Width: 32, Height: 34); "
            + "ItemIcon #ItemImg { Anchor: (Width: 28, Height: 28, Left: 2, Top: 3); ItemId: \"" + item.itemId() + "\"; } } "
            + "Group { FlexWeight: 1; Padding: (Left: 6, Top: 4, Bottom: 4); LayoutMode: Top; "
            + "Label #ItemName { Text: \"" + escapeUiString(itemName) + "\"; "
            + "Style: (FontSize: 11, TextColor: #FFFFFF, VerticalAlignment: Center); Anchor: (Height: 14); } "
            + "Label #ItemInfo { Text: \"x" + item.quantity() + " [" + item.section() + "]\"; "
            + "Style: (FontSize: 9, TextColor: #7c8b99, VerticalAlignment: Center); Anchor: (Height: 12); } } "
            + "Group { Anchor: (Width: 65); Padding: (Right: 6, Top: 5, Bottom: 5); "
            + "TextButton #RemoveBtn { Text: \"Remove\"; Anchor: (Width: 58, Height: 22); } } }";

        cmd.appendInline(preview + " #PreviewItems", rowUi);
        String rowIdx = preview + " #PreviewItems[" + i + "]";

        // Wire remove button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            rowIdx + " #RemoveBtn",
            EventData.of("Button", "RemoveItem")
                .append("Target", kit.name())
                .append("Value", String.valueOf(i)),
            false
        );
      }
    }

    // Wire back button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        preview + " #PreviewBackBtn",
        EventData.of("Button", "CancelPreview"),
        false
    );
  }

  // =====================================================================
  // Edit Mode
  // =====================================================================

  private void buildEditView(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    if (editingKit == null) return;

    Kit kit = kitManager.getKit(editingKit);
    if (kit == null) {
      editingKit = null;
      buildKitList(cmd, events);
      return;
    }

    cmd.set("#KitCount.Text", HEMessages.get(playerRef, AdminKeys.Kits.EDIT_TITLE));

    // Replace list area with edit template
    cmd.clear("#KitList");
    cmd.appendInline("#KitList", "Group #IndexCards { LayoutMode: Top; }");
    cmd.append("#IndexCards", UIPaths.ADMIN_KIT_EDIT);
    String edit = "#IndexCards[0]";

    // Populate read-only fields
    cmd.set(edit + " #EditName.Text", kit.name());
    cmd.set(edit + " #EditItemCount.Text", String.valueOf(kit.items().size()));

    // Populate labels
    cmd.set(edit + " #EditTitle.Text", HEMessages.get(playerRef, AdminKeys.Kits.EDIT_TITLE));
    cmd.set(edit + " #EditDisplayNameLabel.Text", HEMessages.get(playerRef, AdminKeys.Kits.EDIT_DISPLAY_NAME));
    cmd.set(edit + " #EditCooldownLabel.Text", HEMessages.get(playerRef, AdminKeys.Kits.EDIT_COOLDOWN));
    cmd.set(edit + " #EditPermissionLabel.Text", HEMessages.get(playerRef, AdminKeys.Kits.EDIT_PERMISSION));

    // Populate editable field values
    cmd.set(edit + " #EditDisplayName.Value", kit.displayName());
    cmd.set(edit + " #EditCooldown.Value", String.valueOf(kit.cooldownSeconds()));

    // One-time toggle button text
    editOneTimeState = kit.oneTime();
    cmd.set(edit + " #EditOneTimeToggle.Text", editOneTimeState ? "Yes" : "No");

    if (kit.permission() != null) {
      cmd.set(edit + " #EditPermission.Value", kit.permission());
    }

    // Wire one-time toggle button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        edit + " #EditOneTimeToggle",
        EventData.of("Button", "ToggleOneTime"),
        false
    );

    // Wire save — read field values
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        edit + " #EditSaveBtn",
        EventData.of("Button", "SaveEdit")
            .append("Target", kit.name())
            .append("@InputName", edit + " #EditDisplayName.Value")
            .append("@InputCooldown", edit + " #EditCooldown.Value")
            .append("@InputPermission", edit + " #EditPermission.Value"),
        false
    );

    // Wire cancel/back buttons
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        edit + " #EditBackBtn",
        EventData.of("Button", "CancelEdit"),
        false
    );

    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        edit + " #EditCancelBtn",
        EventData.of("Button", "CancelEdit"),
        false
    );
  }

  // =====================================================================
  // Event Handling
  // =====================================================================

  private String formatCooldown(int seconds) {
    if (seconds < 60) return seconds + "s cooldown";
    if (seconds < 3600) return (seconds / 60) + "m cooldown";
    if (seconds < 86400) return (seconds / 3600) + "h cooldown";
    return (seconds / 86400) + "d cooldown";
  }

  /** Converts an item ID like "Weapon_Sword_Adamantite" to "Weapon Sword Adamantite". */
  private String formatItemName(@NotNull String itemId) {
    return itemId.replace('_', ' ');
  }

  /** Escapes characters that would break .ui inline strings. */
  private String escapeUiString(@NotNull String text) {
    // Remove quotes and dollar signs that would break the .ui parser
    return text.replace("\"", "").replace("$", "");
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
      case "Create" -> handleCreate(data);
      case "Delete" -> handleDelete(data.target);
      case "Preview" -> handlePreview(data.target);
      case "CancelPreview" -> handleCancelPreview();
      case "RemoveItem" -> handleRemoveItem(data.target, data.value);
      case "Edit" -> handleEdit(data.target);
      case "SaveEdit" -> handleSaveEdit(data);
      case "CancelEdit" -> handleCancelEdit();
      case "ToggleOneTime" -> handleToggleOneTime();
      default -> sendUpdate();
    }
  }

  private void handleCreate(@NotNull AdminPageData data) {
    // Use the name from the text input, or fall back to auto-generated
    String name = (data.inputName != null && !data.inputName.isBlank())
        ? data.inputName.trim().toLowerCase().replaceAll("\\s+", "_")
        : "kit_" + System.currentTimeMillis() % 100000;

    kitManager.captureFromInventory(playerRef, pageStore, pageRef, name);
    rebuildContent();
  }

  private void handleDelete(String kitName) {
    if (kitName == null) return;
    kitManager.deleteKit(kitName);
    rebuildContent();
  }

  private void handlePreview(@Nullable String kitName) {
    if (kitName == null) return;
    Kit kit = kitManager.getKit(kitName);
    if (kit == null) return;

    previewingKit = kitName;
    rebuildContent();
  }

  private void handleCancelPreview() {
    previewingKit = null;
    rebuildContent();
  }

  private void handleRemoveItem(@Nullable String kitName, @Nullable String indexStr) {
    if (kitName == null || indexStr == null) return;

    Kit kit = kitManager.getKit(kitName);
    if (kit == null) return;

    int index;
    try {
      index = Integer.parseInt(indexStr);
    } catch (NumberFormatException e) {
      return;
    }

    List<KitItem> items = kit.items();
    if (index < 0 || index >= items.size()) return;

    // Create new list without the removed item
    List<KitItem> newItems = new ArrayList<>(items);
    newItems.remove(index);

    Kit updated = kit.withItems(newItems);
    kitManager.updateKit(updated);

    // Stay in preview mode
    rebuildContent();
  }

  private void handleEdit(@Nullable String kitName) {
    if (kitName == null) return;
    Kit kit = kitManager.getKit(kitName);
    if (kit == null) return;

    editingKit = kitName;
    editOneTimeState = kit.oneTime();
    rebuildContent();
  }

  private void handleSaveEdit(@NotNull AdminPageData data) {
    if (data.target == null || editingKit == null) {
      editingKit = null;
      rebuildContent();
      return;
    }

    Kit kit = kitManager.getKit(data.target);
    if (kit == null) {
      editingKit = null;
      rebuildContent();
      return;
    }

    // Apply edits from input fields
    if (data.inputName != null && !data.inputName.isBlank()) {
      kit = kit.withDisplayName(data.inputName.trim());
    }

    // Parse cooldown
    if (data.inputCooldown != null && !data.inputCooldown.isBlank()) {
      try {
        int cooldown = Integer.parseInt(data.inputCooldown.trim());
        kit = kit.withCooldownSeconds(cooldown);
      } catch (NumberFormatException ignored) {
        // Keep existing cooldown if parse fails
      }
    }

    // Apply one-time toggle state
    kit = kit.withOneTime(editOneTimeState);

    // Permission can be cleared (empty string = null)
    if (data.inputPermission != null) {
      String perm = data.inputPermission.trim();
      kit = kit.withPermission(perm.isEmpty() ? null : perm);
    }

    kitManager.updateKit(kit);

    editingKit = null;
    rebuildContent();
  }

  private void handleCancelEdit() {
    editingKit = null;
    rebuildContent();
  }

  private void handleToggleOneTime() {
    editOneTimeState = !editOneTimeState;
    // Rebuild to reflect the toggle change
    rebuildContent();
  }

  private void rebuildContent() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildContent(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
