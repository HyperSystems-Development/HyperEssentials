package com.hyperessentials.gui.admin;

import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.integration.HyperPermsProviderAdapter;
import com.hyperessentials.integration.PermissionManager;
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
 * Supports list, preview, edit, permission management, and create modal modes.
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

  /** Permission node being managed, null when not in permission mode. */
  @Nullable
  private String managingPermission;

  /** Whether the create modal is showing. */
  private boolean showingCreateModal;

  /** Current search filter text. */
  @Nullable
  private String searchFilter;

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
    if (managingPermission != null) {
      buildPermissionView(cmd, events);
    } else if (previewingKit != null) {
      buildPreviewView(cmd, events);
    } else if (editingKit != null) {
      buildEditView(cmd, events);
    } else if (showingCreateModal) {
      buildCreateModal(cmd, events);
    } else {
      buildKitList(cmd, events);
    }
  }

  // =====================================================================
  // List Mode
  // =====================================================================

  private void buildKitList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    Collection<Kit> allKits = kitManager.getAllKits();

    // Set search placeholder
    cmd.set("#SearchInput.PlaceholderText",
        HEMessages.get(playerRef, AdminKeys.Kits.SEARCH_PLACEHOLDER));

    // Create button — opens the create modal, captures search text
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#CreateBtn",
        EventData.of("Button", "ShowCreateModal")
            .append("@SearchInput", "#SearchInput.Value"),
        false
    );

    // Sort kits by name
    List<Kit> sorted = new ArrayList<>(allKits);
    sorted.sort(Comparator.comparing(Kit::name));

    // Apply search filter
    if (searchFilter != null && !searchFilter.isBlank()) {
      String filter = searchFilter.toLowerCase();
      sorted.removeIf(k -> !k.name().toLowerCase().contains(filter)
          && !k.displayName().toLowerCase().contains(filter));
    }

    cmd.set("#KitCount.Text", sorted.size() + " kit" + (sorted.size() != 1 ? "s" : ""));

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
          EventData.of("Button", "Preview").append("Target", kit.name())
              .append("@SearchInput", "#SearchInput.Value"),
          false
      );

      // Edit button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #EditBtn",
          EventData.of("Button", "Edit").append("Target", kit.name())
              .append("@SearchInput", "#SearchInput.Value"),
          false
      );

      // Delete button
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #DeleteBtn",
          EventData.of("Button", "Delete").append("Target", kit.name())
              .append("@SearchInput", "#SearchInput.Value"),
          false
      );

      i++;
    }
  }

  // =====================================================================
  // Create Modal
  // =====================================================================

  private void buildCreateModal(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    cmd.set("#KitCount.Text", HEMessages.get(playerRef, AdminKeys.Kits.CREATE_TITLE));

    // Replace list area with modal content
    cmd.clear("#KitList");
    cmd.appendInline("#KitList", "Group #IndexCards { LayoutMode: Top; }");

    // Build a simple create form inline
    String modalUi =
        "Group { Anchor: (Height: 200); LayoutMode: Top; Padding: (Left: 40, Right: 40, Top: 30); "
        // Title
        + "Label { Text: \"" + HEMessages.get(playerRef, AdminKeys.Kits.CREATE_TITLE)
        + "\"; Style: (FontSize: 16, TextColor: #FFFFFF, HorizontalAlignment: Center); Anchor: (Height: 28, Bottom: 20); } "
        // Name label
        + "Label { Text: \"" + HEMessages.get(playerRef, AdminKeys.Kits.NAME_PLACEHOLDER)
        + "\"; Style: (FontSize: 12, TextColor: #7c8b99); Anchor: (Height: 18, Bottom: 4); } "
        // Name input
        + "Group { Anchor: (Height: 30, Bottom: 20); Background: (Color: #0d1520); Padding: (Left: 6, Right: 6); "
        + "TextField #ModalNameInput { Anchor: (Height: 26); Style: (FontSize: 12, TextColor: #ffffff); } } "
        // Button row
        + "Group { Anchor: (Height: 32); LayoutMode: Left; "
        + "Group { FlexWeight: 1; } "
        + "TextButton #ModalCancelBtn { Text: \"Cancel\"; Anchor: (Width: 90, Height: 28); } "
        + "Group { Anchor: (Width: 8); } "
        + "TextButton #ModalCreateBtn { Text: \"Create\"; Anchor: (Width: 90, Height: 28); } "
        + "Group { FlexWeight: 1; } } }";

    cmd.appendInline("#IndexCards", modalUi);

    // Wire create button — reads the name input value
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#IndexCards[0] #ModalCreateBtn",
        EventData.of("Button", "Create")
            .append("@InputName", "#IndexCards[0] #ModalNameInput.Value"),
        false
    );

    // Wire cancel button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        "#IndexCards[0] #ModalCancelBtn",
        EventData.of("Button", "CancelCreate"),
        false
    );
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
    cmd.set(edit + " #EditOneTimeToggle.Text", HEMessages.get(playerRef, editOneTimeState ? AdminKeys.Common.YES : AdminKeys.Common.NO));

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

    // Show "Permissions" button only when HyperPerms is available
    HyperPermsProviderAdapter adapter = PermissionManager.get().getHyperPermsAdapter();
    if (adapter != null) {
      // Determine the permission node for this kit
      String permNode = kit.permission() != null ? kit.permission() : "hyperessentials.kit." + kit.name();

      // Add Permissions button after save/cancel row
      cmd.appendInline("#IndexCards",
          "Group { Anchor: (Height: 34, Top: 6); LayoutMode: Left; "
          + "Group { FlexWeight: 1; } "
          + "TextButton #PermBtn { Text: \"Permissions\"; "
          + "Anchor: (Width: 120, Height: 28); } "
          + "Group { FlexWeight: 1; } }");

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#IndexCards[1] #PermBtn",
          EventData.of("Button", "ManagePerms").append("Target", permNode),
          false
      );
    }
  }

  // =====================================================================
  // Permission Management Mode
  // =====================================================================

  private void buildPermissionView(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    if (managingPermission == null) return;

    HyperPermsProviderAdapter adapter = PermissionManager.get().getHyperPermsAdapter();
    if (adapter == null) {
      managingPermission = null;
      buildEditView(cmd, events);
      return;
    }

    cmd.set("#KitCount.Text", HEMessages.get(playerRef, AdminKeys.Perms.TITLE));

    // Replace list area with permission template
    cmd.clear("#KitList");
    cmd.appendInline("#KitList", "Group #IndexCards { LayoutMode: Top; }");
    cmd.append("#IndexCards", UIPaths.ADMIN_PERMISSION_ADD);
    String perm = "#IndexCards[0]";

    // Populate header
    cmd.set(perm + " #PermTitle.Text", HEMessages.get(playerRef, AdminKeys.Perms.TITLE));
    cmd.set(perm + " #PermNodeLabel.Text", HEMessages.get(playerRef, AdminKeys.Perms.NODE_LABEL));
    cmd.set(perm + " #PermNodeValue.Text", managingPermission);
    cmd.set(perm + " #PermRolesHeader.Text", HEMessages.get(playerRef, AdminKeys.Perms.ROLES_HEADER));

    // Wire back button
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        perm + " #PermBackBtn",
        EventData.of("Button", "CancelPerms"),
        false
    );

    // Build role list
    List<String> groups = adapter.getGroupNames();
    if (groups.isEmpty()) {
      cmd.appendInline(perm + " #PermRoleList",
          "Group { Anchor: (Height: 30); "
          + "Label { Text: \"" + HEMessages.get(playerRef, AdminKeys.Perms.NO_ROLES)
          + "\"; Style: (FontSize: 12, TextColor: #7c8b99, HorizontalAlignment: Center, "
          + "VerticalAlignment: Center); Anchor: (Height: 28); } }");
      return;
    }

    int i = 0;
    for (String groupName : groups) {
      boolean hasPerm = adapter.groupHasPermission(groupName, managingPermission);

      String btnText = hasPerm
          ? HEMessages.get(playerRef, AdminKeys.Perms.REMOVE)
          : HEMessages.get(playerRef, AdminKeys.Perms.ADD);
      String btnAction = hasPerm ? "RemovePerm" : "AddPerm";

      // Build inline row: status indicator + group name + add/remove button
      String statusColor = hasPerm ? "#44cc44" : "#7c8b99";
      String rowUi = "Group { Anchor: (Height: 32, Bottom: 2); Background: (Color: #141c26); LayoutMode: Left; "
          + "Group { Anchor: (Width: 8); } "
          + "Group { Anchor: (Width: 8, Height: 8, Top: 12); Background: (Color: " + statusColor + "); } "
          + "Group { Anchor: (Width: 8); } "
          + "Label #RoleName { Text: \"" + groupName + "\"; "
          + "Style: (FontSize: 12, TextColor: #FFFFFF, VerticalAlignment: Center); FlexWeight: 1; } "
          + "Group { Anchor: (Width: 80); Padding: (Right: 6, Top: 4, Bottom: 4); "
          + "TextButton #PermToggle { Text: \"" + btnText + "\"; Anchor: (Width: 74, Height: 24); } } }";

      cmd.appendInline(perm + " #PermRoleList", rowUi);
      String rowIdx = perm + " #PermRoleList[" + i + "]";

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          rowIdx + " #PermToggle",
          EventData.of("Button", btnAction).append("Target", groupName),
          false
      );

      i++;
    }
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

    // Capture search filter from any event that includes it
    if (data.inputSearch != null) {
      searchFilter = data.inputSearch.isBlank() ? null : data.inputSearch.trim();
    }

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "ShowCreateModal" -> { showingCreateModal = true; rebuildContent(); }
      case "Create" -> handleCreate(data);
      case "CancelCreate" -> handleCancelCreate();
      case "Delete" -> handleDelete(data.target);
      case "Preview" -> handlePreview(data.target);
      case "CancelPreview" -> handleCancelPreview();
      case "RemoveItem" -> handleRemoveItem(data.target, data.value);
      case "Edit" -> handleEdit(data.target);
      case "SaveEdit" -> handleSaveEdit(data);
      case "CancelEdit" -> handleCancelEdit();
      case "ToggleOneTime" -> handleToggleOneTime();
      case "ManagePerms" -> handleManagePerms(data.target);
      case "CancelPerms" -> handleCancelPerms();
      case "AddPerm" -> handleAddPerm(data.target);
      case "RemovePerm" -> handleRemovePerm(data.target);
      default -> sendUpdate();
    }
  }

  private void handleCreate(@NotNull AdminPageData data) {
    // Use the name from the modal input, or fall back to auto-generated
    String name = (data.inputName != null && !data.inputName.isBlank())
        ? data.inputName.trim().toLowerCase().replaceAll("\\s+", "_")
        : "kit_" + System.currentTimeMillis() % 100000;

    kitManager.captureFromInventory(playerRef, pageStore, pageRef, name);
    showingCreateModal = false;
    rebuildContent();
  }

  private void handleCancelCreate() {
    showingCreateModal = false;
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

  private void handleManagePerms(@Nullable String permNode) {
    if (permNode == null) return;
    managingPermission = permNode;
    rebuildContent();
  }

  private void handleCancelPerms() {
    managingPermission = null;
    // Return to edit view (editingKit is still set)
    rebuildContent();
  }

  private void handleAddPerm(@Nullable String groupName) {
    if (groupName == null || managingPermission == null) return;
    HyperPermsProviderAdapter adapter = PermissionManager.get().getHyperPermsAdapter();
    if (adapter != null) {
      adapter.addPermissionToGroup(groupName, managingPermission);
    }
    // Rebuild to reflect the change
    rebuildContent();
  }

  private void handleRemovePerm(@Nullable String groupName) {
    if (groupName == null || managingPermission == null) return;
    HyperPermsProviderAdapter adapter = PermissionManager.get().getHyperPermsAdapter();
    if (adapter != null) {
      adapter.removePermissionFromGroup(groupName, managingPermission);
    }
    // Rebuild to reflect the change
    rebuildContent();
  }

  private void rebuildContent() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildContent(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
