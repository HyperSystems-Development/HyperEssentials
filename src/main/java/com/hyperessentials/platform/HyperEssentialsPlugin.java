package com.hyperessentials.platform;

import com.hyperessentials.BuildInfo;
import com.hyperessentials.HyperEssentials;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.command.AdminCommand;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.integration.PermissionManager;
import com.hyperessentials.config.modules.SpawnsConfig;
import com.hyperessentials.config.modules.TeleportConfig;
import com.hyperessentials.config.modules.WarpsConfig;
import com.hyperessentials.ecs.PlayerDeathTrackingSystem;
import com.hyperessentials.module.homes.HomeManager;
import com.hyperessentials.module.homes.HomesModule;
import com.hyperessentials.module.homes.command.DelHomeCommand;
import com.hyperessentials.module.homes.command.HomeCommand;
import com.hyperessentials.module.homes.command.HomesCommand;
import com.hyperessentials.module.homes.command.SetHomeCommand;
import com.hyperessentials.module.teleport.RtpManager;
import com.hyperessentials.module.teleport.command.RtpCommand;
import com.hyperessentials.module.spawns.SpawnManager;
import com.hyperessentials.module.spawns.SpawnsModule;
import com.hyperessentials.module.spawns.command.*;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.module.teleport.TpaManager;
import com.hyperessentials.module.teleport.command.*;
import com.hyperessentials.module.warps.WarpManager;
import com.hyperessentials.module.warps.WarpsModule;
import com.hyperessentials.module.warps.command.*;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Main Hytale plugin class for HyperEssentials.
 */
public class HyperEssentialsPlugin extends JavaPlugin {

  private static HyperEssentialsPlugin instance;

  public static HyperEssentialsPlugin getInstance() {
    return instance;
  }

  private HyperEssentials hyperEssentials;
  private final Map<UUID, PlayerRef> trackedPlayers = new ConcurrentHashMap<>();

  public HyperEssentialsPlugin(JavaPluginInit init) {
    super(init);
  }

  @Override
  protected void setup() {
    instance = this;

    // Initialize core
    hyperEssentials = new HyperEssentials(getDataDirectory(), getLogger());

    // Set API instance
    HyperEssentialsAPI.setInstance(hyperEssentials);

    getLogger().at(Level.INFO).log("HyperEssentials v%s loading...", BuildInfo.VERSION);
  }

  @Override
  protected void start() {
    // Enable core (loads config, integrations, modules)
    hyperEssentials.enable();

    // Register ECS systems
    registerEcsSystems();

    // Register commands
    registerCommands();

    // Register event listeners
    registerEventListeners();

    getLogger().at(Level.INFO).log("HyperEssentials v%s enabled!", BuildInfo.VERSION);
  }

  @Override
  protected void shutdown() {
    // Clear instances
    instance = null;
    HyperEssentialsAPI.setInstance(null);

    // Disable core
    if (hyperEssentials != null) {
      hyperEssentials.disable();
    }

    // Clear tracked players
    trackedPlayers.clear();

    getLogger().at(Level.INFO).log("HyperEssentials disabled");
  }

  private void registerEcsSystems() {
    TeleportModule teleport = hyperEssentials.getTeleportModule();
    if (teleport != null && teleport.isEnabled()) {
      getEntityStoreRegistry().registerSystem(new PlayerDeathTrackingSystem(hyperEssentials));
      getLogger().at(Level.INFO).log("Registered PlayerDeathTrackingSystem");
    }
  }

