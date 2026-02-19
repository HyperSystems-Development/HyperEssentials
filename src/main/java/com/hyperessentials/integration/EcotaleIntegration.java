package com.hyperessentials.integration;

import com.hyperessentials.util.Logger;

/**
 * Stub for future Ecotale economy integration.
 */
public final class EcotaleIntegration {

    private static boolean available = false;

    private EcotaleIntegration() {}

    public static void init() {
        // TODO: Detect and initialize Ecotale integration
        Logger.debug("[Integration] Ecotale integration not yet implemented");
    }

    public static boolean isAvailable() { return available; }
}
