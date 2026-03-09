package com.hyperessentials.gui.player;

import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.HomeShareData;
import com.hyperessentials.gui.util.PlayerSearchUtil;
import com.hyperessentials.module.homes.HomeManager;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sub-page for managing who a home is shared with.
 * Player search with share/remove functionality.
 */
public class HomeSharePage extends InteractiveCustomUIPage<HomeShareData> {

  private final Player player;
  private final PlayerRef playerRef;
  private final HomeManager homeManager;
  private final String homeName;
  private final Runnable onBack;

  private Ref<EntityStore> lastRef;
  private Store<EntityStore> lastStore;

  public HomeSharePage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull HomeManager homeManager,
      @NotNull String homeName,
      @NotNull Runnable onBack
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, HomeShareData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.homeManager = homeManager;
    this.homeName = homeName;
    this.onBack = onBack;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    this.lastRef = ref;
    this.lastStore = store;

    cmd.append(UIPaths.HOME_SHARE_PAGE);
    cmd.set("#ShareTitle.Text", "Share: " + homeName);

    buildSharedList(cmd, events);

    // Search field event — fires on every activation of search button
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#SearchBtn",
        EventData.of("Button", "Search").append("@SearchText", "#SearchField.Value"),
        false
    );

    // Back button
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#BackBtn",
        EventData.of("Button", "Back"),
        false
    );
  }

  @Override
  public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                              @NotNull HomeShareData data) {
    super.handleDataEvent(ref, store, data);

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "Search" -> handleSearch(data.searchText);
      case "Add" -> handleAdd(data.target);
      case "Remove" -> handleRemove(data.target);
      case "Back" -> onBack.run();
      default -> sendUpdate();
    }
  }

  private void handleSearch(String query) {
    if (query == null || query.isBlank()) {
      rebuildContent(List.of());
      return;
    }

    UUID uuid = playerRef.getUuid();
    List<PlayerSearchUtil.SearchResult> results = PlayerSearchUtil.search(query, uuid);

    // Filter out players already shared with
    Set<UUID> alreadyShared = homeManager.getSharedWith(uuid, homeName);
    List<PlayerSearchUtil.SearchResult> filtered = results.stream()
        .filter(r -> !alreadyShared.contains(r.uuid()))
        .toList();

    rebuildContent(filtered);
  }

  private void handleAdd(String targetUuidStr) {
    if (targetUuidStr == null) return;

    try {
      UUID targetUuid = UUID.fromString(targetUuidStr);
      homeManager.shareHome(playerRef.getUuid(), homeName, targetUuid);
    } catch (IllegalArgumentException ignored) {}

    rebuildContent(List.of());
  }

  private void handleRemove(String targetUuidStr) {
    if (targetUuidStr == null) return;

    try {
      UUID targetUuid = UUID.fromString(targetUuidStr);
      homeManager.unshareHome(playerRef.getUuid(), homeName, targetUuid);
    } catch (IllegalArgumentException ignored) {}

    rebuildContent(List.of());
  }

  private void rebuildContent(List<PlayerSearchUtil.SearchResult> searchResults) {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();

    buildSearchResults(cmd, events, searchResults);
    buildSharedList(cmd, events);

    // Re-bind search event
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#SearchBtn",
        EventData.of("Button", "Search").append("@SearchText", "#SearchField.Value"),
        false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#BackBtn",
        EventData.of("Button", "Back"),
        false
    );

    sendUpdate(cmd, events, false);
  }

  private void buildSearchResults(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events,
                                  @NotNull List<PlayerSearchUtil.SearchResult> results) {
    cmd.clear("#SearchResults");
    cmd.appendInline("#SearchResults", "Group #ResultCards { LayoutMode: Top; }");

    if (results.isEmpty()) return;

    int i = 0;
    for (PlayerSearchUtil.SearchResult result : results) {
      if (i >= 10) break; // Limit displayed results

      cmd.append("#ResultCards", UIPaths.SHARE_SEARCH_RESULT);
      String idx = "#ResultCards[" + i + "]";

      cmd.set(idx + " #PlayerName.Text", result.username());

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #AddBtn",
          EventData.of("Button", "Add").append("Target", result.uuid().toString()),
          false
      );

      i++;
    }
  }

  private void buildSharedList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    UUID uuid = playerRef.getUuid();
    Set<UUID> sharedWith = homeManager.getSharedWith(uuid, homeName);

    cmd.set("#SharedCount.Text", "Shared with " + sharedWith.size() + " player" + (sharedWith.size() != 1 ? "s" : ""));

    cmd.clear("#SharedList");
    cmd.appendInline("#SharedList", "Group #SharedCards { LayoutMode: Top; }");

    if (sharedWith.isEmpty()) return;

    int i = 0;
    for (UUID targetUuid : sharedWith) {
      cmd.append("#SharedCards", UIPaths.SHARED_PLAYER_ENTRY);
      String idx = "#SharedCards[" + i + "]";

      String name = resolvePlayerName(targetUuid);
      cmd.set(idx + " #PlayerName.Text", name);

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #RemoveBtn",
          EventData.of("Button", "Remove").append("Target", targetUuid.toString()),
          false
      );

      i++;
    }
  }

  private String resolvePlayerName(@NotNull UUID uuid) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      PlayerRef ref = plugin.getTrackedPlayer(uuid);
      if (ref != null) {
        return ref.getUsername();
      }
    }
    return uuid.toString().substring(0, 8) + "...";
  }
}
