package com.hyperessentials.config.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.hyperessentials.config.ModuleConfig;

public class RtpConfig extends ModuleConfig {
    private int centerX = 0;
    private int centerZ = 0;
    private int minRadius = 100;
    private int maxRadius = 5000;
    private int maxAttempts = 10;
    private List<String> blacklistedWorlds = new ArrayList<>();

    public RtpConfig(@NotNull Path filePath) { super(filePath); }
    @Override @NotNull public String getModuleName() { return "rtp"; }
    @Override protected boolean getDefaultEnabled() { return false; }
    @Override protected void createDefaults() {}

    @Override protected void loadModuleSettings(@NotNull JsonObject root) {
        centerX = getInt(root, "centerX", centerX);
        centerZ = getInt(root, "centerZ", centerZ);
        minRadius = getInt(root, "minRadius", minRadius);
        maxRadius = getInt(root, "maxRadius", maxRadius);
        maxAttempts = getInt(root, "maxAttempts", maxAttempts);

        blacklistedWorlds = new ArrayList<>();
        if (root.has("blacklistedWorlds") && root.get("blacklistedWorlds").isJsonArray()) {
            JsonArray arr = root.getAsJsonArray("blacklistedWorlds");
            for (int i = 0; i < arr.size(); i++) {
                blacklistedWorlds.add(arr.get(i).getAsString().toLowerCase());
            }
        }
    }

    @Override protected void writeModuleSettings(@NotNull JsonObject root) {
        root.addProperty("centerX", centerX);
        root.addProperty("centerZ", centerZ);
        root.addProperty("minRadius", minRadius);
        root.addProperty("maxRadius", maxRadius);
        root.addProperty("maxAttempts", maxAttempts);

        JsonArray arr = new JsonArray();
        for (String world : blacklistedWorlds) {
            arr.add(world);
        }
        root.add("blacklistedWorlds", arr);
    }

    public int getCenterX() { return centerX; }
    public int getCenterZ() { return centerZ; }
    public int getMinRadius() { return minRadius; }
    public int getMaxRadius() { return maxRadius; }
    public int getMaxAttempts() { return maxAttempts; }
    public List<String> getBlacklistedWorlds() { return blacklistedWorlds; }
}
