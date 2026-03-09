package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import com.hyperessentials.config.ModuleConfig;

public class WarmupConfig extends ModuleConfig {
  private boolean cancelOnMove = true;
  private boolean cancelOnDamage = true;
  private boolean safeTeleport = true;
  private int safeRadius = 3;
  private final Map<String, ModuleWarmupSettings> moduleSettings = new HashMap<>();

  public record ModuleWarmupSettings(int warmup, int cooldown) {}

  public WarmupConfig(@NotNull Path filePath) { super(filePath); }
  @Override @NotNull public String getModuleName() { return "warmup"; }
  @Override protected void createDefaults() {
    moduleSettings.put("homes", new ModuleWarmupSettings(3, 5));
    moduleSettings.put("warps", new ModuleWarmupSettings(3, 5));
    moduleSettings.put("spawns", new ModuleWarmupSettings(3, 5));
    moduleSettings.put("teleport", new ModuleWarmupSettings(3, 5));
    moduleSettings.put("rtp", new ModuleWarmupSettings(5, 30));
    moduleSettings.put("factionhome", new ModuleWarmupSettings(5, 10));
  }

  @Override protected void loadModuleSettings(@NotNull JsonObject root) {
    cancelOnMove = getBool(root, "cancelOnMove", cancelOnMove);
    cancelOnDamage = getBool(root, "cancelOnDamage", cancelOnDamage);
    safeTeleport = getBool(root, "safeTeleport", safeTeleport);
    safeRadius = getInt(root, "safeRadius", safeRadius);

    moduleSettings.clear();
    if (hasSection(root, "modules")) {
      JsonObject modules = getSection(root, "modules");
      for (String key : modules.keySet()) {
        if (modules.get(key).isJsonObject()) {
          JsonObject mod = modules.getAsJsonObject(key);
          int warmup = mod.has("warmup") ? mod.get("warmup").getAsInt() : 3;
          int cooldown = mod.has("cooldown") ? mod.get("cooldown").getAsInt() : 5;
          moduleSettings.put(key, new ModuleWarmupSettings(warmup, cooldown));
        }
      }
    }
    if (moduleSettings.isEmpty()) {
      createDefaults();
      needsSave = true;
    }
  }

  @Override protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("cancelOnMove", cancelOnMove);
    root.addProperty("cancelOnDamage", cancelOnDamage);
    root.addProperty("safeTeleport", safeTeleport);
    root.addProperty("safeRadius", safeRadius);

    JsonObject modules = new JsonObject();
    for (Map.Entry<String, ModuleWarmupSettings> entry : moduleSettings.entrySet()) {
      JsonObject mod = new JsonObject();
      mod.addProperty("warmup", entry.getValue().warmup());
      mod.addProperty("cooldown", entry.getValue().cooldown());
      modules.add(entry.getKey(), mod);
    }
    root.add("modules", modules);
  }

  public boolean isCancelOnMove() { return cancelOnMove; }
  public boolean isCancelOnDamage() { return cancelOnDamage; }
  public boolean isSafeTeleport() { return safeTeleport; }
  public int getSafeRadius() { return safeRadius; }
  public int getWarmup(String moduleName) {
    ModuleWarmupSettings s = moduleSettings.get(moduleName);
    return s != null ? s.warmup() : 3;
  }
  public int getCooldown(String moduleName) {
    ModuleWarmupSettings s = moduleSettings.get(moduleName);
    return s != null ? s.cooldown() : 5;
  }
}
