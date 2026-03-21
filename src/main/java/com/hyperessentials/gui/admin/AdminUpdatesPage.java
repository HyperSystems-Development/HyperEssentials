package com.hyperessentials.gui.admin;

import com.hyperessentials.BuildInfo;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
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

/**
 * Admin Updates page — shows version info and update status.
 */
public class AdminUpdatesPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;

  public AdminUpdatesPage(
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
    cmd.append(UIPaths.ADMIN_UPDATES);
    NavBarHelper.setupAdminBar(playerRef, "updates", guiManager.getAdminRegistry(), cmd, events);

    cmd.set("#VersionLabel.Text", "v" + BuildInfo.VERSION);
    cmd.set("#BuildDateLabel.Text", java.time.Instant.ofEpochMilli(BuildInfo.BUILD_TIMESTAMP)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString());
    cmd.set("#ServerVersionLabel.Text", "Java " + BuildInfo.JAVA_VERSION);

    cmd.set("#UpdateStatusLabel.Text", "Up to date");
    cmd.set("#UpdateStatusLabel.Style.TextColor", "#55FF55");

    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#CheckUpdatesBtn",
        EventData.of("Button", "CheckUpdates"), false
    );
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

    if ("CheckUpdates".equals(data.button)) {
      // Placeholder — update check not yet implemented
      UICommandBuilder cmd = new UICommandBuilder();
      cmd.set("#UpdateStatusLabel.Text", "Up to date");
      cmd.set("#UpdateStatusLabel.Style.TextColor", "#55FF55");
      sendUpdate(cmd, new UIEventBuilder(), false);
      return;
    }

    sendUpdate();
  }
}
