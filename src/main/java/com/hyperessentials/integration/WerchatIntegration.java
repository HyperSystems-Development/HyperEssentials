package com.hyperessentials.integration;

import com.hyperessentials.util.Logger;

/**
 * Stub for future Werchat chat integration.
 */
public final class WerchatIntegration {

    private static boolean available = false;

    private WerchatIntegration() {}

    public static void init() {
        // TODO: Detect and initialize Werchat integration
        Logger.debug("[Integration] Werchat integration not yet implemented");
    }

    public static boolean isAvailable() { return available; }
}
