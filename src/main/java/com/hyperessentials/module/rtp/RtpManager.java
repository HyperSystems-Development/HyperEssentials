package com.hyperessentials.module.rtp;

import com.hyperessentials.config.modules.RtpConfig;
import com.hyperessentials.data.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;

/**
 * Manages random teleport location generation.
 */
public class RtpManager {

    private final RtpConfig config;
    private final Random random = new Random();

    public RtpManager(@NotNull RtpConfig config) {
        this.config = config;
    }

    /**
     * Generates a random location within the configured ring.
     * Y coordinate is set to 64 as a placeholder; actual safe Y resolution
     * happens at the platform level when world access is available.
     *
     * @param worldName the world to generate a location in
     * @return a random location, or null if the world is blacklisted
     */
    @Nullable
    public Location findRandomLocation(@NotNull String worldName) {
        if (isWorldBlacklisted(worldName)) {
            return null;
        }

        int centerX = config.getCenterX();
        int centerZ = config.getCenterZ();
        int minR = config.getMinRadius();
        int maxR = config.getMaxRadius();

        // Random point in ring: angle + radius
        double angle = random.nextDouble() * 2 * Math.PI;
        double radius = minR + random.nextDouble() * (maxR - minR);
        double x = centerX + radius * Math.cos(angle);
        double z = centerZ + radius * Math.sin(angle);

        return new Location(worldName, x, 64, z, 0, 0);
    }

    public boolean isWorldBlacklisted(@NotNull String worldName) {
        return config.getBlacklistedWorlds().contains(worldName.toLowerCase());
    }

    public int getMaxAttempts() {
        return config.getMaxAttempts();
    }
}
