package com.hyperessentials.platform;

import com.hyperessentials.BuildInfo;
import com.hyperessentials.HyperEssentials;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.command.AdminCommand;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.SpawnsConfig;
import com.hyperessentials.config.modules.TeleportConfig;
import com.hyperessentials.config.modules.WarpsConfig;
import com.hyperessentials.listener.DeathListener;
import com.hyperessentials.module.rtp.RtpModule;
import com.hyperessentials.module.rtp.command.RtpCommand;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
    private DeathListener deathListener;
    private final Map<UUID, PlayerRef> trackedPlayers = new ConcurrentHashMap<>();

    public HyperEssentialsPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;

        // Initialize core
        hyperEssentials = new HyperEssentials(getDataDirectory(), java.util.logging.Logger.getLogger("HyperEssentials"));

        // Set API instance
        HyperEssentialsAPI.setInstance(hyperEssentials);

        getLogger().at(Level.INFO).log("HyperEssentials v%s loading...", BuildInfo.VERSION);
    }

    @Override
    protected void start() {
        // Enable core (loads config, integrations, modules)
        hyperEssentials.enable();

        // Initialize death listener
        deathListener = new DeathListener(hyperEssentials);

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

    private void registerCommands() {
        List<String> registered = new ArrayList<>();

        try {
            // Admin
            getCommandRegistry().registerCommand(new AdminCommand());
            registered.add("/hessentials");

            // Warps
            WarpsModule warps = hyperEssentials.getWarpsModule();
            if (warps != null && warps.isEnabled() && warps.getWarpManager() != null) {
                WarpManager wm = warps.getWarpManager();
                WarpsConfig warpsConfig = ConfigManager.get().warps();
                getCommandRegistry().registerCommand(new WarpCommand(wm, hyperEssentials.getWarmupManager()));
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
                getCommandRegistry().registerCommand(new SpawnCommand(sm, spawnsConfig, hyperEssentials.getWarmupManager()));
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
            TeleportModule teleport = hyperEssentials.getTeleportModule();
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
            }

            // RTP
            RtpModule rtp = hyperEssentials.getRtpModule();
            if (rtp != null && rtp.isEnabled() && rtp.getRtpManager() != null) {
                getCommandRegistry().registerCommand(new RtpCommand(rtp.getRtpManager(), hyperEssentials.getWarmupManager()));
                registered.add("/rtp");
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

        // Load teleport data
        TeleportModule tm = hyperEssentials.getTeleportModule();
        if (tm != null && tm.isEnabled() && tm.getTpaManager() != null) {
            tm.getTpaManager().loadPlayer(playerRef.getUuid(), playerRef.getUsername());
        }

        Logger.debug("Player connected: %s", playerRef.getUsername());
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        PlayerRef playerRef = event.getPlayerRef();
        trackedPlayers.remove(playerRef.getUuid());

        // Cancel any active warmups
        hyperEssentials.getWarmupManager().cancelWarmup(playerRef.getUuid());

        // Unregister from page tracker
        hyperEssentials.getGuiManager().getPageTracker().unregister(playerRef.getUuid());

        // Unload teleport data
        TeleportModule tm = hyperEssentials.getTeleportModule();
        if (tm != null && tm.isEnabled() && tm.getTpaManager() != null) {
            tm.getTpaManager().unloadPlayer(playerRef.getUuid());
        }

        Logger.debug("Player disconnected: %s", playerRef.getUsername());
    }

    @Nullable
    public PlayerRef getTrackedPlayer(UUID uuid) {
        return trackedPlayers.get(uuid);
    }

    /**
     * Finds an online player by username (case-insensitive).
     */
    @Nullable
    public PlayerRef findPlayerByUsername(String username) {
        if (username == null) {
            return null;
        }
        for (PlayerRef ref : trackedPlayers.values()) {
            if (ref.getUsername().equalsIgnoreCase(username)) {
                return ref;
            }
        }
        return null;
    }

    public DeathListener getDeathListener() {
        return deathListener;
    }

    public HyperEssentials getHyperEssentials() {
        return hyperEssentials;
    }
}
