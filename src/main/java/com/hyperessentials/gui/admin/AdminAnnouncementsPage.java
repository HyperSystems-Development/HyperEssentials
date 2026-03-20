package com.hyperessentials.gui.admin;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.AnnouncementsConfig;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.HEMessages;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Admin announcements page — view current announcement messages and settings.
 */
public class AdminAnnouncementsPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;

  public AdminAnnouncementsPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_ANNOUNCEMENTS);
    NavBarHelper.setupAdminBar(playerRef, "announcements", guiManager.getAdminRegistry(), cmd, events);
    populateMessages(cmd);
  }

  private void populateMessages(@NotNull UICommandBuilder cmd) {
    AnnouncementsConfig config = ConfigManager.get().announcements();
    List<String> messages = config.getMessages();

    cmd.set("#MessageCount.Text", messages.size() + " message" + (messages.size() != 1 ? "s" : ""));

    int intervalSecs = config.getIntervalSeconds();
    if (intervalSecs > 0) {
      cmd.set("#IntervalLabel.Text", HEMessages.get(playerRef, AdminKeys.Announcements.INTERVAL_LABEL, formatInterval(intervalSecs)));
    } else {
      cmd.set("#IntervalLabel.Text", HEMessages.get(playerRef, AdminKeys.Announcements.DISABLED));
    }

    cmd.set("#ModeLabel.Text", config.isRandomize()
        ? HEMessages.get(playerRef, AdminKeys.Announcements.MODE_RANDOM)
        : HEMessages.get(playerRef, AdminKeys.Announcements.MODE_SEQUENTIAL));

    cmd.clear("#MessageList");
    cmd.appendInline("#MessageList", "Group #IndexCards { LayoutMode: Top; }");

    if (messages.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", "No Messages");
      cmd.set("#IndexCards[0] #EmptyMessage.Text", "Add messages in announcements.json config.");
      return;
    }

    int i = 0;
    for (String message : messages) {
      cmd.append("#IndexCards", UIPaths.ADMIN_ANNOUNCEMENT_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #EntryNumber.Text", "#" + (i + 1));
      cmd.set(idx + " #MessageText.Text", UIHelper.truncate(message, 60));

      i++;
    }
  }

  private String formatInterval(int seconds) {
    if (seconds < 60) return seconds + "s";
    if (seconds < 3600) return (seconds / 60) + "m";
    return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
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

    sendUpdate();
  }
}
