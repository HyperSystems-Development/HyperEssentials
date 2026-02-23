package com.hyperessentials.platform;

import com.hyperessentials.BuildInfo;
import com.hyperessentials.HyperEssentials;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.command.AdminCommand;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
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
        hyperEssentials = new HyperEssentials(getDataDirectory(), java.util.logging.Logger.getLogger("HyperEssentials"));

        // Set API instance
        HyperEssentialsAPI.setInstance(hyperEssentials);

        getLogger().at(Level.INFO).log("HyperEssentials v%s loading...", BuildInfo.VERSION);
    }

    @Override
    protected void start() {
        // Enable core (loads config, integrations, modules)
        hyperEssentials.enable();

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
        try {
            getCommandRegistry().registerCommand(new AdminCommand());
            getLogger().at(Level.INFO).log("Registered commands: /hessentials");
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

        // Remove from tracked players last
        trackedPlayers.remove(uuid);

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
