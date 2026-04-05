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
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
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
import java.util.UUID;

/**
 * Admin kits page — list all kits with create from inventory/delete/preview/edit.
 * Supports list, preview, edit, permission management, and create modal modes.
 */
public class AdminKitsPage extends InteractiveCustomUIPage<AdminPageData> {

  private static final int ITEMS_PER_PAGE = 8;

  /** Sort modes for the kit list. */
  private enum SortMode {
    NAME, ITEMS, COOLDOWN
  }

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

  /** Items captured from inventory for create preview (not yet saved). */
  @Nullable
  private List<KitItem> pendingCreateItems;

  /** Tracks the one-time toggle state during creation. */
  private boolean createOneTimeState;

  /** Current search filter text. */
  @Nullable
  private String searchFilter;

  /** Current page index for pagination. */
  private int currentPage = 0;

  /** Current sort mode for the kit list. */
  private SortMode sortMode = SortMode.NAME;

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

    // Ensure header, search/sort, and pagination are visible in list mode
    cmd.set("#HeaderRow.Visible", true);
    cmd.set("#ControlsRow.Visible", true);
    cmd.set("#PaginationRow.Visible", true);

    // Set search placeholder
    cmd.set("#SearchInput.PlaceholderText",
        HEMessages.get(playerRef, AdminKeys.Kits.SEARCH_PLACEHOLDER));

