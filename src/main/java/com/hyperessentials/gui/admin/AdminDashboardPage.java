package com.hyperessentials.gui.admin;

import com.hyperessentials.BuildInfo;
import com.hyperessentials.gui.GuiColors;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.module.Module;
import com.hyperessentials.module.ModuleRegistry;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.module.spawns.SpawnsModule;
import com.hyperessentials.module.warps.WarpsModule;
import com.hyperessentials.platform.HyperEssentialsPlugin;
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

/**
 * Admin dashboard — server overview, stats cards, module status grid.
 */
public class AdminDashboardPage extends InteractiveCustomUIPage<AdminPageData> {

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;
  private final ModuleRegistry moduleRegistry;

  public AdminDashboardPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager,
      @NotNull ModuleRegistry moduleRegistry
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.moduleRegistry = moduleRegistry;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_DASHBOARD);
    NavBarHelper.setupAdminBar(playerRef, "dashboard", guiManager.getAdminRegistry(), cmd, events);
    populateDashboard(cmd);
  }

  private void populateDashboard(@NotNull UICommandBuilder cmd) {
    // Version info
    cmd.set("#VersionLabel.Text", "v" + BuildInfo.VERSION);

    // Online count
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      cmd.set("#OnlineCount.Text", String.valueOf(plugin.getTrackedPlayers().size()));
    }

    // Warp count
    WarpsModule warps = moduleRegistry.getModule(WarpsModule.class);
    if (warps != null && warps.isEnabled() && warps.getWarpManager() != null) {
      cmd.set("#WarpCount.Text", String.valueOf(warps.getWarpManager().getWarpCount()));
    }

    // Spawn count
    SpawnsModule spawns = moduleRegistry.getModule(SpawnsModule.class);
    if (spawns != null && spawns.isEnabled() && spawns.getSpawnManager() != null) {
      cmd.set("#SpawnCount.Text", String.valueOf(spawns.getSpawnManager().getSpawnCount()));
    }

    // Kit count
    KitsModule kits = moduleRegistry.getModule(KitsModule.class);
    if (kits != null && kits.isEnabled() && kits.getKitManager() != null) {
      cmd.set("#KitCount.Text", String.valueOf(kits.getKitManager().getAllKits().size()));
    }

    // Module status grid
    cmd.clear("#ModuleList");
    int idx = 0;
    for (Module module : moduleRegistry.getModules()) {
      cmd.append("#ModuleList", UIPaths.ADMIN_MODULE_CARD);
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

    sendUpdate();
  }
}
