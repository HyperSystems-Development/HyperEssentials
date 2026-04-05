package com.hyperessentials.gui.admin;

import com.hyperessentials.gui.GuiColors;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.module.moderation.ModerationManager;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.HEMessages;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.module.moderation.data.PunishmentType;
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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Admin moderation page — list punishments with search, sort, filter, pagination, and revoke.
 */
public class AdminModerationPage extends InteractiveCustomUIPage<AdminPageData> {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneId.systemDefault());

  private static final int ITEMS_PER_PAGE = 10;

  /** Sort modes for the punishment list. */
  private enum SortMode {
    NEWEST, OLDEST, PLAYER
  }

  private final PlayerRef playerRef;
  private final Player player;
  private final ModerationManager moderationManager;
  private final GuiManager guiManager;

  private int currentPage = 0;
  private @NotNull String searchQuery = "";
  private @NotNull SortMode sortMode = SortMode.NEWEST;
  private @Nullable PunishmentType filterType = null;  // null = all types
  private boolean filterActive = false;  // false = all, true = active only

  public AdminModerationPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull ModerationManager moderationManager,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.moderationManager = moderationManager;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_MODERATION);
    NavBarHelper.setupAdminBar(playerRef, "moderation", guiManager.getAdminRegistry(), cmd, events);
    buildPunishmentList(cmd, events);
  }

  private void buildPunishmentList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    // Search input placeholder
    cmd.set("#SearchInput.PlaceholderText", "Search player...");

    // Wire search — triggers on change, reads current search text
    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged, "#SearchInput",
        EventData.of("Button", "SearchChanged")
            .append("@SearchInput", "#SearchInput.Value")
            .append("@SortMode", "#SortDropdown.Value")
            .append("@FilterValue", "#TypeDropdown.Value"),
        false
    );

    // Populate sort dropdown: Newest, Oldest, Player
    cmd.set("#SortDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString("Newest"), "NEWEST"),
        new DropdownEntryInfo(LocalizableString.fromString("Oldest"), "OLDEST"),
        new DropdownEntryInfo(LocalizableString.fromString("Player"), "PLAYER")
    ));
    cmd.set("#SortDropdown.Value", sortMode.name());

    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged, "#SortDropdown",
        EventData.of("Button", "SortChanged")
            .append("@SortMode", "#SortDropdown.Value")
            .append("@SearchInput", "#SearchInput.Value")
            .append("@FilterValue", "#TypeDropdown.Value"),
        false
    );

    // Populate type filter dropdown: All, Ban, IP Ban, Mute, Kick, Warn
    cmd.set("#TypeDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString("All"), "ALL"),
        new DropdownEntryInfo(LocalizableString.fromString("Ban"), "BAN"),
        new DropdownEntryInfo(LocalizableString.fromString("IP Ban"), "IPBAN"),
        new DropdownEntryInfo(LocalizableString.fromString("Mute"), "MUTE"),
        new DropdownEntryInfo(LocalizableString.fromString("Kick"), "KICK"),
        new DropdownEntryInfo(LocalizableString.fromString("Warn"), "WARN")
    ));
    cmd.set("#TypeDropdown.Value", filterType == null ? "ALL" : filterType.name());

    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged, "#TypeDropdown",
        EventData.of("Button", "TypeFilterChanged")
            .append("@FilterValue", "#TypeDropdown.Value")
            .append("@SortMode", "#SortDropdown.Value")
            .append("@SearchInput", "#SearchInput.Value"),
        false
    );

    // Populate active filter dropdown: All, Active Only
    cmd.set("#ActiveDropdown.Entries", List.of(
        new DropdownEntryInfo(LocalizableString.fromString("All"), "ALL"),
        new DropdownEntryInfo(LocalizableString.fromString("Active Only"), "ACTIVE")
    ));
    cmd.set("#ActiveDropdown.Value", filterActive ? "ACTIVE" : "ALL");

    events.addEventBinding(
        CustomUIEventBindingType.ValueChanged, "#ActiveDropdown",
        EventData.of("Button", "ActiveFilterChanged")
            .append("@FilterValue", "#ActiveDropdown.Value")
            .append("@SortMode", "#SortDropdown.Value")
            .append("@SearchInput", "#SearchInput.Value"),
        false
    );

    // Player Search button (navigates to AdminPlayerModerationPage)
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#PlayerSearchBtn",
        EventData.of("Button", "PlayerSearch"), false
    );

    // Fetch all punishments (false = all, not just active — we filter ourselves)
    List<Punishment> punishments = new ArrayList<>(moderationManager.getAllPunishments(false));

    // Apply active filter
    if (filterActive) {
      punishments.removeIf(p -> !p.isEffective());
    }

    // Apply type filter
    if (filterType != null) {
      punishments.removeIf(p -> p.type() != filterType);
    }

    // Apply search filter (match player name)
    if (!searchQuery.isEmpty()) {
      String query = searchQuery.toLowerCase();
      punishments.removeIf(p -> !p.playerName().toLowerCase().contains(query));
    }

    // Apply sort
    switch (sortMode) {
      case NEWEST -> punishments.sort(Comparator.comparing(Punishment::issuedAt).reversed());
      case OLDEST -> punishments.sort(Comparator.comparing(Punishment::issuedAt));
      case PLAYER -> punishments.sort(Comparator.comparing(Punishment::playerName, String.CASE_INSENSITIVE_ORDER));
    }

    // Calculate pagination
    int totalItems = punishments.size();
    int totalPages = Math.max(1, (totalItems + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
    if (currentPage >= totalPages) currentPage = totalPages - 1;
    if (currentPage < 0) currentPage = 0;

    int startIdx = currentPage * ITEMS_PER_PAGE;
    int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, totalItems);
    List<Punishment> pageItems = totalItems > 0
        ? punishments.subList(startIdx, endIdx)
        : List.of();

    // Count label
    String label = totalItems + " punishment" + (totalItems != 1 ? "s" : "");
    if (filterActive) label += " (active)";
    cmd.set("#PunishmentCount.Text", label);

    // Pagination info
    cmd.set("#PageInfo.Text", "Page " + (currentPage + 1) + " / " + totalPages);

    // Pagination buttons
    cmd.set("#PrevBtn.Disabled", currentPage <= 0);
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#PrevBtn",
        EventData.of("Button", "PrevPage")
            .append("@SearchInput", "#SearchInput.Value")
            .append("@SortMode", "#SortDropdown.Value")
            .append("@FilterValue", "#TypeDropdown.Value"),
        false
    );
    cmd.set("#NextBtn.Disabled", currentPage >= totalPages - 1);
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#NextBtn",
        EventData.of("Button", "NextPage")
            .append("@SearchInput", "#SearchInput.Value")
            .append("@SortMode", "#SortDropdown.Value")
            .append("@FilterValue", "#TypeDropdown.Value"),
        false
    );

    // Build punishment list
    cmd.clear("#PunishmentList");
    cmd.appendInline("#PunishmentList", "Group #IndexCards { LayoutMode: Top; }");

    if (pageItems.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", filterActive
          ? HEMessages.get(playerRef, AdminKeys.Moderation.EMPTY_ACTIVE_TITLE)
          : HEMessages.get(playerRef, AdminKeys.Moderation.EMPTY_ALL_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, AdminKeys.Moderation.EMPTY_MESSAGE));
      return;
    }

    int i = 0;
    for (Punishment p : pageItems) {
      cmd.append("#IndexCards", UIPaths.ADMIN_PUNISHMENT_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      // Type badge and color
      String typeLabel = p.type().name();
      String typeColor = switch (p.type()) {
        case BAN -> GuiColors.DANGER;
        case IPBAN -> "#AA0000";
        case MUTE -> GuiColors.WARNING;
        case KICK -> GuiColors.INFO;
        case WARN -> "#FFFF55";
      };
      cmd.set(idx + " #TypeBadge.Text", typeLabel);
      cmd.set(idx + " #TypeBadge.Style.TextColor", typeColor);
      cmd.set(idx + " #TypeBar.Background.Color", typeColor);

      cmd.set(idx + " #PlayerName.Text", p.playerName());
      cmd.set(idx + " #IssuerName.Text", "by " + p.issuerName());
      cmd.set(idx + " #Reason.Text", p.reason() != null ? p.reason() : HEMessages.get(playerRef, AdminKeys.Moderation.NO_REASON));

      // Expires info
      if (p.isEffective()) {
        if (p.isPermanent()) {
          cmd.set(idx + " #Expires.Text", HEMessages.get(playerRef, AdminKeys.Moderation.PERMANENT));
        } else {
          cmd.set(idx + " #Expires.Text", UIHelper.formatDuration((int) (p.getRemainingMillis() / 1000)));
        }
      } else {
        cmd.set(idx + " #Expires.Text", p.active()
            ? HEMessages.get(playerRef, AdminKeys.Moderation.EXPIRED)
            : HEMessages.get(playerRef, AdminKeys.Moderation.REVOKED));
      }

      // Only show revoke button for active punishments (not kicks/warns)
      if (p.isEffective() && p.type() != PunishmentType.KICK && p.type() != PunishmentType.WARN) {
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #RevokeBtn",
            EventData.of("Button", "Revoke").append("Target", p.id().toString())
                .append("@SearchInput", "#SearchInput.Value")
                .append("@SortMode", "#SortDropdown.Value")
                .append("@FilterValue", "#TypeDropdown.Value"),
            false
        );
      }

      i++;
    }
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

    // Capture search input from any event that includes it
    if (data.inputSearch != null) {
      searchQuery = data.inputSearch.isBlank() ? "" : data.inputSearch.trim();
    }

    // Capture sort mode from any event that includes it
    if (data.sortMode != null) {
      try {
        sortMode = SortMode.valueOf(data.sortMode);
      } catch (IllegalArgumentException ignored) {}
    }

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "SearchChanged" -> { currentPage = 0; rebuildList(); }
      case "SortChanged" -> { rebuildList(); }
      case "TypeFilterChanged" -> {
        parseTypeFilter(data.filterValue);
        currentPage = 0;
        rebuildList();
      }
      case "ActiveFilterChanged" -> {
        parseActiveFilter(data.filterValue);
        currentPage = 0;
        rebuildList();
      }
      case "PrevPage" -> {
        if (currentPage > 0) currentPage--;
        rebuildList();
      }
      case "NextPage" -> {
        currentPage++;
        rebuildList();
      }
      case "Revoke" -> { handleRevoke(data.target); }
      case "PlayerSearch" -> { openPlayerModeration(ref, store); }
      default -> sendUpdate();
    }
  }

  /**
   * Parse the type dropdown value into a PunishmentType filter.
   * "ALL" = no filter, otherwise matches PunishmentType name (BAN, IPBAN, MUTE, KICK, WARN).
   */
  private void parseTypeFilter(@Nullable String value) {
    if (value == null || "ALL".equals(value)) { filterType = null; return; }
    try {
      filterType = PunishmentType.valueOf(value);
    } catch (IllegalArgumentException e) {
      filterType = null;
    }
  }

  /**
   * Parse the active dropdown value. "ALL" = show all, "ACTIVE" = active only.
   */
  private void parseActiveFilter(@Nullable String value) {
    filterActive = "ACTIVE".equals(value);
  }

  private void handleRevoke(String punishmentIdStr) {
    if (punishmentIdStr == null) return;

    try {
      UUID punishmentId = UUID.fromString(punishmentIdStr);
      // Find the punishment and revoke it based on type
      List<Punishment> all = moderationManager.getAllPunishments(true);
      for (Punishment p : all) {
        if (p.id().equals(punishmentId)) {
          if (p.type() == PunishmentType.BAN) {
            moderationManager.unban(p.playerUuid(), playerRef.getUuid(), playerRef.getUsername());
          } else if (p.type() == PunishmentType.MUTE) {
            moderationManager.unmute(p.playerUuid(), playerRef.getUuid(), playerRef.getUsername());
          }
          break;
        }
      }
    } catch (IllegalArgumentException ignored) {
    }

    rebuildList();
  }

  private void openPlayerModeration(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store) {
    AdminPlayerModerationPage page = new AdminPlayerModerationPage(player, playerRef, guiManager);
    player.getPageManager().openCustomPage(ref, store, page);
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildPunishmentList(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
