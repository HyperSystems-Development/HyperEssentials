package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import com.hyperessentials.config.ModuleConfig;

public class TeleportConfig extends ModuleConfig {
    private int tpaTimeout = 60;
    private int tpaCooldown = 30;
    private int maxPendingTpa = 5;
    private int backHistorySize = 5;
    private boolean saveBackOnDeath = true;
    private boolean saveBackOnTeleport = true;

    public TeleportConfig(@NotNull Path filePath) { super(filePath); }
    @Override @NotNull public String getModuleName() { return "teleport"; }
    @Override protected void createDefaults() {}

    @Override protected void loadModuleSettings(@NotNull JsonObject root) {
        tpaTimeout = getInt(root, "tpaTimeout", tpaTimeout);
        tpaCooldown = getInt(root, "tpaCooldown", tpaCooldown);
        maxPendingTpa = getInt(root, "maxPendingTpa", maxPendingTpa);
        backHistorySize = getInt(root, "backHistorySize", backHistorySize);
        saveBackOnDeath = getBool(root, "saveBackOnDeath", saveBackOnDeath);
        saveBackOnTeleport = getBool(root, "saveBackOnTeleport", saveBackOnTeleport);
    }

    @Override protected void writeModuleSettings(@NotNull JsonObject root) {
        root.addProperty("tpaTimeout", tpaTimeout);
        root.addProperty("tpaCooldown", tpaCooldown);
        root.addProperty("maxPendingTpa", maxPendingTpa);
        root.addProperty("backHistorySize", backHistorySize);
        root.addProperty("saveBackOnDeath", saveBackOnDeath);
        root.addProperty("saveBackOnTeleport", saveBackOnTeleport);
    }

    public int getTpaTimeout() { return tpaTimeout; }
    public int getTpaCooldown() { return tpaCooldown; }
    public int getMaxPendingTpa() { return maxPendingTpa; }
    public int getBackHistorySize() { return backHistorySize; }
    public boolean isSaveBackOnDeath() { return saveBackOnDeath; }
    public boolean isSaveBackOnTeleport() { return saveBackOnTeleport; }
}
