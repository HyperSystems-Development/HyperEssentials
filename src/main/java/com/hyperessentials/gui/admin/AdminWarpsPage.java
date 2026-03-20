package com.hyperessentials.gui.admin;

import com.hyperessentials.data.Warp;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
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
 * Supports a detail edit mode for modifying individual warp properties.
 */
public class AdminWarpsPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final WarpManager warpManager;
  private final GuiManager guiManager;

  /** Name of the warp being edited, null for list mode. */
  @Nullable
  private String editingWarp;

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
    if (editingWarp != null) {
      buildEditView(cmd, events);
    } else {
      buildWarpList(cmd, events);
    }
  }

  // =====================================================================
  // List Mode
  // =====================================================================

  private void buildWarpList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    Collection<Warp> allWarps = warpManager.getAllWarps();
    cmd.set("#WarpCount.Text", allWarps.size() + " warp" + (allWarps.size() != 1 ? "s" : ""));

    // Set placeholder text on the name input
    cmd.set("#WarpNameInput.PlaceholderText",
        HEMessages.get(playerRef, AdminKeys.Warps.NAME_PLACEHOLDER));

    // Create button — reads the name input value
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#CreateBtn",
        EventData.of("Button", "Create")
            .append("@InputName", "#WarpNameInput.Value"),
        false
    );

    // Build warp list sorted by category then name
    List<Warp> sorted = new ArrayList<>(allWarps);
    sorted.sort(Comparator.comparing(Warp::category).thenComparing(Warp::name));

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
          EventData.of("Button", "Edit").append("Target", warp.name()),
          false
      );

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #DeleteBtn",
          EventData.of("Button", "Delete").append("Target", warp.name()),
          false
      );

      i++;
    }
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

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "Create" -> handleCreate(data);
      case "Delete" -> handleDelete(data.target);
      case "Edit" -> handleEdit(data.target);
      case "SaveEdit" -> handleSaveEdit(data);
      case "CancelEdit" -> handleCancelEdit();
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

    // Use the name from the text input, or fall back to auto-generated
    String name = (data.inputName != null && !data.inputName.isBlank())
        ? data.inputName.trim().toLowerCase().replaceAll("\\s+", "_")
        : "warp_" + System.currentTimeMillis() % 100000;

    Warp warp = Warp.create(name, worldName, worldUuidStr,
        pos.getX(), pos.getY(), pos.getZ(),
        rot.getY(), rot.getX(),
        playerRef.getUuid().toString());
    warpManager.setWarp(warp);

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

  private void rebuildContent() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildContent(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