    // Create button — opens the create modal, captures search text and sort
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#CreateBtn",
        EventData.of("Button", "ShowCreateModal")
            .append("@SearchInput", "#SearchInput.Value")
            .append("@SortMode", "#SortDropdown.Value"),
        false
    );

    // Sort dropdown entries
    cmd.set("#SortDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString("Name"), "NAME"),
        new DropdownEntryInfo(LocalizableString.fromString("Items"), "ITEMS"),
        new DropdownEntryInfo(LocalizableString.fromString("Cooldown"), "COOLDOWN")
    ));
    cmd.set("#SortDropdown.Value", sortMode.name());
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SortDropdown",
        EventData.of("Button", "SortChanged")
            .append("@SortMode", "#SortDropdown.Value")
            .append("@SearchInput", "#SearchInput.Value"), false);

    // Search event — trigger on activating to capture text
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
        EventData.of("Button", "SearchChanged")
            .append("@SearchInput", "#SearchInput.Value")
            .append("@SortMode", "#SortDropdown.Value"), false);

    // Build filtered and sorted list
    List<Kit> sorted = new ArrayList<>(allKits);

    // Apply search filter
    if (searchFilter != null && !searchFilter.isBlank()) {
      String filter = searchFilter.toLowerCase();
      sorted.removeIf(k -> !k.name().toLowerCase().contains(filter)
          && !k.displayName().toLowerCase().contains(filter));
    }

    // Apply sort
    switch (sortMode) {
      case ITEMS -> sorted.sort(Comparator.comparingInt((Kit k) -> k.items().size()).reversed());
      case COOLDOWN -> sorted.sort(Comparator.comparingInt(Kit::cooldownSeconds).reversed());
      default -> sorted.sort(Comparator.comparing(Kit::name));
    }

    cmd.set("#KitCount.Text", sorted.size() + " kit" + (sorted.size() != 1 ? "s" : ""));

    // Pagination
    int totalPages = Math.max(1, (int) Math.ceil((double) sorted.size() / ITEMS_PER_PAGE));
    if (currentPage >= totalPages) currentPage = totalPages - 1;
    if (currentPage < 0) currentPage = 0;

    int start = currentPage * ITEMS_PER_PAGE;
    int end = Math.min(start + ITEMS_PER_PAGE, sorted.size());
    List<Kit> pageKits = sorted.subList(start, end);

    cmd.clear("#KitList");
    cmd.appendInline("#KitList", "Group #IndexCards { LayoutMode: Top; }");

    if (pageKits.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", HEMessages.get(playerRef, AdminKeys.Kits.EMPTY_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, AdminKeys.Kits.EMPTY_MESSAGE));
    } else {
      int i = 0;
      for (Kit kit : pageKits) {
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
                .append("@SearchInput", "#SearchInput.Value")
                .append("@SortMode", "#SortDropdown.Value"),
            false
        );

        // Edit button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #EditBtn",
            EventData.of("Button", "Edit").append("Target", kit.name())
                .append("@SearchInput", "#SearchInput.Value")
                .append("@SortMode", "#SortDropdown.Value"),
            false
        );

        // Delete button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #DeleteBtn",
            EventData.of("Button", "Delete").append("Target", kit.name())
                .append("@SearchInput", "#SearchInput.Value")
                .append("@SortMode", "#SortDropdown.Value"),
            false
        );

        i++;
      }
    }

    // Pagination controls
    cmd.set("#PageInfo.Text", "Page " + (currentPage + 1) + " / " + totalPages);

    cmd.set("#PrevBtn.Disabled", currentPage <= 0);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#PrevBtn",
        EventData.of("Button", "PrevPage").append("Page", String.valueOf(currentPage - 1))
            .append("@SearchInput", "#SearchInput.Value")
            .append("@SortMode", "#SortDropdown.Value"), false);

    cmd.set("#NextBtn.Disabled", currentPage >= totalPages - 1);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#NextBtn",
        EventData.of("Button", "NextPage").append("Page", String.valueOf(currentPage + 1))
            .append("@SearchInput", "#SearchInput.Value")
            .append("@SortMode", "#SortDropdown.Value"), false);
  }

  // =====================================================================
  // Create Modal
  // =====================================================================

  private void buildCreateModal(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    cmd.set("#KitCount.Text", HEMessages.get(playerRef, AdminKeys.Kits.CREATE_TITLE));

    // Hide search/sort/create controls and pagination in create modal
    cmd.set("#HeaderRow.Visible", false);
    cmd.set("#ControlsRow.Visible", false);
    cmd.set("#PaginationRow.Visible", false);

    // Replace list area with two-column create template
    cmd.clear("#KitList");
    cmd.appendInline("#KitList", "Group #IndexCards { LayoutMode: Top; }");
    cmd.append("#IndexCards", UIPaths.ADMIN_KIT_CREATE);
    String create = "#IndexCards[0]";

    // Title
    cmd.set(create + " #CreateTitle.Text", HEMessages.get(playerRef, AdminKeys.Kits.CREATE_TITLE));
    cmd.set(create + " #CreateNameInput.PlaceholderText",
        HEMessages.get(playerRef, AdminKeys.Kits.NAME_PLACEHOLDER));

    // Default cooldown/one-time from config
    int defaultCooldown = com.hyperessentials.config.ConfigManager.get().kits().getDefaultCooldownSeconds();
    cmd.set(create + " #CreateCooldown.Value", String.valueOf(defaultCooldown));
    cmd.set(create + " #CreateOneTimeToggle.Text",
        HEMessages.get(playerRef, createOneTimeState ? AdminKeys.Common.YES : AdminKeys.Common.NO));

    // Item preview (right column)
    List<KitItem> items = pendingCreateItems != null ? pendingCreateItems : List.of();
    cmd.set(create + " #CreateItemCount.Text",
        items.size() + " item" + (items.size() != 1 ? "s" : "") + " from inventory");

    if (items.isEmpty()) {
      cmd.appendInline(create + " #CreateItems",
          "Group { Anchor: (Height: 40); LayoutMode: Top; "
          + "Label { Text: \"Inventory is empty.\"; "
          + "Style: (FontSize: 12, TextColor: #7c8b99, HorizontalAlignment: Center, VerticalAlignment: Center); "
          + "Anchor: (Height: 30); } }");
    } else {
      for (int i = 0; i < items.size(); i++) {
        KitItem item = items.get(i);
        cmd.append(create + " #CreateItems", UIPaths.ADMIN_KIT_PREVIEW_ITEM);
        String rowIdx = create + " #CreateItems[" + i + "]";

        cmd.set(rowIdx + " #ItemImg.ItemId", item.itemId());
        cmd.set(rowIdx + " #ItemName.Text", formatItemName(item.itemId()));
        cmd.set(rowIdx + " #ItemInfo.Text", "x" + item.quantity() + " [" + item.section() + "]");

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            rowIdx + " #RemoveBtn",
            EventData.of("Button", "RemoveCreateItem")
                .append("Value", String.valueOf(i)),
            false
        );
      }
    }

    // Wire one-time toggle
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        create + " #CreateOneTimeToggle",
        EventData.of("Button", "ToggleCreateOneTime"),
        false
    );

    // Wire create button — reads all form fields
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        create + " #CreateSaveBtn",
        EventData.of("Button", "Create")
            .append("@InputName", create + " #CreateNameInput.Value")
            .append("@InputDisplayName", create + " #CreateDisplayName.Value")
            .append("@InputCooldown", create + " #CreateCooldown.Value"),
        false
    );

    // Wire back/cancel buttons
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        create + " #CreateBackBtn",
        EventData.of("Button", "CancelCreate"),
        false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        create + " #CreateCancelBtn",
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

    // Hide search/sort/create controls and pagination in preview mode
    cmd.set("#HeaderRow.Visible", false);
    cmd.set("#ControlsRow.Visible", false);
    cmd.set("#PaginationRow.Visible", false);

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

        cmd.append(preview + " #PreviewItems", UIPaths.ADMIN_KIT_PREVIEW_ITEM);
        String rowIdx = preview + " #PreviewItems[" + i + "]";

        cmd.set(rowIdx + " #ItemImg.ItemId", item.itemId());
        cmd.set(rowIdx + " #ItemName.Text", itemName);
        cmd.set(rowIdx + " #ItemInfo.Text", "x" + item.quantity() + " [" + item.section() + "]");

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

    // Hide search/sort/create controls and pagination in edit mode
    cmd.set("#HeaderRow.Visible", false);
    cmd.set("#ControlsRow.Visible", false);
    cmd.set("#PaginationRow.Visible", false);

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

    // Populate editable field values
    cmd.set(edit + " #EditDisplayName.Value", kit.displayName());
    cmd.set(edit + " #EditCooldown.Value", String.valueOf(kit.cooldownSeconds()));

    // One-time toggle button text
    editOneTimeState = kit.oneTime();
    cmd.set(edit + " #EditOneTimeToggle.Text", HEMessages.get(playerRef, editOneTimeState ? AdminKeys.Common.YES : AdminKeys.Common.NO));

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
            .append("@InputCooldown", edit + " #EditCooldown.Value"),
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

    // Show "Permissions" button inline when HyperPerms is available
    HyperPermsProviderAdapter adapter = PermissionManager.get().getHyperPermsAdapter();
    if (adapter != null) {
      String permNode = kit.permission() != null ? kit.permission() : "hyperessentials.kit." + kit.name();

      cmd.set(edit + " #EditPermBtn.Visible", true);
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          edit + " #EditPermBtn",
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

    // Hide search/sort/create controls and pagination in permission mode
    cmd.set("#HeaderRow.Visible", false);
    cmd.set("#ControlsRow.Visible", false);
    cmd.set("#PaginationRow.Visible", false);

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

      cmd.append(perm + " #PermRoleList", UIPaths.ADMIN_PERM_ROLE_ENTRY);
      String rowIdx = perm + " #PermRoleList[" + i + "]";

      cmd.set(rowIdx + " #RoleName.Text", groupName);
      cmd.set(rowIdx + " #PermToggle.Text", btnText);
      cmd.set(rowIdx + " #StatusIndicator.Background.Color", hasPerm ? "#44cc44" : "#7c8b99");

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

    // Capture sort mode from any event that includes it
    if (data.sortMode != null && !data.sortMode.isBlank()) {
      try {
        sortMode = SortMode.valueOf(data.sortMode.trim());
      } catch (IllegalArgumentException ignored) {
        // Keep existing sort mode if parse fails
      }
    }

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "ShowCreateModal" -> {
        pendingCreateItems = new ArrayList<>(kitManager.captureInventoryItems(pageStore, pageRef));
        createOneTimeState = com.hyperessentials.config.ConfigManager.get().kits().isOneTimeDefault();
        showingCreateModal = true;
        rebuildContent();
      }
      case "Create" -> handleCreate(data);
      case "CancelCreate" -> handleCancelCreate();
      case "ToggleCreateOneTime" -> { createOneTimeState = !createOneTimeState; rebuildContent(); }
      case "RemoveCreateItem" -> handleRemoveCreateItem(data.value);
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
      case "SearchChanged" -> { currentPage = 0; rebuildContent(); }
      case "SortChanged" -> { currentPage = 0; rebuildContent(); }
      case "PrevPage", "NextPage" -> { currentPage = data.page; rebuildContent(); }
      default -> sendUpdate();
    }
  }

  private void handleCreate(@NotNull AdminPageData data) {
    // Use the name from the form input, or fall back to auto-generated
    String name = (data.inputName != null && !data.inputName.isBlank())
        ? data.inputName.trim().toLowerCase().replaceAll("\\s+", "_")
        : "kit_" + System.currentTimeMillis() % 100000;

    // Build display name
    String displayName = (data.inputDisplayName != null && !data.inputDisplayName.isBlank())
        ? data.inputDisplayName.trim() : name;

    // Parse cooldown
    int cooldown = com.hyperessentials.config.ConfigManager.get().kits().getDefaultCooldownSeconds();
    if (data.inputCooldown != null && !data.inputCooldown.isBlank()) {
      try { cooldown = Integer.parseInt(data.inputCooldown.trim()); } catch (NumberFormatException ignored) {}
    }

    // Use pending items (may have had items removed by user)
    List<KitItem> items = pendingCreateItems != null ? pendingCreateItems : List.of();

    Kit kit = new Kit(UUID.randomUUID(), name, displayName, items,
        cooldown, createOneTimeState, null);
    kitManager.updateKit(kit);

    showingCreateModal = false;
    pendingCreateItems = null;
    rebuildContent();
  }

  private void handleCancelCreate() {
    showingCreateModal = false;
    pendingCreateItems = null;
    rebuildContent();
  }

  private void handleRemoveCreateItem(@Nullable String indexStr) {
    if (indexStr == null || pendingCreateItems == null) return;
    try {
      int index = Integer.parseInt(indexStr);
      if (index >= 0 && index < pendingCreateItems.size()) {
        pendingCreateItems.remove(index);
      }
    } catch (NumberFormatException ignored) {}
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
