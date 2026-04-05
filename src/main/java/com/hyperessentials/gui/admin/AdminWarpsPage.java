package com.hyperessentials.gui.admin;

import com.hyperessentials.data.Warp;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.integration.HyperPermsProviderAdapter;
import com.hyperessentials.integration.PermissionManager;
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

/**
 * Admin warps page — list all warps with create/delete/edit.
 * Supports a detail edit mode for modifying individual warp properties,
 * a permission management mode for quick-adding permissions to HyperPerms roles,
 * and a create modal for naming new warps.
 */
public class AdminWarpsPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final WarpManager warpManager;
  private final GuiManager guiManager;

  /** Name of the warp being edited, null for list mode. */
  @Nullable
  private String editingWarp;

  /** Permission node being managed, null when not in permission mode. */
  @Nullable
  private String managingPermission;

  /** Whether the create modal is showing. */
  private boolean showingCreateModal;

  /** Current search filter text. */
  @Nullable
  private String searchFilter;

  /** Current page index (zero-based) for pagination. */
  private int currentPage = 0;

  /** Number of warp entries displayed per page. */
  private static final int ITEMS_PER_PAGE = 8;

  /** Current sort mode for the warp list. */
  private SortMode sortMode = SortMode.NAME;

  private enum SortMode { NAME, CATEGORY, WORLD, NEWEST }

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
    buildContent(cmd, events);
  }

  private void buildContent(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    if (managingPermission != null) {
      buildPermissionView(cmd, events);
    } else if (editingWarp != null) {
      buildEditView(cmd, events);
    } else if (showingCreateModal) {
      buildCreateModal(cmd, events);
    } else {
      buildWarpList(cmd, events);
    }
  }

  // =====================================================================
  // List Mode
  // =====================================================================

  private void buildWarpList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    Collection<Warp> allWarps = warpManager.getAllWarps();

    // Ensure header, controls, and pagination are visible in list mode
    cmd.set("#HeaderRow.Visible", true);
    cmd.set("#ControlsRow.Visible", true);
    cmd.set("#PaginationRow.Visible", true);

    // Set search placeholder and restore current search value
    cmd.set("#SearchInput.PlaceholderText",
        HEMessages.get(playerRef, AdminKeys.Warps.SEARCH_PLACEHOLDER));
    if (searchFilter != null) {
      cmd.set("#SearchInput.Value", searchFilter);
    }

    // Search field — fires on every value change
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SearchInput",
        EventData.of("Button", "SearchChanged")
            .append("@SearchInput", "#SearchInput.Value"), false);

    // Sort dropdown
    cmd.set("#SortDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString("Name"), "NAME"),
        new DropdownEntryInfo(LocalizableString.fromString("Category"), "CATEGORY"),
        new DropdownEntryInfo(LocalizableString.fromString("World"), "WORLD"),
        new DropdownEntryInfo(LocalizableString.fromString("Newest"), "NEWEST")
    ));
    cmd.set("#SortDropdown.Value", sortMode.name());
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#SortDropdown",
        EventData.of("Button", "SortChanged")
            .append("@SortMode", "#SortDropdown.Value")
            .append("@SearchInput", "#SearchInput.Value"), false);

    // Create button — opens the create modal, captures search text
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#CreateBtn",
        EventData.of("Button", "ShowCreateModal")
            .append("@SearchInput", "#SearchInput.Value"),
        false
    );

    // Build warp list with current sort mode
    List<Warp> sorted = new ArrayList<>(allWarps);
    Comparator<Warp> comparator = switch (sortMode) {
      case NAME -> Comparator.comparing(Warp::name);
      case CATEGORY -> Comparator.comparing(Warp::category).thenComparing(Warp::name);
      case WORLD -> Comparator.comparing(Warp::world).thenComparing(Warp::name);
      case NEWEST -> Comparator.comparingLong(Warp::createdAt).reversed();
    };
    sorted.sort(comparator);

    // Apply search filter
    if (searchFilter != null && !searchFilter.isBlank()) {
      String filter = searchFilter.toLowerCase();
      sorted.removeIf(w -> !w.name().toLowerCase().contains(filter)
          && !w.displayName().toLowerCase().contains(filter)
          && !w.category().toLowerCase().contains(filter));
    }

    // Count display (after filtering)
    cmd.set("#WarpCount.Text", sorted.size() + " warp" + (sorted.size() != 1 ? "s" : ""));

    // Pagination calculation
    int totalItems = sorted.size();
    int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
    if (currentPage >= totalPages) currentPage = totalPages - 1;
    if (currentPage < 0) currentPage = 0;

    int startIdx = currentPage * ITEMS_PER_PAGE;
    int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, totalItems);

    cmd.clear("#WarpList");
    cmd.appendInline("#WarpList", "Group #IndexCards { LayoutMode: Top; }");

    if (sorted.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", HEMessages.get(playerRef, AdminKeys.Warps.EMPTY_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, AdminKeys.Warps.EMPTY_MESSAGE));
    } else {
      List<Warp> pageWarps = sorted.subList(startIdx, endIdx);

      int i = 0;
      for (Warp warp : pageWarps) {
        cmd.append("#IndexCards", UIPaths.ADMIN_WARP_ENTRY);
        String idx = "#IndexCards[" + i + "]";

        cmd.set(idx + " #WarpName.Text", warp.displayName());
        cmd.set(idx + " #WarpCategory.Text", warp.category());
        cmd.set(idx + " #WarpWorld.Text", UIHelper.formatWorldName(warp.world()));
        cmd.set(idx + " #WarpCoords.Text", UIHelper.formatCoords(warp.x(), warp.y(), warp.z()));

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #EditBtn",
            EventData.of("Button", "Edit").append("Target", warp.name())
                .append("@SearchInput", "#SearchInput.Value"),
            false
        );

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #DeleteBtn",
            EventData.of("Button", "Delete").append("Target", warp.name())
                .append("@SearchInput", "#SearchInput.Value"),
            false
        );

        i++;
      }
    }

    // Pagination controls
    cmd.set("#PageInfo.Text", "Page " + (currentPage + 1) + " / " + totalPages);

    cmd.set("#PrevBtn.Disabled", currentPage <= 0);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#PrevBtn",
        EventData.of("Button", "PrevPage")
            .append("@SearchInput", "#SearchInput.Value"), false);

    cmd.set("#NextBtn.Disabled", currentPage >= totalPages - 1);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#NextBtn",
        EventData.of("Button", "NextPage")
            .append("@SearchInput", "#SearchInput.Value"), false);
  }

  // =====================================================================
  // Create Modal
  // =====================================================================

  private void buildCreateModal(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    cmd.set("#WarpCount.Text", HEMessages.get(playerRef, AdminKeys.Warps.CREATE_TITLE));

    // Hide search/sort/create controls and pagination in create modal
    cmd.set("#HeaderRow.Visible", false);
    cmd.set("#ControlsRow.Visible", false);
    cmd.set("#PaginationRow.Visible", false);

    // Replace with edit template in create mode
    cmd.clear("#WarpList");
    cmd.appendInline("#WarpList", "Group #IndexCards { LayoutMode: Top; }");
    cmd.append("#IndexCards", UIPaths.ADMIN_WARP_EDIT);
    String edit = "#IndexCards[0]";

    // Title
    cmd.set(edit + " #EditTitle.Text", HEMessages.get(playerRef, AdminKeys.Warps.CREATE_TITLE));

    // Swap name label for editable input
    cmd.set(edit + " #EditName.Visible", false);
    cmd.set(edit + " #EditNameInput.Visible", true);
    cmd.set(edit + " #EditNameInput.PlaceholderText",
        HEMessages.get(playerRef, AdminKeys.Warps.NAME_PLACEHOLDER));

    // Populate labels
    cmd.set(edit + " #EditDisplayNameLabel.Text", HEMessages.get(playerRef, AdminKeys.Warps.EDIT_DISPLAY_NAME));
    cmd.set(edit + " #EditCategoryLabel.Text", HEMessages.get(playerRef, AdminKeys.Warps.EDIT_CATEGORY));
    cmd.set(edit + " #EditDescriptionLabel.Text", HEMessages.get(playerRef, AdminKeys.Warps.EDIT_DESCRIPTION));

    // Show current location for world/coords
    String worldName = "";
    var worldUuid = playerRef.getWorldUuid();
    if (worldUuid != null) {
      var world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(worldUuid);
      if (world != null) worldName = world.getName();
    }
    cmd.set(edit + " #EditWorld.Text", UIHelper.formatWorldName(worldName));

    var pos = playerRef.getTransform().getPosition();
    cmd.set(edit + " #EditCoords.Text", UIHelper.formatCoords(pos.getX(), pos.getY(), pos.getZ()));

    // Button labels for create mode
    cmd.set(edit + " #EditSaveBtn.Text", "Create");
    cmd.set(edit + " #EditCancelBtn.Text", "Cancel");

    // Wire create button — reads all fields
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        edit + " #EditSaveBtn",
        EventData.of("Button", "Create")
            .append("@InputName", edit + " #EditNameInput.Value")
            .append("@InputDisplayName", edit + " #EditDisplayName.Value")
            .append("@InputCategory", edit + " #EditCategory.Value")
            .append("@InputDescription", edit + " #EditDescription.Value"),
        false
    );

    // Wire back/cancel buttons
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        edit + " #EditBackBtn",
        EventData.of("Button", "CancelCreate"),
        false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        edit + " #EditCancelBtn",
        EventData.of("Button", "CancelCreate"),
        false
    );
  }

  // =====================================================================
  // Edit Mode
  // =====================================================================

  private void buildEditView(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    if (editingWarp == null) return;

    Warp warp = warpManager.getWarp(editingWarp);
    if (warp == null) {
      editingWarp = null;
      buildWarpList(cmd, events);
      return;
    }

    cmd.set("#WarpCount.Text", HEMessages.get(playerRef, AdminKeys.Warps.EDIT_TITLE));

    // Hide search/sort/create controls and pagination in edit mode
    cmd.set("#HeaderRow.Visible", false);
    cmd.set("#ControlsRow.Visible", false);
    cmd.set("#PaginationRow.Visible", false);

    // Clear and replace the list area
    cmd.clear("#WarpList");
    cmd.appendInline("#WarpList", "Group #IndexCards { LayoutMode: Top; }");

    // Append the edit template
    cmd.append("#IndexCards", UIPaths.ADMIN_WARP_EDIT);
    String edit = "#IndexCards[0]";

    // Populate read-only fields
    cmd.set(edit + " #EditName.Text", warp.name());
    cmd.set(edit + " #EditWorld.Text", UIHelper.formatWorldName(warp.world()));
    cmd.set(edit + " #EditCoords.Text", UIHelper.formatCoords(warp.x(), warp.y(), warp.z()));

    // Populate labels
    cmd.set(edit + " #EditTitle.Text", HEMessages.get(playerRef, AdminKeys.Warps.EDIT_TITLE));
    cmd.set(edit + " #EditDisplayNameLabel.Text", HEMessages.get(playerRef, AdminKeys.Warps.EDIT_DISPLAY_NAME));
    cmd.set(edit + " #EditCategoryLabel.Text", HEMessages.get(playerRef, AdminKeys.Warps.EDIT_CATEGORY));
    cmd.set(edit + " #EditDescriptionLabel.Text", HEMessages.get(playerRef, AdminKeys.Warps.EDIT_DESCRIPTION));

    // Populate editable field values
    cmd.set(edit + " #EditDisplayName.Value", warp.displayName());
    cmd.set(edit + " #EditCategory.Value", warp.category());
    if (warp.description() != null) {
      cmd.set(edit + " #EditDescription.Value", warp.description());
    }

    // Wire edit events — read field values on save
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        edit + " #EditSaveBtn",
        EventData.of("Button", "SaveEdit")
            .append("Target", warp.name())
            .append("@InputName", edit + " #EditDisplayName.Value")
            .append("@InputCategory", edit + " #EditCategory.Value")
            .append("@InputDescription", edit + " #EditDescription.Value"),
        false
    );

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
      String permNode = warp.permission() != null ? warp.permission() : "hyperessentials.warp." + warp.name();

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

    cmd.set("#WarpCount.Text", HEMessages.get(playerRef, AdminKeys.Perms.TITLE));

    // Hide search/sort/create controls and pagination in permission mode
    cmd.set("#HeaderRow.Visible", false);
    cmd.set("#ControlsRow.Visible", false);
    cmd.set("#PaginationRow.Visible", false);

    // Replace list area with permission template
    cmd.clear("#WarpList");
    cmd.appendInline("#WarpList", "Group #IndexCards { LayoutMode: Top; }");
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
      case "Edit" -> handleEdit(data.target);
      case "SaveEdit" -> handleSaveEdit(data);
      case "CancelEdit" -> handleCancelEdit();
      case "ManagePerms" -> handleManagePerms(data.target);
      case "CancelPerms" -> handleCancelPerms();
      case "AddPerm" -> handleAddPerm(data.target);
      case "RemovePerm" -> handleRemovePerm(data.target);
      case "SearchChanged" -> {
        searchFilter = (data.inputSearch != null && !data.inputSearch.isBlank())
            ? data.inputSearch.trim() : null;
        currentPage = 0;
        rebuildContent();
      }
      case "SortChanged" -> {
        if (data.sortMode != null) {
          try { sortMode = SortMode.valueOf(data.sortMode); } catch (IllegalArgumentException ignored) {}
        }
        currentPage = 0;
        rebuildContent();
      }
      case "PrevPage" -> {
        currentPage = Math.max(0, currentPage - 1);
        rebuildContent();
      }
      case "NextPage" -> {
        currentPage = currentPage + 1;
        rebuildContent();
      }
      default -> sendUpdate();
    }
  }

  private void handleCreate(@NotNull AdminPageData data) {
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

    // Use the name from the input, or fall back to auto-generated
    String name = (data.inputName != null && !data.inputName.isBlank())
        ? data.inputName.trim().toLowerCase().replaceAll("\\s+", "_")
        : "warp_" + System.currentTimeMillis() % 100000;

    Warp warp = Warp.create(name, worldName, worldUuidStr,
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX(),
        playerRef.getUuid().toString());

    // Apply optional fields from the create form
    if (data.inputDisplayName != null && !data.inputDisplayName.isBlank()) {
      warp = warp.withDisplayName(data.inputDisplayName.trim());
    }
    if (data.inputCategory != null && !data.inputCategory.isBlank()) {
      warp = warp.withCategory(data.inputCategory.trim());
    }
    if (data.inputDescription != null) {
      String desc = data.inputDescription.trim();
      warp = warp.withDescription(desc.isEmpty() ? null : desc);
    }

    warpManager.setWarp(warp);

    showingCreateModal = false;
    rebuildContent();
  }

  private void handleCancelCreate() {
    showingCreateModal = false;
    rebuildContent();
  }

  private void handleDelete(String warpName) {
    if (warpName == null) return;
    warpManager.deleteWarp(warpName);
    rebuildContent();
  }

  private void handleEdit(@Nullable String warpName) {
    if (warpName == null) return;

    // Verify the warp exists
    Warp warp = warpManager.getWarp(warpName);
    if (warp == null) return;

    editingWarp = warpName;
    rebuildContent();
  }

  private void handleSaveEdit(@NotNull AdminPageData data) {
    if (data.target == null || editingWarp == null) {
      editingWarp = null;
      rebuildContent();
      return;
    }

    Warp warp = warpManager.getWarp(data.target);
    if (warp == null) {
      editingWarp = null;
      rebuildContent();
      return;
    }

    // Apply edits from input fields
    if (data.inputName != null && !data.inputName.isBlank()) {
      warp = warp.withDisplayName(data.inputName.trim());
    }
    if (data.inputCategory != null && !data.inputCategory.isBlank()) {
      warp = warp.withCategory(data.inputCategory.trim());
    }

    // Description can be cleared (empty string = null)
    if (data.inputDescription != null) {
      String desc = data.inputDescription.trim();
      warp = warp.withDescription(desc.isEmpty() ? null : desc);
    }

    warpManager.setWarp(warp);

    editingWarp = null;
    rebuildContent();
  }

  private void handleCancelEdit() {
    editingWarp = null;
    rebuildContent();
  }

  private void handleManagePerms(@Nullable String permNode) {
    if (permNode == null) return;
    managingPermission = permNode;
    rebuildContent();
  }

  private void handleCancelPerms() {
    managingPermission = null;
    // Return to edit view (editingWarp is still set)
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
