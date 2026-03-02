package com.hyperessentials.gui.admin;

import com.hyperessentials.BuildInfo;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.gui.GuiColors;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.module.Module;
import com.hyperessentials.module.ModuleRegistry;
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

import java.nio.file.Path;

/**
 * Admin settings page — version info, config reload, module status.
 */
public class AdminSettingsPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;
  private final ModuleRegistry moduleRegistry;
  private final Path dataDir;

  public AdminSettingsPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager,
      @NotNull ModuleRegistry moduleRegistry,
      @NotNull Path dataDir
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.moduleRegistry = moduleRegistry;
    this.dataDir = dataDir;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_SETTINGS);
    NavBarHelper.setupAdminBar(playerRef, "settings", guiManager.getAdminRegistry(), cmd, events);
    populateSettings(cmd, events);
  }

  private void populateSettings(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    // Version and data dir
    cmd.set("#VersionLabel.Text", "v" + BuildInfo.VERSION);
    cmd.set("#DataDirLabel.Text", "Data: " + dataDir.toAbsolutePath());

    // Reload button
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#ReloadBtn",
        EventData.of("Button", "Reload"), false
    );

    // Module status list
    cmd.clear("#ModuleList");
    int idx = 0;
    for (Module module : moduleRegistry.getModules()) {
      cmd.append("#ModuleList", UIPaths.ADMIN_MODULE_TOGGLE);
      String selector = "#ModuleList[" + idx + "]";

      cmd.set(selector + " #ModuleName.Text", module.getDisplayName());
      boolean enabled = module.isEnabled();
      cmd.set(selector + " #ModuleStatus.Text", enabled ? "Enabled" : "Disabled");
      cmd.set(selector + " #ModuleStatus.Style.TextColor", GuiColors.forModuleEnabled(enabled));
      cmd.set(selector + " #StatusDot.Background.Color", GuiColors.forModuleEnabled(enabled));
      idx++;
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

    if ("Reload".equals(data.button)) {
      ConfigManager.get().reloadAll();
      Logger.info("[Admin] Configuration reloaded by %s", playerRef.getUsername());
      rebuildSettings();
      return;
    }

    sendUpdate();
  }

  private void rebuildSettings() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    populateSettings(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
