package com.hyperessentials.gui.admin;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.AnnouncementsConfig;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.module.announcements.AnnouncementScheduler;
import com.hyperessentials.module.announcements.data.Announcement;
import com.hyperessentials.module.announcements.data.AnnouncementEvent;
import com.hyperessentials.module.announcements.data.AnnouncementType;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.HEMessages;
import com.hyperessentials.util.Logger;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Admin announcements page with full CRUD, type management, and event configuration.
 * Three view modes: list (default), edit (single announcement), and events (join/leave/welcome).
 */
public class AdminAnnouncementsPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;
  @Nullable private final AnnouncementScheduler scheduler;

  /** UUID of announcement being edited, null when in list mode. */
  @Nullable private UUID editingId;

  /** Current view: "list", "edit", "events". */
  private String viewMode = "list";

  /** Tracks the type dropdown state during editing (0 = CHAT, 1 = NOTIFICATION). */
  private int editTypeIndex = 0;

  /** Tracks the enabled toggle state during editing. */
  private boolean editEnabledState = true;

  public AdminAnnouncementsPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager,
      @Nullable AnnouncementScheduler scheduler
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.scheduler = scheduler;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_ANNOUNCEMENTS);
    NavBarHelper.setupAdminBar(playerRef, "announcements", guiManager.getAdminRegistry(), cmd, events);
    buildContent(cmd, events);
  }

  private void buildContent(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    switch (viewMode) {
      case "edit" -> buildEditView(cmd, events);
      case "events" -> buildEventsView(cmd, events);
      default -> buildListView(cmd, events);
    }
  }

  // =====================================================================
  // List View
  // =====================================================================

  private void buildListView(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    AnnouncementsConfig config = ConfigManager.get().announcements();
    List<Announcement> all = config.getAnnouncements();

    cmd.set("#MessageCount.Text", all.size() + " announcement" + (all.size() != 1 ? "s" : ""));

    int intervalSecs = config.getIntervalSeconds();
    if (intervalSecs > 0) {
      cmd.set("#IntervalLabel.Text", HEMessages.get(playerRef, AdminKeys.Announcements.INTERVAL_LABEL, formatInterval(intervalSecs)));
    } else {
      cmd.set("#IntervalLabel.Text", HEMessages.get(playerRef, AdminKeys.Announcements.DISABLED));
    }

    cmd.set("#ModeLabel.Text", config.isRandomize()
        ? HEMessages.get(playerRef, AdminKeys.Announcements.MODE_RANDOM)
        : HEMessages.get(playerRef, AdminKeys.Announcements.MODE_SEQUENTIAL));

    // Replace the static info note with action buttons
    cmd.clear("#MessageList");
    cmd.appendInline("#MessageList", "Group #IndexCards { LayoutMode: Top; }");

    // Action button row: Create + Events
    cmd.appendInline("#IndexCards",
        "Group { Anchor: (Height: 30, Bottom: 6); LayoutMode: Left; "
        + "TextButton #CreateBtn { Text: \"+ New\"; Anchor: (Width: 80, Height: 26); } "
        + "Group { Anchor: (Width: 8); } "
        + "TextButton #EventsBtn { Text: \"Events\"; Anchor: (Width: 80, Height: 26); } "
        + "Group { FlexWeight: 1; } "
        + "Label #CountLabel { Text: \"\"; Style: (FontSize: 10, TextColor: #7c8b99, VerticalAlignment: Center); } }");

    long enabledCount = all.stream().filter(Announcement::enabled).count();
    cmd.set("#IndexCards[0] #CountLabel.Text", enabledCount + " enabled");

    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#IndexCards[0] #CreateBtn",
        EventData.of("Button", "Create"), false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#IndexCards[0] #EventsBtn",
        EventData.of("Button", "ShowEvents"), false
    );

    if (all.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[1] #EmptyTitle.Text",
          HEMessages.get(playerRef, AdminKeys.Announcements.EMPTY_TITLE));
      cmd.set("#IndexCards[1] #EmptyMessage.Text",
          HEMessages.get(playerRef, AdminKeys.Announcements.EMPTY_MESSAGE));
      return;
    }

    // Sort by order
    List<Announcement> sorted = new ArrayList<>(all);
    sorted.sort(Comparator.comparingInt(Announcement::order));

    int i = 1; // index 0 is the action button row
    for (Announcement ann : sorted) {
      cmd.append("#IndexCards", UIPaths.ADMIN_ANNOUNCEMENT_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      // Order number
      cmd.set(idx + " #EntryNumber.Text", "#" + ann.order());

      // Type badge
      String typeBadge = ann.type() == AnnouncementType.NOTIFICATION ? "NOTIF" : "CHAT";
      cmd.set(idx + " #TypeBadge.Text", typeBadge);
      String badgeColor = ann.type() == AnnouncementType.NOTIFICATION ? "#55FFFF" : "#FFAA00";
      cmd.set(idx + " #TypeBadge.Style.TextColor", badgeColor);

      // Message text
      cmd.set(idx + " #MessageText.Text", UIHelper.truncate(ann.message(), 45));

      // Enabled status
      String statusText = ann.enabled() ? "ON" : "OFF";
      String statusColor = ann.enabled() ? "#55FF55" : "#FF5555";
      cmd.set(idx + " #StatusLabel.Text", statusText);
      cmd.set(idx + " #StatusLabel.Style.TextColor", statusColor);

      // Toggle button
      events.addEventBinding(
          CustomUIEventBindingType.Activating, idx + " #ToggleBtn",
          EventData.of("Button", "Toggle").append("Target", ann.id().toString()), false
      );

      // Edit button
      events.addEventBinding(
          CustomUIEventBindingType.Activating, idx + " #EditBtn",
          EventData.of("Button", "Edit").append("Target", ann.id().toString()), false
      );

      // Delete button
      events.addEventBinding(
          CustomUIEventBindingType.Activating, idx + " #DeleteBtn",
          EventData.of("Button", "Delete").append("Target", ann.id().toString()), false
      );

      // Preview button
      events.addEventBinding(
          CustomUIEventBindingType.Activating, idx + " #PreviewBtn",
          EventData.of("Button", "Preview").append("Target", ann.id().toString()), false
      );

      i++;
    }
  }

  // =====================================================================
  // Edit View
  // =====================================================================

  private void buildEditView(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    if (editingId == null) {
      viewMode = "list";
      buildListView(cmd, events);
      return;
    }

    AnnouncementsConfig config = ConfigManager.get().announcements();
    Announcement ann = findById(config, editingId);

    if (ann == null) {
      editingId = null;
      viewMode = "list";
      buildListView(cmd, events);
      return;
    }

    cmd.set("#MessageCount.Text", HEMessages.get(playerRef, AdminKeys.Announcements.EDIT_TITLE));
    cmd.set("#IntervalLabel.Text", "");
    cmd.set("#ModeLabel.Text", "");

    cmd.clear("#MessageList");
    cmd.appendInline("#MessageList", "Group #IndexCards { LayoutMode: Top; }");
    cmd.append("#IndexCards", UIPaths.ADMIN_ANNOUNCEMENT_EDIT);
    String edit = "#IndexCards[0]";

    // Populate fields
    cmd.set(edit + " #EditTitle.Text",
        HEMessages.get(playerRef, AdminKeys.Announcements.EDIT_TITLE));
    cmd.set(edit + " #EditMessage.Value", ann.message());
    cmd.set(edit + " #EditOrder.Value", String.valueOf(ann.order()));

    // Type selector (using toggle button since DropdownBox in templates is problematic)
    editTypeIndex = ann.type().ordinal();
    cmd.set(edit + " #EditTypeToggle.Text", ann.type().name());

    // Enabled toggle
    editEnabledState = ann.enabled();
    cmd.set(edit + " #EditEnabledToggle.Text", HEMessages.get(playerRef, editEnabledState ? AdminKeys.Announcements.ENABLED : AdminKeys.Announcements.DISABLED));

    // Optional fields
    if (ann.permission() != null) {
      cmd.set(edit + " #EditPermission.Value", ann.permission());
    }
    if (ann.world() != null) {
      cmd.set(edit + " #EditWorld.Value", ann.world());
    }
    if (ann.cronExpression() != null) {
      cmd.set(edit + " #EditCron.Value", ann.cronExpression());
    }

    // Wire type toggle
    events.addEventBinding(
        CustomUIEventBindingType.Activating, edit + " #EditTypeToggle",
        EventData.of("Button", "ToggleType"), false
    );

    // Wire enabled toggle
    events.addEventBinding(
        CustomUIEventBindingType.Activating, edit + " #EditEnabledToggle",
        EventData.of("Button", "ToggleEnabled"), false
    );

    // Wire save
    events.addEventBinding(
        CustomUIEventBindingType.Activating, edit + " #EditSaveBtn",
        EventData.of("Button", "SaveEdit")
            .append("Target", ann.id().toString())
            .append("@InputName", edit + " #EditMessage.Value")
            .append("@InputDescription", edit + " #EditOrder.Value")
            .append("@InputPermission", edit + " #EditPermission.Value")
            .append("@InputCategory", edit + " #EditWorld.Value")
            .append("@InputCooldown", edit + " #EditCron.Value"),
        false
    );

    // Wire cancel/back
    events.addEventBinding(
        CustomUIEventBindingType.Activating, edit + " #EditBackBtn",
        EventData.of("Button", "CancelEdit"), false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating, edit + " #EditCancelBtn",
        EventData.of("Button", "CancelEdit"), false
    );
  }

  // =====================================================================
  // Events View (join/leave/welcome)
  // =====================================================================

  private void buildEventsView(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    AnnouncementsConfig config = ConfigManager.get().announcements();

    cmd.set("#MessageCount.Text", HEMessages.get(playerRef, AdminKeys.Announcements.EVENTS_TITLE));
    cmd.set("#IntervalLabel.Text", "");
    cmd.set("#ModeLabel.Text", "");

    cmd.clear("#MessageList");
    cmd.appendInline("#MessageList", "Group #IndexCards { LayoutMode: Top; }");

    // Back button
    cmd.appendInline("#IndexCards",
        "Group { Anchor: (Height: 30, Bottom: 8); LayoutMode: Left; "
        + "TextButton #EventsBackBtn { Text: \"Back\"; Anchor: (Width: 70, Height: 26); } "
        + "Group { Anchor: (Width: 10); } "
        + "Label { Text: \"Event Announcements\"; "
        + "Style: (FontSize: 14, TextColor: #FFAA00, RenderBold: true, VerticalAlignment: Center); Anchor: (Height: 26); } }");

    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#IndexCards[0] #EventsBackBtn",
        EventData.of("Button", "BackToList"), false
    );

    // Toggle rows for join/leave/welcome
    String[][] toggles = {
        {"Join Messages", config.isJoinMessagesEnabled() ? "ON" : "OFF", "ToggleJoin"},
        {"Leave Messages", config.isLeaveMessagesEnabled() ? "ON" : "OFF", "ToggleLeave"},
        {"Welcome Messages", config.isWelcomeMessagesEnabled() ? "ON" : "OFF", "ToggleWelcome"},
    };

    boolean[] states = {
        config.isJoinMessagesEnabled(),
        config.isLeaveMessagesEnabled(),
        config.isWelcomeMessagesEnabled()
    };

    for (int t = 0; t < toggles.length; t++) {
      String label = toggles[t][0];
      String state = toggles[t][1];
      String action = toggles[t][2];
      String stateColor = states[t] ? "#55FF55" : "#FF5555";

      cmd.appendInline("#IndexCards",
          "Group { Anchor: (Height: 32, Bottom: 4); Background: (Color: #141c26); LayoutMode: Left; "
          + "Padding: (Left: 10, Right: 10); "
          + "Label { Text: \"" + label + "\"; "
          + "Style: (FontSize: 12, TextColor: #e0e8f0, VerticalAlignment: Center); Anchor: (Width: 200); } "
          + "Group { FlexWeight: 1; } "
          + "TextButton #Evt" + action + " { Text: \"" + state + "\"; Anchor: (Width: 60, Height: 24); } }");

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#IndexCards[" + (t + 1) + "] #Evt" + action,
          EventData.of("Button", action), false
      );
    }

    // Divider
    cmd.appendInline("#IndexCards",
        "Group { Anchor: (Height: 1, Bottom: 8); Background: (Color: #2a3a4a); }");

    // Event announcements list
    List<AnnouncementEvent> evts = config.getEventAnnouncements();
    int baseIdx = toggles.length + 2; // 1 for back row, 3 for toggles, 1 for divider

    for (int i = 0; i < evts.size(); i++) {
      AnnouncementEvent evt = evts.get(i);
      String typeLabel = evt.eventType().replace("_", " ");
      String typeBadge = evt.type() == AnnouncementType.NOTIFICATION ? "NOTIF" : "CHAT";
      String enabledLabel = evt.enabled() ? "ON" : "OFF";
      String enabledColor = evt.enabled() ? "#55FF55" : "#FF5555";

      cmd.appendInline("#IndexCards",
          "Group { Anchor: (Height: 48, Bottom: 4); Background: (Color: #141c26); LayoutMode: Left; "
          + "Padding: (Left: 10, Right: 6); "
          + "Group { Anchor: (Width: 80); LayoutMode: Top; Padding: (Top: 6); "
          + "Label { Text: \"" + typeLabel + "\"; Style: (FontSize: 11, TextColor: #FFAA00, VerticalAlignment: Center); Anchor: (Height: 16); } "
          + "Label { Text: \"" + typeBadge + "\"; Style: (FontSize: 9, TextColor: #55FFFF, VerticalAlignment: Center); Anchor: (Height: 14); } } "
          + "Group { FlexWeight: 1; Padding: (Left: 4, Top: 8, Bottom: 8); "
          + "Label #EvtMsg { Text: \"" + escapeUi(UIHelper.truncate(evt.message(), 40)) + "\"; "
          + "Style: (FontSize: 10, TextColor: #e0e8f0, VerticalAlignment: Center); } } "
          + "Group { Anchor: (Width: 50); Padding: (Top: 12); "
          + "TextButton #EvtToggle { Text: \"" + enabledLabel + "\"; Anchor: (Width: 44, Height: 22); } } }");

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          "#IndexCards[" + (baseIdx + i) + "] #EvtToggle",
          EventData.of("Button", "ToggleEvent").append("Target", evt.id().toString()), false
      );
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

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "Create" -> handleCreate();
      case "Delete" -> handleDelete(data.target);
      case "Toggle" -> handleToggle(data.target);
      case "Edit" -> handleStartEdit(data.target);
      case "Preview" -> handlePreview(data.target);
      case "SaveEdit" -> handleSaveEdit(data);
      case "CancelEdit" -> handleCancelEdit();
      case "ToggleType" -> handleToggleType();
      case "ToggleEnabled" -> handleToggleEnabled();
      case "ShowEvents" -> handleShowEvents();
      case "BackToList" -> handleBackToList();
      case "ToggleJoin" -> handleToggleJoinMessages();
      case "ToggleLeave" -> handleToggleLeaveMessages();
      case "ToggleWelcome" -> handleToggleWelcomeMessages();
      case "ToggleEvent" -> handleToggleEventAnnouncement(data.target);
      default -> sendUpdate();
    }
  }

  private void handleCreate() {
    AnnouncementsConfig config = ConfigManager.get().announcements();
    int nextOrder = config.getAnnouncements().size();
    Announcement newAnn = new Announcement(
        UUID.randomUUID(), "New announcement message",
        AnnouncementType.CHAT, true, 0, null, null, null, nextOrder
    );
    config.getAnnouncements().add(newAnn);
    config.save();

    // Start editing the newly created announcement
    editingId = newAnn.id();
    viewMode = "edit";
    rebuildContent();
  }

  private void handleDelete(@Nullable String targetId) {
    if (targetId == null) return;

    try {
      UUID id = UUID.fromString(targetId);
      AnnouncementsConfig config = ConfigManager.get().announcements();
      config.getAnnouncements().removeIf(a -> a.id().equals(id));
      config.save();
      restartSchedulerIfNeeded();
    } catch (Exception e) {
      Logger.warn("[Announcements] Failed to delete: %s", e.getMessage());
    }
    rebuildContent();
  }

  private void handleToggle(@Nullable String targetId) {
    if (targetId == null) return;

    try {
      UUID id = UUID.fromString(targetId);
      AnnouncementsConfig config = ConfigManager.get().announcements();
      List<Announcement> list = config.getAnnouncements();
      for (int i = 0; i < list.size(); i++) {
        Announcement ann = list.get(i);
        if (ann.id().equals(id)) {
          list.set(i, ann.withEnabled(!ann.enabled()));
          break;
        }
      }
      config.save();
      restartSchedulerIfNeeded();
    } catch (Exception ignored) {}
    rebuildContent();
  }

  private void handleStartEdit(@Nullable String targetId) {
    if (targetId == null) return;

    try {
      editingId = UUID.fromString(targetId);
      viewMode = "edit";
    } catch (Exception e) {
      editingId = null;
      viewMode = "list";
    }
    rebuildContent();
  }

  private void handlePreview(@Nullable String targetId) {
    if (targetId == null || scheduler == null) return;

    try {
      UUID id = UUID.fromString(targetId);
      AnnouncementsConfig config = ConfigManager.get().announcements();
      Announcement ann = findById(config, id);
      if (ann != null) {
        scheduler.broadcastNow(ann.message(), ann.type());
      }
    } catch (Exception ignored) {}
  }

  private void handleSaveEdit(@NotNull AdminPageData data) {
    if (data.target == null || editingId == null) {
      editingId = null;
      viewMode = "list";
      rebuildContent();
      return;
    }

    try {
      UUID id = UUID.fromString(data.target);
      AnnouncementsConfig config = ConfigManager.get().announcements();
      List<Announcement> list = config.getAnnouncements();

      for (int i = 0; i < list.size(); i++) {
        Announcement ann = list.get(i);
        if (ann.id().equals(id)) {
          // Apply edits
          Announcement updated = ann;

          // Message text (mapped to @InputName)
          if (data.inputName != null && !data.inputName.isBlank()) {
            updated = updated.withMessage(data.inputName.trim());
          }

          // Order (mapped to @InputDescription)
          if (data.inputDescription != null && !data.inputDescription.isBlank()) {
            try {
              int order = Integer.parseInt(data.inputDescription.trim());
              updated = updated.withOrder(order);
            } catch (NumberFormatException ignored) {}
          }

          // Permission (mapped to @InputPermission)
          if (data.inputPermission != null) {
            String perm = data.inputPermission.trim();
            updated = updated.withPermission(perm.isEmpty() ? null : perm);
          }

          // World (mapped to @InputCategory)
          if (data.inputCategory != null) {
            String world = data.inputCategory.trim();
            updated = updated.withWorld(world.isEmpty() ? null : world);
          }

          // Cron (mapped to @InputCooldown)
          if (data.inputCooldown != null) {
            String cron = data.inputCooldown.trim();
            updated = updated.withCronExpression(cron.isEmpty() ? null : cron);
          }

          // Type (from toggle state)
          updated = updated.withType(AnnouncementType.values()[editTypeIndex]);

          // Enabled (from toggle state)
          updated = updated.withEnabled(editEnabledState);

          list.set(i, updated);
          break;
        }
      }

      config.save();
      restartSchedulerIfNeeded();
    } catch (Exception e) {
      Logger.warn("[Announcements] Failed to save edit: %s", e.getMessage());
    }

    editingId = null;
    viewMode = "list";
    rebuildContent();
  }

  private void handleCancelEdit() {
    editingId = null;
    viewMode = "list";
    rebuildContent();
  }

  private void handleToggleType() {
    editTypeIndex = (editTypeIndex + 1) % AnnouncementType.values().length;
    rebuildContent();
  }

  private void handleToggleEnabled() {
    editEnabledState = !editEnabledState;
    rebuildContent();
  }

  private void handleShowEvents() {
    viewMode = "events";
    rebuildContent();
  }

  private void handleBackToList() {
    viewMode = "list";
    rebuildContent();
  }

  private void handleToggleJoinMessages() {
    AnnouncementsConfig config = ConfigManager.get().announcements();
    config.setJoinMessagesEnabled(!config.isJoinMessagesEnabled());
    config.save();
    rebuildContent();
  }

  private void handleToggleLeaveMessages() {
    AnnouncementsConfig config = ConfigManager.get().announcements();
    config.setLeaveMessagesEnabled(!config.isLeaveMessagesEnabled());
    config.save();
    rebuildContent();
  }

  private void handleToggleWelcomeMessages() {
    AnnouncementsConfig config = ConfigManager.get().announcements();
    config.setWelcomeMessagesEnabled(!config.isWelcomeMessagesEnabled());
    config.save();
    rebuildContent();
  }

  private void handleToggleEventAnnouncement(@Nullable String targetId) {
    if (targetId == null) return;

    try {
      UUID id = UUID.fromString(targetId);
      AnnouncementsConfig config = ConfigManager.get().announcements();
      List<AnnouncementEvent> list = config.getEventAnnouncements();
      for (int i = 0; i < list.size(); i++) {
        AnnouncementEvent evt = list.get(i);
        if (evt.id().equals(id)) {
          list.set(i, evt.withEnabled(!evt.enabled()));
          break;
        }
      }
      config.save();
    } catch (Exception ignored) {}
    rebuildContent();
  }

  // =====================================================================
  // Helpers
  // =====================================================================

  @Nullable
  private Announcement findById(@NotNull AnnouncementsConfig config, @NotNull UUID id) {
    for (Announcement ann : config.getAnnouncements()) {
      if (ann.id().equals(id)) return ann;
    }
    return null;
  }

  private void restartSchedulerIfNeeded() {
    if (scheduler != null) {
      scheduler.restart();
    }
  }

  private String formatInterval(int seconds) {
    if (seconds < 60) return seconds + "s";
    if (seconds < 3600) return (seconds / 60) + "m";
    return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
  }

  private String escapeUi(@NotNull String text) {
    return text.replace("\"", "").replace("$", "");
  }

  private void rebuildContent() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildContent(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
