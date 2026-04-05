package com.hyperessentials.gui.player;

import com.hyperessentials.data.TeleportRequest;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.RefreshablePage;
import com.hyperessentials.gui.data.PlayerPageData;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.GuiKeys;
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

import java.util.List;
import java.util.UUID;

/**
 * Player TPA page — list incoming TPA requests with accept/deny, toggle TPA acceptance.
 */
public class TpaPage extends InteractiveCustomUIPage<PlayerPageData> implements RefreshablePage {

  private final PlayerRef playerRef;
  private final Player player;
  private final TpaManager tpaManager;
  private final GuiManager guiManager;

  public TpaPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull TpaManager tpaManager,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.tpaManager = tpaManager;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.TPA_PAGE);
    NavBarHelper.setupBar(playerRef, "tpa", guiManager.getPlayerRegistry(), cmd, events);
    buildRequestList(cmd, events);

    guiManager.getPageTracker().register(playerRef.getUuid(), "tpa", this);
  }

  @Override
  public void onDismiss(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store) {
    guiManager.getPageTracker().unregister(playerRef.getUuid());
  }

  @Override
  public void refreshContent() {
    rebuildList();
  }

  private void buildRequestList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    UUID uuid = playerRef.getUuid();

    // Toggle button
    boolean accepting = tpaManager.isAcceptingRequests(uuid);
    cmd.set("#ToggleBtn.Text", HEMessages.get(playerRef, accepting ? GuiKeys.Tpa.TOGGLE_ENABLED : GuiKeys.Tpa.TOGGLE_DISABLED));
    cmd.set("#ToggleLabel.Text", HEMessages.get(playerRef, accepting ? GuiKeys.Tpa.TOGGLE_LABEL_ON : GuiKeys.Tpa.TOGGLE_LABEL_OFF));

    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#ToggleBtn",
        EventData.of("Button", "Toggle"), false
    );

    // Request list
    List<TeleportRequest> incoming = tpaManager.getIncomingRequests(uuid);
    cmd.set("#RequestCount.Text", HEMessages.get(playerRef,
        incoming.size() != 1 ? GuiKeys.Tpa.REQUEST_COUNT_PLURAL : GuiKeys.Tpa.REQUEST_COUNT, incoming.size()));

    cmd.clear("#RequestList");
    cmd.appendInline("#RequestList", "Group #IndexCards { LayoutMode: Top; }");

    if (incoming.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", HEMessages.get(playerRef, GuiKeys.Tpa.EMPTY_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, GuiKeys.Tpa.EMPTY_MESSAGE));
      return;
    }

    int i = 0;
    for (TeleportRequest request : incoming) {
      cmd.append("#IndexCards", UIPaths.TPA_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      // Resolve requester name
      String requesterName = resolvePlayerName(request.requester());
      cmd.set(idx + " #RequesterName.Text", requesterName);

      // Request type
      String typeLabel = HEMessages.get(playerRef, request.type() == TeleportRequest.Type.TPA ? GuiKeys.Tpa.TYPE_TPA : GuiKeys.Tpa.TYPE_TPAHERE);
      cmd.set(idx + " #RequestType.Text", typeLabel);

      // Time remaining
      long remainingMs = request.getRemainingTime();
      if (remainingMs > 0) {
        int remainingSecs = (int) (remainingMs / 1000);
        cmd.set(idx + " #TimeRemaining.Text", HEMessages.get(playerRef, GuiKeys.Tpa.TIME_REMAINING, remainingSecs));
      }

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #AcceptBtn",
          EventData.of("Button", "Accept").append("Target", request.requester().toString()),
          false
      );

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #DenyBtn",
          EventData.of("Button", "Deny").append("Target", request.requester().toString()),
          false
      );

      i++;
    }
  }

  @Override
  public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                              @NotNull PlayerPageData data) {
    super.handleDataEvent(ref, store, data);

    if ("Nav".equals(data.button)) {
      NavBarHelper.handleNavEvent(
          data.navTarget != null ? data.navTarget : "",
          player, ref, store, playerRef, guiManager, GuiType.PLAYER
      );
      return;
    }

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "Accept" -> handleAccept(data.target);
      case "Deny" -> handleDeny(data.target);
      case "Toggle" -> handleToggle();
      default -> sendUpdate();
    }
  }

  private void handleAccept(String requesterUuidStr) {
    if (requesterUuidStr == null) return;

    try {
      UUID requesterUuid = UUID.fromString(requesterUuidStr);
      List<TeleportRequest> incoming = tpaManager.getIncomingRequests(playerRef.getUuid());
      for (TeleportRequest request : incoming) {
        if (request.requester().equals(requesterUuid)) {
          tpaManager.acceptRequest(request);
          break;
        }
      }
    } catch (IllegalArgumentException ignored) {
    }

    rebuildList();
  }

  private void handleDeny(String requesterUuidStr) {
    if (requesterUuidStr == null) return;

    try {
      UUID requesterUuid = UUID.fromString(requesterUuidStr);
      List<TeleportRequest> incoming = tpaManager.getIncomingRequests(playerRef.getUuid());
      for (TeleportRequest request : incoming) {
        if (request.requester().equals(requesterUuid)) {
          tpaManager.denyRequest(request);
          break;
        }
      }
    } catch (IllegalArgumentException ignored) {
    }

    rebuildList();
  }

  private void handleToggle() {
    UUID uuid = playerRef.getUuid();
    // TpaManager stores toggle state — toggle it
    tpaManager.toggleTpToggle(uuid);
    rebuildList();
  }

  private String resolvePlayerName(UUID uuid) {
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      PlayerRef ref = plugin.getTrackedPlayer(uuid);
      if (ref != null) {
        return ref.getUsername();
      }
    }
    return uuid.toString().substring(0, 8) + "...";
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildRequestList(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