  private void registerCommands() {
    List<String> registered = new ArrayList<>();

    // Get BackManager early for passing to teleport commands
    TeleportModule teleport = hyperEssentials.getTeleportModule();
    BackManager backManager = (teleport != null && teleport.isEnabled()) ? teleport.getBackManager() : null;

    try {
      // Admin
      getCommandRegistry().registerCommand(new AdminCommand());
      registered.add("/hessentials");

      // Homes
      HomesModule homesModule = hyperEssentials.getHomesModule();
      if (homesModule != null && homesModule.isEnabled() && homesModule.getHomeManager() != null) {
        HomeManager hm = homesModule.getHomeManager();
        getCommandRegistry().registerCommand(new SetHomeCommand(hm));
        getCommandRegistry().registerCommand(new HomeCommand(hm, hyperEssentials.getWarmupManager(), backManager));
        getCommandRegistry().registerCommand(new DelHomeCommand(hm));
        getCommandRegistry().registerCommand(new HomesCommand(hm));
        registered.add("/sethome");
        registered.add("/home");
        registered.add("/delhome");
        registered.add("/homes");
      }

      // Warps
      WarpsModule warps = hyperEssentials.getWarpsModule();
      if (warps != null && warps.isEnabled() && warps.getWarpManager() != null) {
        WarpManager wm = warps.getWarpManager();
        WarpsConfig warpsConfig = ConfigManager.get().warps();
        getCommandRegistry().registerCommand(new WarpCommand(wm, hyperEssentials.getWarmupManager(), backManager));
        getCommandRegistry().registerCommand(new WarpsCommand(wm));
        getCommandRegistry().registerCommand(new SetWarpCommand(wm, warpsConfig));
        getCommandRegistry().registerCommand(new DelWarpCommand(wm));
        getCommandRegistry().registerCommand(new WarpInfoCommand(wm));
        registered.add("/warp");
        registered.add("/warps");
        registered.add("/setwarp");
        registered.add("/delwarp");
        registered.add("/warpinfo");
      }

      // Spawns
      SpawnsModule spawns = hyperEssentials.getSpawnsModule();
      if (spawns != null && spawns.isEnabled() && spawns.getSpawnManager() != null) {
        SpawnManager sm = spawns.getSpawnManager();
        SpawnsConfig spawnsConfig = ConfigManager.get().spawns();
        getCommandRegistry().registerCommand(new SpawnCommand(sm, spawnsConfig, hyperEssentials.getWarmupManager(), backManager));
        getCommandRegistry().registerCommand(new SpawnsCommand(sm));
        getCommandRegistry().registerCommand(new SetSpawnCommand(sm, spawnsConfig));
        getCommandRegistry().registerCommand(new DelSpawnCommand(sm));
        getCommandRegistry().registerCommand(new SpawnInfoCommand(sm));
        registered.add("/spawn");
        registered.add("/spawns");
        registered.add("/setspawn");
        registered.add("/delspawn");
        registered.add("/spawninfo");
      }

      // Teleport
      if (teleport != null && teleport.isEnabled() && teleport.getTpaManager() != null) {
        TpaManager tpa = teleport.getTpaManager();
        BackManager back = teleport.getBackManager();
        TeleportConfig teleportConfig = ConfigManager.get().teleport();
        getCommandRegistry().registerCommand(new TpaCommand(tpa, teleportConfig));
        getCommandRegistry().registerCommand(new TpaHereCommand(tpa, teleportConfig));
        getCommandRegistry().registerCommand(new TpAcceptCommand(tpa, back, hyperEssentials.getWarmupManager()));
        getCommandRegistry().registerCommand(new TpDenyCommand(tpa));
        getCommandRegistry().registerCommand(new TpCancelCommand(tpa));
        getCommandRegistry().registerCommand(new TpToggleCommand(tpa));
        getCommandRegistry().registerCommand(new BackCommand(back, hyperEssentials.getWarmupManager()));
        registered.add("/tpa");
        registered.add("/tpahere");
        registered.add("/tpaccept");
        registered.add("/tpdeny");
        registered.add("/tpcancel");
        registered.add("/tptoggle");
        registered.add("/back");

        // RTP (part of Teleport module)
        RtpManager rtpMgr = teleport.getRtpManager();
        if (rtpMgr != null) {
          getCommandRegistry().registerCommand(new RtpCommand(rtpMgr, hyperEssentials.getWarmupManager(), backManager));
          registered.add("/rtp");
        }
      }

      getLogger().at(Level.INFO).log("Registered commands: %s", String.join(", ", registered));
    } catch (Exception e) {
      getLogger().at(Level.SEVERE).withCause(e).log("Failed to register commands");
    }
  }

  private void registerEventListeners() {
    getEventRegistry().register(PlayerConnectEvent.class, this::onPlayerConnect);
    getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);
    getLogger().at(Level.INFO).log("Registered event listeners");
  }

  private void onPlayerConnect(PlayerConnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    trackedPlayers.put(playerRef.getUuid(), playerRef);

    // Notify connect handlers
    hyperEssentials.onPlayerConnect(playerRef.getUuid(), playerRef.getUsername());

    // Load home data
    HomesModule homesModule = hyperEssentials.getHomesModule();
    if (homesModule != null && homesModule.isEnabled() && homesModule.getHomeManager() != null) {
      homesModule.getHomeManager().loadPlayer(playerRef.getUuid(), playerRef.getUsername());
    }

    // Load teleport data
    TeleportModule tm = hyperEssentials.getTeleportModule();
    if (tm != null && tm.isEnabled() && tm.getTpaManager() != null) {
      tm.getTpaManager().loadPlayer(playerRef.getUuid(), playerRef.getUsername());
    }

    Logger.debug("Player connected: %s", playerRef.getUsername());
  }

  private void onPlayerDisconnect(PlayerDisconnectEvent event) {
    PlayerRef playerRef = event.getPlayerRef();
    UUID uuid = playerRef.getUuid();

    // Cancel any active warmups
    hyperEssentials.getWarmupManager().cancelWarmup(uuid);

    // Unregister from page tracker
    hyperEssentials.getGuiManager().getPageTracker().unregister(uuid);

    // Notify modules of disconnect for cleanup
    hyperEssentials.onPlayerDisconnect(uuid);

    // Unload home data
    HomesModule hmModule = hyperEssentials.getHomesModule();
    if (hmModule != null && hmModule.isEnabled() && hmModule.getHomeManager() != null) {
      hmModule.getHomeManager().unloadPlayer(uuid);
    }

    // Remove from tracked players last
    trackedPlayers.remove(uuid);

    // Unload teleport data
    TeleportModule tm = hyperEssentials.getTeleportModule();
    if (tm != null && tm.isEnabled() && tm.getTpaManager() != null) {
      tm.getTpaManager().unloadPlayer(playerRef.getUuid());
    }

    // Clear admin bypass state
    PermissionManager.get().clearAdminBypass(uuid);

    Logger.debug("Player disconnected: %s", playerRef.getUsername());
  }

  @Nullable
  public PlayerRef getTrackedPlayer(UUID uuid) {
    return trackedPlayers.get(uuid);
  }

  /**
   * Returns an unmodifiable view of all tracked (online) players.
   */
  @NotNull
  public Map<UUID, PlayerRef> getTrackedPlayers() {
    return Collections.unmodifiableMap(trackedPlayers);
  }

  /**
   * Finds an online player by name (case-insensitive).
   *
   * @param name the player name to search for
   * @return the PlayerRef if found online, null otherwise
   */
  @Nullable
  public PlayerRef findOnlinePlayer(@NotNull String name) {
    for (PlayerRef ref : trackedPlayers.values()) {
      if (ref.getUsername().equalsIgnoreCase(name)) {
        return ref;
      }
    }
    return null;
  }

  public HyperEssentials getHyperEssentials() {
    return hyperEssentials;
  }
}
