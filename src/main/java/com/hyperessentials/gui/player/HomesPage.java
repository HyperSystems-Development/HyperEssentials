package com.hyperessentials.gui.player;

import com.hyperessentials.Permissions;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.data.Home;
import com.hyperessentials.data.Location;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.RefreshablePage;
import com.hyperessentials.gui.data.PlayerPageData;
import com.hyperessentials.integration.FactionTerritoryChecker;
import com.hyperessentials.util.GuiKeys;
import com.hyperessentials.util.HEMessages;
import com.hyperessentials.integration.HyperFactionsIntegration;
import com.hyperessentials.module.homes.HomeManager;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Player homes page — browse, teleport to, share, and delete homes.
 * Also shows homes shared from other players.
 */
public class HomesPage extends InteractiveCustomUIPage<PlayerPageData> implements RefreshablePage {

  private final PlayerRef playerRef;
  private final Player player;
  private final HomeManager homeManager;
  private final WarmupManager warmupManager;
  private final GuiManager guiManager;
  private final Set<String> expandedHomes = new HashSet<>();

  // Cooldown tracking for lightweight tick refresh
  private record CooldownEntry(int index, boolean isFaction) {}
  private final List<CooldownEntry> cooldownEntries = new ArrayList<>();

  private Ref<EntityStore> lastRef;
  private Store<EntityStore> lastStore;

  public HomesPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull HomeManager homeManager,
      @NotNull WarmupManager warmupManager,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.homeManager = homeManager;
    this.warmupManager = warmupManager;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    this.lastRef = ref;
    this.lastStore = store;

    cmd.append(UIPaths.HOMES_PAGE);
    NavBarHelper.setupBar(playerRef, "homes", guiManager.getPlayerRegistry(), cmd, events);
    buildHomeList(cmd, events);

