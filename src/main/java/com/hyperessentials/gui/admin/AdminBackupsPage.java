package com.hyperessentials.gui.admin;

import com.hyperessentials.backup.BackupManager;
import com.hyperessentials.backup.BackupMetadata;
import com.hyperessentials.backup.BackupType;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.HEMessages;
import com.hyperessentials.util.Logger;
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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Admin Backups page - lists, creates, restores, and deletes data backups.
 */
public class AdminBackupsPage extends InteractiveCustomUIPage<AdminPageData> {

  private static final int BACKUPS_PER_PAGE = 8;

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;
  private final BackupManager backupManager;

  private int currentPage = 0;
  private final Set<String> expandedBackups = new HashSet<>();
  private String confirmingRestore = null;
  private String confirmingDelete = null;
  private String statusMessage = "";
  private boolean creating = false;

  @Nullable
  private BackupType filterType = null;

  public AdminBackupsPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager,
      @NotNull BackupManager backupManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.backupManager = backupManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_BACKUPS);
    NavBarHelper.setupAdminBar(playerRef, "backups", guiManager.getAdminRegistry(), cmd, events);
    buildBackupList(cmd, events);
  }

  private void buildBackupList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    List<BackupMetadata> allBackups = backupManager.listBackups();

    List<BackupMetadata> filteredBackups = filterType == null ? allBackups
        : allBackups.stream().filter(b -> b.type() == filterType).toList();

    // Filter dropdown
    cmd.set("#FilterDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString("All"), "all"),
        new DropdownEntryInfo(LocalizableString.fromString("Hourly"), "hourly"),
        new DropdownEntryInfo(LocalizableString.fromString("Daily"), "daily"),
        new DropdownEntryInfo(LocalizableString.fromString("Weekly"), "weekly"),
        new DropdownEntryInfo(LocalizableString.fromString("Manual"), "manual"),
        new DropdownEntryInfo(LocalizableString.fromString("Migration"), "migration")
    ));
    cmd.set("#FilterDropdown.Value", filterType == null ? "all" : filterType.getPrefix());
    events.addEventBinding(CustomUIEventBindingType.ValueChanged, "#FilterDropdown",
        EventData.of("Button", "FilterChanged")
            .append("@FilterValue", "#FilterDropdown.Value"), false);

    // Backup count
    String countText = filteredBackups.size() + " backup(s)";
    if (filterType != null) {
      countText += " / " + allBackups.size() + " total";
    }
    cmd.set("#BackupCount.Text", countText);

    // Create button
    if (!creating) {
      events.addEventBinding(CustomUIEventBindingType.Activating, "#CreateBackupBtn",
          EventData.of("Button", "Create")
              .append("@BackupInputName", "#BackupNameInput.Value"), false);
    }

    // Status
    cmd.set("#StatusMessage.Text", statusMessage);

    // Pagination
    int totalPages = Math.max(1, (int) Math.ceil((double) filteredBackups.size() / BACKUPS_PER_PAGE));
    if (currentPage >= totalPages) currentPage = totalPages - 1;
    if (currentPage < 0) currentPage = 0;

    int start = currentPage * BACKUPS_PER_PAGE;
    int end = Math.min(start + BACKUPS_PER_PAGE, filteredBackups.size());
    List<BackupMetadata> pageBackups = filteredBackups.subList(start, end);

    // Build list
    cmd.clear("#BackupListContainer");

    if (pageBackups.isEmpty()) {
      cmd.appendInline("#BackupListContainer",
          "Label { Text: \"No backups found.\"; "
          + "Style: (FontSize: 11, TextColor: #666666, HorizontalAlignment: Center); "
          + "Anchor: (Height: 40); }");
    } else {
      cmd.appendInline("#BackupListContainer", "Group #IndexCards { LayoutMode: Top; }");
      for (int i = 0; i < pageBackups.size(); i++) {
        buildBackupEntry(cmd, events, i, pageBackups.get(i));
      }
    }

    // Pagination controls
    cmd.set("#PageInfo.Text", "Page " + (currentPage + 1) + " / " + totalPages);

    cmd.set("#PrevBtn.Disabled", currentPage <= 0);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#PrevBtn",
        EventData.of("Button", "PrevPage").append("Page", String.valueOf(currentPage - 1)), false);

    cmd.set("#NextBtn.Disabled", currentPage >= totalPages - 1);
    events.addEventBinding(CustomUIEventBindingType.Activating, "#NextBtn",
        EventData.of("Button", "NextPage").append("Page", String.valueOf(currentPage + 1)), false);
  }

  private void buildBackupEntry(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events,
                                int index, @NotNull BackupMetadata backup) {
    String name = backup.name();
    boolean expanded = expandedBackups.contains(name);

    cmd.append("#IndexCards", UIPaths.ADMIN_BACKUP_ENTRY);
    String idx = "#IndexCards[" + index + "]";

    cmd.set(idx + " #BackupNameLabel.Text", formatBackupName(name));
    cmd.set(idx + " #BackupSizeLabel.Text", backup.getFormattedSize());
    cmd.set(idx + " #BackupTypeTag.Text", backup.type().getDisplayName());

    // Indicator bar color: green for manual, gold for automatic
    String indicatorColor = backup.type().name().equals("MANUAL") ? "#55FF55" : "#FFAA00";
    cmd.set(idx + " #IndicatorBar.Background.Color", indicatorColor);

    // Expand/collapse
    cmd.set(idx + " #ExpandBtn.Text", expanded ? "v" : ">");
    events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #ExpandBtn",
        EventData.of("Button", "Toggle").append("Target", name), false);

    if (expanded) {
      cmd.set(idx + " #DetailSection.Visible", true);
      cmd.set(idx + " #DetailTypeLabel.Text", HEMessages.get(playerRef, AdminKeys.Backups.DETAIL_TYPE, backup.type().getDisplayName()));
      cmd.set(idx + " #DetailCreatedLabel.Text", HEMessages.get(playerRef, AdminKeys.Backups.DETAIL_CREATED, backup.getFormattedTimestamp()));
      cmd.set(idx + " #DetailSizeLabel.Text", HEMessages.get(playerRef, AdminKeys.Backups.DETAIL_SIZE, backup.getFormattedSize()));

      // Restore
      if (name.equals(confirmingRestore)) {
        cmd.set(idx + " #RestoreWarning.Visible", true);
        cmd.set(idx + " #RestoreWarning.Text", HEMessages.get(playerRef, AdminKeys.Backups.RESTORE_WARNING));
        cmd.set(idx + " #RestoreBtn.Text", HEMessages.get(playerRef, AdminKeys.Backups.CONFIRM_RESTORE));
      } else {
        cmd.set(idx + " #RestoreBtn.Text", HEMessages.get(playerRef, AdminKeys.Backups.RESTORE));
      }
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #RestoreBtn",
          EventData.of("Button", "Restore").append("Target", name), false);

      // Delete
      if (name.equals(confirmingDelete)) {
        cmd.set(idx + " #DeleteBtn.Text", HEMessages.get(playerRef, AdminKeys.Backups.CONFIRM_DELETE));
      } else {
        cmd.set(idx + " #DeleteBtn.Text", HEMessages.get(playerRef, AdminKeys.Backups.DELETE));
      }
      events.addEventBinding(CustomUIEventBindingType.Activating, idx + " #DeleteBtn",
          EventData.of("Button", "Delete").append("Target", name), false);
    }
  }

  private String formatBackupName(@NotNull String name) {
    if (name.startsWith("backup_")) {
      return name.substring(7);
    }
    return name;
  }

  @Nullable
  private World resolveWorld() {
    UUID worldUuid = playerRef.getWorldUuid();
    if (worldUuid == null) return null;
    return Universe.get().getWorld(worldUuid);
  }

  private void rebuildOnWorldThread() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildBackupList(cmd, events);
    sendUpdate(cmd, events, false);
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
      return;
    }

    switch (data.button) {
      case "Create" -> handleCreate(data);
      case "Toggle" -> handleToggle(data);
      case "Restore" -> handleRestore(data);
      case "Delete" -> handleDelete(data);
      case "PrevPage", "NextPage" -> {
        currentPage = data.page;
        expandedBackups.clear();
        confirmingRestore = null;
        confirmingDelete = null;
        rebuildOnWorldThread();
      }
      case "FilterChanged" -> {
        if (data.filterValue != null) {
          filterType = "all".equals(data.filterValue) ? null : BackupType.fromPrefix(data.filterValue);
          currentPage = 0;
          expandedBackups.clear();
          confirmingRestore = null;
          confirmingDelete = null;
          rebuildOnWorldThread();
        }
      }
      default -> sendUpdate();
    }
  }

  private void handleCreate(@NotNull AdminPageData data) {
    if (creating) return;

    creating = true;
    String customName = data.backupInputName;
    statusMessage = "Creating backup...";
    rebuildOnWorldThread();

    backupManager.createBackup(BackupType.MANUAL, customName, playerRef.getUuid()).thenAccept(result -> {
      World world = resolveWorld();
      if (world == null) { creating = false; return; }
      world.execute(() -> {
        creating = false;
        if (result instanceof BackupManager.BackupResult.Success success) {
          statusMessage = "Backup created: " + success.metadata().name();
          Logger.info("[Backups] %s created manual backup: %s",
              playerRef.getUsername(), success.metadata().name());
        } else if (result instanceof BackupManager.BackupResult.Failure failure) {
          statusMessage = "Failed: " + failure.error();
        }
        rebuildOnWorldThread();
      });
    });
  }

  private void handleToggle(@NotNull AdminPageData data) {
    if (data.target == null) return;
    if (expandedBackups.contains(data.target)) {
      expandedBackups.remove(data.target);
    } else {
      expandedBackups.add(data.target);
    }
    confirmingRestore = null;
    confirmingDelete = null;
    rebuildOnWorldThread();
  }

  private void handleRestore(@NotNull AdminPageData data) {
    if (data.target == null) return;

    if (!data.target.equals(confirmingRestore)) {
      confirmingRestore = data.target;
      confirmingDelete = null;
      rebuildOnWorldThread();
      return;
    }

    confirmingRestore = null;
    statusMessage = "Restoring...";
    rebuildOnWorldThread();

    backupManager.createBackup(BackupType.MANUAL, "pre-restore-safety", playerRef.getUuid())
        .thenCompose(safetyResult -> backupManager.restoreBackup(data.target))
        .thenAccept(result -> {
          World world = resolveWorld();
          if (world == null) return;
          world.execute(() -> {
            if (result instanceof BackupManager.RestoreResult.Success success) {
              statusMessage = "Restored " + success.filesRestored() + " files from " + success.backupName()
                  + ". Reload config to apply changes.";
              Logger.info("[Backups] %s restored backup: %s (%d files)",
                  playerRef.getUsername(), success.backupName(), success.filesRestored());
            } else if (result instanceof BackupManager.RestoreResult.Failure failure) {
              statusMessage = "Restore failed: " + failure.error();
            }
            rebuildOnWorldThread();
          });
        });
  }

  private void handleDelete(@NotNull AdminPageData data) {
    if (data.target == null) return;

    if (!data.target.equals(confirmingDelete)) {
      confirmingDelete = data.target;
      confirmingRestore = null;
      rebuildOnWorldThread();
      return;
    }

    confirmingDelete = null;
    statusMessage = "Deleting...";
    rebuildOnWorldThread();

    backupManager.deleteBackup(data.target).thenAccept(success -> {
      World world = resolveWorld();
      if (world == null) return;
      world.execute(() -> {
        if (success) {
          statusMessage = "Deleted backup: " + data.target;
          expandedBackups.remove(data.target);
          Logger.info("[Backups] %s deleted backup: %s", playerRef.getUsername(), data.target);
        } else {
          statusMessage = "Failed to delete backup.";
        }
        rebuildOnWorldThread();
      });
    });
  }
}
