package com.hyperessentials.listener;

import com.hyperessentials.HyperEssentials;
import com.hyperessentials.data.Location;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Listener for player death events.
 * Saves death location to back history.
 */
public class DeathListener {

    private final HyperEssentials hyperEssentials;

    public DeathListener(@NotNull HyperEssentials hyperEssentials) {
        this.hyperEssentials = hyperEssentials;
    }

    /**
     * Called when a player dies.
     * Saves their death location if configured.
     */
    public void onPlayerDeath(@NotNull UUID playerUuid, @NotNull Location deathLocation) {
        TeleportModule tm = hyperEssentials.getTeleportModule();
        if (tm == null || !tm.isEnabled()) {
            return;
        }

        BackManager backManager = tm.getBackManager();
        if (backManager != null) {
            backManager.onDeath(playerUuid, deathLocation);
            Logger.debug("Saved death location for %s", playerUuid);
        }
    }

    /**
     * Called when a player respawns.
     * Can teleport to spawn on respawn if configured.
     */
    public void onPlayerRespawn(@NotNull UUID playerUuid) {
        // Teleport to spawn on respawn would require the player's store/ref
        // from the respawn event, handled at the platform level
        Logger.debug("Player respawning: %s", playerUuid);
    }
}