    guiManager.getPageTracker().register(playerRef.getUuid(), "homes", this);
  }

  @Override
  public void onDismiss(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store) {
    guiManager.getPageTracker().unregister(playerRef.getUuid());
  }

  @Override
  public void refreshContent() {
    rebuildList();
  }

  @Override
  public void refreshCooldowns() {
    if (cooldownEntries.isEmpty()) return;

    UUID uuid = playerRef.getUuid();
    UICommandBuilder cmd = new UICommandBuilder();
    boolean needsRebuild = false;

    for (CooldownEntry entry : cooldownEntries) {
      String idx = "#IndexCards[" + entry.index() + "]";
      int remaining;

      if (entry.isFaction()) {
        remaining = HyperFactionsIntegration.getFactionHomeCooldownRemaining(uuid);
      } else {
        remaining = warmupManager.getRemainingCooldown(uuid, "homes", "home");
      }

      if (remaining > 0) {
        cmd.set(idx + " #HomeCooldown.Text", "Cooldown: " + UIHelper.formatDuration(remaining));
      } else {
        // Cooldown expired — full rebuild to re-enable buttons and restore coords
        needsRebuild = true;
        break;
      }
    }

    if (needsRebuild) {
      refreshContent();
    } else {
      sendUpdate(cmd, null, false);
    }
  }

  private void buildHomeList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    UUID uuid = playerRef.getUuid();
    Collection<Home> homes = homeManager.getHomes(uuid);
    int limit = homeManager.getHomeLimit(uuid);
    boolean canShare = CommandUtil.hasPermission(uuid, Permissions.HOME_SHARE);

    cmd.set("#HomeCount.Text", homes.size() + " / " + UIHelper.formatLimit(limit) + " homes");

    cmd.clear("#HomeList");
    cmd.appendInline("#HomeList", "Group #IndexCards { LayoutMode: Top; }");
    cooldownEntries.clear();

    int i = 0;

    // Faction home entry (if available)
    if (HyperFactionsIntegration.isAvailable() && HyperFactionsIntegration.hasFactionHome(uuid)) {
      String factionWorld = HyperFactionsIntegration.getFactionHomeWorld(uuid);
      double[] factionCoords = HyperFactionsIntegration.getFactionHomeCoords(uuid);

      if (factionWorld != null && factionCoords != null) {
        cmd.append("#IndexCards", UIPaths.FACTION_HOME_ENTRY);
        String idx = "#IndexCards[" + i + "]";

        cmd.set(idx + " #HomeWorld.Text", UIHelper.formatWorldName(factionWorld));
        cmd.set(idx + " #HomeCoords.Text", UIHelper.formatCoords(factionCoords[0], factionCoords[1], factionCoords[2]));

        // Zone flag check (faction home destination)
        boolean factionHomeZoneBlocked = FactionTerritoryChecker.checkZoneFlag(
            factionWorld, factionCoords[0], factionCoords[2], HyperFactionsIntegration.FLAG_HOMES)
            != FactionTerritoryChecker.Result.ALLOWED;

        int factionCooldown = HyperFactionsIntegration.getFactionHomeCooldownRemaining(uuid);
        if (factionHomeZoneBlocked) {
          cmd.set(idx + " #TeleportBtn.Disabled", true);
          cmd.set(idx + " #TeleportBtn.Text", HEMessages.get(playerRef, GuiKeys.Homes.ZONE_RESTRICTED));
        } else if (factionCooldown > 0) {
          cmd.set(idx + " #TeleportBtn.Disabled", true);
          cmd.set(idx + " #TeleportBtn.Text", HEMessages.get(playerRef, GuiKeys.Homes.COOLDOWN));
          cmd.set(idx + " #HomeCooldown.Visible", true);
          cmd.set(idx + " #HomeCooldown.Text", HEMessages.get(playerRef, GuiKeys.Homes.COOLDOWN_LABEL, UIHelper.formatDuration(factionCooldown)));
          cmd.set(idx + " #HomeCoords.Visible", false);
          cooldownEntries.add(new CooldownEntry(i, true));
        }

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #TeleportBtn",
            EventData.of("Button", "FactionHome"),
            false
        );

        i++;
      }
    }

    if (homes.isEmpty() && i == 0) {
      // Check for shared homes before showing empty state
      List<HomeManager.SharedHome> sharedHomes = homeManager.getSharedHomes(uuid);
      if (sharedHomes.isEmpty()) {
        cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
        cmd.set("#IndexCards[" + i + "] #EmptyTitle.Text", HEMessages.get(playerRef, GuiKeys.Homes.EMPTY_TITLE));
        cmd.set("#IndexCards[" + i + "] #EmptyMessage.Text", HEMessages.get(playerRef, GuiKeys.Homes.EMPTY_MESSAGE));
        return;
      }
    }

    List<Home> sorted = new ArrayList<>(homes);
    sorted.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

    // Cooldown is module-level (same for all homes) — check once
    int cooldownSecs = warmupManager.getRemainingCooldown(uuid, "homes", "home");

    String defaultHomeName = homeManager.getDefaultHome(uuid);

    for (Home home : sorted) {
      cmd.append("#IndexCards", UIPaths.HOME_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      cmd.set(idx + " #HomeName.Text", home.name());
      cmd.set(idx + " #HomeWorld.Text", UIHelper.formatWorldName(home.world()));
      cmd.set(idx + " #HomeCoords.Text", UIHelper.formatCoords(home.x(), home.y(), home.z()));

      boolean isDefault = defaultHomeName != null && defaultHomeName.equalsIgnoreCase(home.name());
      boolean isExpanded = expandedHomes.contains(home.name().toLowerCase());

      // Default badge on header
      if (isDefault) {
        cmd.set(idx + " #DefaultBadge.Visible", true);
      }

      // Zone flag check (home destination)
      boolean homeZoneBlocked = FactionTerritoryChecker.checkZoneFlag(
          home.world(), home.x(), home.z(), HyperFactionsIntegration.FLAG_HOMES)
          != FactionTerritoryChecker.Result.ALLOWED;

      // Disable teleport button if zone-restricted or on cooldown
      if (homeZoneBlocked) {
        cmd.set(idx + " #TeleportBtn.Disabled", true);
        cmd.set(idx + " #TeleportBtn.Text", HEMessages.get(playerRef, GuiKeys.Homes.ZONE_RESTRICTED));
      } else if (cooldownSecs > 0) {
        cmd.set(idx + " #TeleportBtn.Disabled", true);
        cmd.set(idx + " #TeleportBtn.Text", HEMessages.get(playerRef, GuiKeys.Homes.COOLDOWN));
        cmd.set(idx + " #HomeCooldown.Visible", true);
        cmd.set(idx + " #HomeCooldown.Text", HEMessages.get(playerRef, GuiKeys.Homes.COOLDOWN_LABEL, UIHelper.formatDuration(cooldownSecs)));
        cmd.set(idx + " #HomeCoords.Visible", false);
        cooldownEntries.add(new CooldownEntry(i, false));
      }

      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #TeleportBtn",
          EventData.of("Button", "Teleport").append("Target", home.name()),
          false
      );

      // Expand/collapse toggle via header overlay
      events.addEventBinding(
          CustomUIEventBindingType.Activating,
          idx + " #Header",
          EventData.of("Button", "Toggle").append("Target", home.name()),
          false
      );

      // Expand/collapse state
      if (isExpanded) {
        cmd.set(idx + " #ExpandIcon.Visible", false);
        cmd.set(idx + " #CollapseIcon.Visible", true);
        cmd.set(idx + " #ExtendedInfo.Visible", true);

        // Default home button or badge
        if (isDefault) {
          cmd.set(idx + " #SetDefaultBtn.Visible", false);
          cmd.set(idx + " #DefaultLabel.Visible", true);
        } else {
          events.addEventBinding(
              CustomUIEventBindingType.Activating,
              idx + " #SetDefaultBtn",
              EventData.of("Button", "SetDefault").append("Target", home.name()),
              false
          );
        }

        // Share button — show only if player has permission
        if (canShare) {
          cmd.set(idx + " #ShareBtn.Visible", true);
          events.addEventBinding(
              CustomUIEventBindingType.Activating,
              idx + " #ShareBtn",
              EventData.of("Button", "Share").append("Target", home.name()),
              false
          );
        }

        // Delete button
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #DeleteBtn",
            EventData.of("Button", "Delete").append("Target", home.name()),
            false
        );
      }

      i++;
    }

    // Shared homes section (homes shared TO this player from others)
    List<HomeManager.SharedHome> sharedHomes = homeManager.getSharedHomes(uuid);
    if (!sharedHomes.isEmpty()) {
      sharedHomes.sort((a, b) -> a.home().name().compareToIgnoreCase(b.home().name()));

      for (HomeManager.SharedHome shared : sharedHomes) {
        cmd.append("#IndexCards", UIPaths.SHARED_HOME_ENTRY);
        String idx = "#IndexCards[" + i + "]";

        cmd.set(idx + " #HomeName.Text", shared.home().name());
        cmd.set(idx + " #OwnerName.Text", "Shared by " + resolvePlayerName(shared.ownerUuid()));
        cmd.set(idx + " #HomeCoords.Text",
            UIHelper.formatCoords(shared.home().x(), shared.home().y(), shared.home().z()));

        // Zone flag check (shared home destination)
        boolean sharedZoneBlocked = FactionTerritoryChecker.checkZoneFlag(
            shared.home().world(), shared.home().x(), shared.home().z(),
            HyperFactionsIntegration.FLAG_HOMES) != FactionTerritoryChecker.Result.ALLOWED;

        // Cooldown applies to shared home teleports too
        if (sharedZoneBlocked) {
          cmd.set(idx + " #TeleportBtn.Disabled", true);
          cmd.set(idx + " #TeleportBtn.Text", HEMessages.get(playerRef, GuiKeys.Homes.ZONE_RESTRICTED));
        } else if (cooldownSecs > 0) {
          cmd.set(idx + " #TeleportBtn.Disabled", true);
          cmd.set(idx + " #TeleportBtn.Text", HEMessages.get(playerRef, GuiKeys.Homes.COOLDOWN));
          cmd.set(idx + " #HomeCooldown.Visible", true);
          cmd.set(idx + " #HomeCooldown.Text", HEMessages.get(playerRef, GuiKeys.Homes.COOLDOWN_LABEL, UIHelper.formatDuration(cooldownSecs)));
          cmd.set(idx + " #HomeCoords.Visible", false);
          cooldownEntries.add(new CooldownEntry(i, false));
        }

        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #TeleportBtn",
            EventData.of("Button", "TeleportShared")
                .append("Target", shared.ownerUuid() + ":" + shared.home().name()),
            false
        );

        i++;
      }
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
      case "Teleport" -> handleTeleport(ref, data.target);
      case "TeleportShared" -> handleTeleportShared(ref, data.target);
      case "Delete" -> handleDelete(ref, store, data.target);
      case "Share" -> handleShare(data.target);
      case "FactionHome" -> handleFactionHome(ref);
      case "Toggle" -> handleToggle(data.target);
      case "SetDefault" -> handleSetDefault(data.target);
      default -> sendUpdate();
    }
  }

  private void handleToggle(String homeName) {
    if (homeName == null) return;
    String key = homeName.toLowerCase();
    if (expandedHomes.contains(key)) {
      expandedHomes.remove(key);
    } else {
      expandedHomes.add(key);
    }
    rebuildList();
  }

  private void handleSetDefault(String homeName) {
    if (homeName == null) return;
    UUID uuid = playerRef.getUuid();
    homeManager.setDefaultHome(uuid, homeName);
    rebuildList();
  }

  private void handleTeleport(@NotNull Ref<EntityStore> ref, String homeName) {
    if (homeName == null) return;

    UUID uuid = playerRef.getUuid();
    Home home = homeManager.getHome(uuid, homeName);
    if (home == null) {
      rebuildList();
      return;
    }

    // Zone flag check (home destination)
    if (FactionTerritoryChecker.checkZoneFlag(home.world(), home.x(), home.z(),
        HyperFactionsIntegration.FLAG_HOMES) != FactionTerritoryChecker.Result.ALLOWED) {
      rebuildList();
      return;
    }

    if (warmupManager.isOnCooldown(uuid, "homes", "home")) {
      rebuildList();
      return;
    }

    // Save back location before teleport
    saveBackLocation(uuid);

    Location dest = Location.fromHome(home);
    warmupManager.startWarmup(uuid, "homes", "home", () -> {
      World targetWorld = Universe.get().getWorld(UUID.fromString(dest.worldUuid()));
      if (targetWorld == null) return;
      targetWorld.execute(() -> {
        if (!ref.isValid()) return;
        Store<EntityStore> s = ref.getStore();
        Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
        Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
        Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
        s.addComponent(ref, Teleport.getComponentType(), teleport);
      });
    });

    // Close GUI after starting teleport
    player.getPageManager().setPage(ref, lastStore, Page.None);
  }

  private void handleTeleportShared(@NotNull Ref<EntityStore> ref, String targetData) {
    if (targetData == null) return;

    // targetData format: "ownerUuid:homeName"
    int sep = targetData.indexOf(':');
    if (sep < 0) return;

    try {
      UUID ownerUuid = UUID.fromString(targetData.substring(0, sep));
      String homeName = targetData.substring(sep + 1);

      // Verify still shared
      if (!homeManager.isSharedWith(ownerUuid, homeName, playerRef.getUuid())) {
        rebuildList();
        return;
      }

      Home home = homeManager.getHome(ownerUuid, homeName);
      if (home == null) {
        rebuildList();
        return;
      }

      // Zone flag check (shared home destination)
      if (FactionTerritoryChecker.checkZoneFlag(home.world(), home.x(), home.z(),
          HyperFactionsIntegration.FLAG_HOMES) != FactionTerritoryChecker.Result.ALLOWED) {
        rebuildList();
        return;
      }

      UUID uuid = playerRef.getUuid();
      if (warmupManager.isOnCooldown(uuid, "homes", "home")) {
        rebuildList();
        return;
      }

      // Save back location before teleport
      saveBackLocation(uuid);

      Location dest = Location.fromHome(home);
      warmupManager.startWarmup(uuid, "homes", "home", () -> {
        World targetWorld = Universe.get().getWorld(UUID.fromString(dest.worldUuid()));
        if (targetWorld == null) return;
        targetWorld.execute(() -> {
          if (!ref.isValid()) return;
          Store<EntityStore> s = ref.getStore();
          Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
          Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
          Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
          s.addComponent(ref, Teleport.getComponentType(), teleport);
        });
      });

      player.getPageManager().setPage(ref, lastStore, Page.None);
    } catch (IllegalArgumentException ignored) {
      rebuildList();
    }
  }

  private void handleFactionHome(@NotNull Ref<EntityStore> ref) {
    UUID uuid = playerRef.getUuid();

    // Check cooldown
    int cooldown = HyperFactionsIntegration.getFactionHomeCooldownRemaining(uuid);
    if (cooldown > 0) {
      rebuildList();
      return;
    }

    // Get faction home coords
    String world = HyperFactionsIntegration.getFactionHomeWorld(uuid);
    double[] coords = HyperFactionsIntegration.getFactionHomeCoords(uuid);
    if (world == null || coords == null) {
      rebuildList();
      return;
    }

    // Zone flag check (faction home destination)
    if (FactionTerritoryChecker.checkZoneFlag(world, coords[0], coords[2],
        HyperFactionsIntegration.FLAG_HOMES) != FactionTerritoryChecker.Result.ALLOWED) {
      rebuildList();
      return;
    }

    // Resolve world name to get UUID for teleport
    World resolvedWorld = Universe.get().getWorld(world);
    if (resolvedWorld == null) {
      rebuildList();
      return;
    }
    String worldUuid = resolvedWorld.getWorldConfig().getUuid().toString();

    // Save back location before teleport
    saveBackLocation(uuid);

    // Teleport using warmup manager (uses HyperEssentials warmup, not HyperFactions)
    Location dest = new Location(world, worldUuid, coords[0], coords[1], coords[2], (float) coords[3], (float) coords[4]);
    warmupManager.startWarmup(uuid, "factionhome", "factionhome", () -> {
      World targetWorld = Universe.get().getWorld(UUID.fromString(dest.worldUuid()));
      if (targetWorld == null) return;
      targetWorld.execute(() -> {
        if (!ref.isValid()) return;
        Store<EntityStore> s = ref.getStore();
        Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
        Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
        Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
        s.addComponent(ref, Teleport.getComponentType(), teleport);
      });
    });

    // Close GUI
    player.getPageManager().setPage(ref, lastStore, Page.None);
  }

  private void handleDelete(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                            String homeName) {
    if (homeName == null) return;

    UUID uuid = playerRef.getUuid();
    boolean deleted = homeManager.deleteHome(uuid, homeName);
    if (deleted) {
      rebuildList();
    }
  }

  private void handleShare(String homeName) {
    if (homeName == null) return;

    // Verify home exists
    UUID uuid = playerRef.getUuid();
    Home home = homeManager.getHome(uuid, homeName);
    if (home == null) return;

    // Unregister from tracker to stop periodic refresh while on sub-page
    guiManager.getPageTracker().unregister(playerRef.getUuid());

    // Open share sub-page
    HomeSharePage sharePage = new HomeSharePage(
        player, playerRef, homeManager, homeName,
        () -> guiManager.openPlayerPage("homes", player, lastRef, lastStore, playerRef)
    );
    player.getPageManager().openCustomPage(lastRef, lastStore, sharePage);
  }

  private void saveBackLocation(@NotNull UUID uuid) {
    if (!HyperEssentialsAPI.isAvailable()) return;
    TeleportModule tm = HyperEssentialsAPI.getInstance().getTeleportModule();
    if (tm == null || !tm.isEnabled()) return;
    BackManager bm = tm.getBackManager();
    if (bm == null || lastRef == null || lastStore == null) return;
    try {
      TransformComponent transform = lastStore.getComponent(lastRef, TransformComponent.getComponentType());
      if (transform != null) {
        Vector3d pos = transform.getPosition();
        World world = lastStore.getExternalData().getWorld();
        Location currentLoc = new Location(world.getName(),
            world.getWorldConfig().getUuid().toString(),
            pos.getX(), pos.getY(), pos.getZ(), 0, 0);
        bm.onTeleport(uuid, currentLoc, "home");
      }
    } catch (Exception ignored) {}
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

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildHomeList(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
