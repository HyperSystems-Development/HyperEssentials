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

    // Set search placeholder
    cmd.set("#SearchInput.PlaceholderText",
        HEMessages.get(playerRef, AdminKeys.Warps.SEARCH_PLACEHOLDER));

    // Create button — opens the create modal, captures search text
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#CreateBtn",
        EventData.of("Button", "ShowCreateModal")
            .append("@SearchInput", "#SearchInput.Value"),
        false
    );

    // Build warp list sorted by category then name
    List<Warp> sorted = new ArrayList<>(allWarps);
    sorted.sort(Comparator.comparing(Warp::category).thenComparing(Warp::name));

    // Apply search filter
    if (searchFilter != null && !searchFilter.isBlank()) {
      String filter = searchFilter.toLowerCase();
      sorted.removeIf(w -> !w.name().toLowerCase().contains(filter)
          && !w.displayName().toLowerCase().contains(filter));
    }

    cmd.set("#WarpCount.Text", sorted.size() + " warp" + (sorted.size() != 1 ? "s" : ""));

    cmd.clear("#WarpList");
    cmd.appendInline("#WarpList", "Group #IndexCards { LayoutMode: Top; }");

    if (sorted.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", HEMessages.get(playerRef, AdminKeys.Warps.EMPTY_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, AdminKeys.Warps.EMPTY_MESSAGE));
      return;
    }

    int i = 0;
    for (Warp warp : sorted) {
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

  // =====================================================================
  // Create Modal
  // =====================================================================

  private void buildCreateModal(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    cmd.set("#WarpCount.Text", HEMessages.get(playerRef, AdminKeys.Warps.CREATE_TITLE));

    // Hide the header row elements and replace with modal content
    cmd.clear("#WarpList");
    cmd.appendInline("#WarpList", "Group #IndexCards { LayoutMode: Top; }");

    // Build a simple create form inline
    String modalUi =
        "Group { Anchor: (Height: 200); LayoutMode: Top; Padding: (Left: 40, Right: 40, Top: 30); "
        // Title
        + "Label { Text: \"" + HEMessages.get(playerRef, AdminKeys.Warps.CREATE_TITLE)
        + "\"; Style: (FontSize: 16, TextColor: #FFFFFF, HorizontalAlignment: Center); Anchor: (Height: 28, Bottom: 20); } "
        // Name label
        + "Label { Text: \"" + HEMessages.get(playerRef, AdminKeys.Warps.NAME_PLACEHOLDER)
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

    // Hide the create row elements by clearing and replacing the list area
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
    cmd.set(edit + " #EditPermissionLabel.Text", HEMessages.get(playerRef, AdminKeys.Warps.EDIT_PERMISSION));

    // Populate editable field values
    cmd.set(edit + " #EditDisplayName.Value", warp.displayName());
    cmd.set(edit + " #EditCategory.Value", warp.category());
    if (warp.description() != null) {
      cmd.set(edit + " #EditDescription.Value", warp.description());
    }
    if (warp.permission() != null) {
      cmd.set(edit + " #EditPermission.Value", warp.permission());
    }

    // Wire edit events — read field values on save
    events.addEventBinding(
        CustomUIEventBindingType.Activating,
        edit + " #EditSaveBtn",
        EventData.of("Button", "SaveEdit")
            .append("Target", warp.name())
            .append("@InputName", edit + " #EditDisplayName.Value")
            .append("@InputCategory", edit + " #EditCategory.Value")
            .append("@InputDescription", edit + " #EditDescription.Value")
            .append("@InputPermission", edit + " #EditPermission.Value"),
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

    // Show "Permissions" button only when HyperPerms is available
    HyperPermsProviderAdapter adapter = PermissionManager.get().getHyperPermsAdapter();
    if (adapter != null) {
      // Determine the permission node for this warp
      String permNode = warp.permission() != null ? warp.permission() : "hyperessentials.warp." + warp.name();

      // Add Permissions button after save/cancel row — inline within the edit template area
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

    cmd.set("#WarpCount.Text", HEMessages.get(playerRef, AdminKeys.Perms.TITLE));

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

      // Build inline row: group name + add/remove button
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

    // Use the name from the modal input, or fall back to auto-generated
    String name = (data.inputName != null && !data.inputName.isBlank())
        ? data.inputName.trim().toLowerCase().replaceAll("\\s+", "_")
        : "warp_" + System.currentTimeMillis() % 100000;

    Warp warp = Warp.create(name, worldName, worldUuidStr,
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX(),
        playerRef.getUuid().toString());
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

    // Description and permission can be cleared (empty string = null)
    if (data.inputDescription != null) {
      String desc = data.inputDescription.trim();
      warp = warp.withDescription(desc.isEmpty() ? null : desc);
    }
    if (data.inputPermission != null) {
      String perm = data.inputPermission.trim();
      warp = warp.withPermission(perm.isEmpty() ? null : perm);
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
