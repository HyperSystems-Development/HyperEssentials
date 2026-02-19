package com.hyperessentials.api;

import com.hyperessentials.HyperEssentials;
import org.jetbrains.annotations.Nullable;

/**
 * Public API for HyperEssentials.
 * Other plugins can use this to interact with HyperEssentials modules.
 */
public final class HyperEssentialsAPI {

    private static HyperEssentials instance;

    private HyperEssentialsAPI() {}

    public static void setInstance(@Nullable HyperEssentials instance) {
        HyperEssentialsAPI.instance = instance;
    }

    /**
     * Gets the HyperEssentials instance.
     *
     * @return the instance, or null if not initialized
     */
    @Nullable
    public static HyperEssentials getInstance() {
        return instance;
    }

    /**
     * Checks if HyperEssentials is available and initialized.
     *
     * @return true if available
     */
    public static boolean isAvailable() {
        return instance != null;
    }
}
