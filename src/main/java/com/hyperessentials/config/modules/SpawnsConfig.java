package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import com.hyperessentials.config.ModuleConfig;

public class SpawnsConfig extends ModuleConfig {
  private String defaultSpawnName = "spawn";
  private boolean teleportOnJoin = false;
  private boolean teleportOnRespawn = true;
  private boolean perWorldSpawns = false;

  public SpawnsConfig(@NotNull Path filePath) { super(filePath); }
  @Override @NotNull public String getModuleName() { return "spawns"; }
  @Override protected void createDefaults() {}

  @Override protected void loadModuleSettings(@NotNull JsonObject root) {
    defaultSpawnName = getString(root, "defaultSpawnName", defaultSpawnName);
    teleportOnJoin = getBool(root, "teleportOnJoin", teleportOnJoin);
    teleportOnRespawn = getBool(root, "teleportOnRespawn", teleportOnRespawn);
    perWorldSpawns = getBool(root, "perWorldSpawns", perWorldSpawns);
  }

  @Override protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("defaultSpawnName", defaultSpawnName);
    root.addProperty("teleportOnJoin", teleportOnJoin);
    root.addProperty("teleportOnRespawn", teleportOnRespawn);
    root.addProperty("perWorldSpawns", perWorldSpawns);
  }

  public String getDefaultSpawnName() { return defaultSpawnName; }
  public boolean isTeleportOnJoin() { return teleportOnJoin; }
  public boolean isTeleportOnRespawn() { return teleportOnRespawn; }
  public boolean isPerWorldSpawns() { return perWorldSpawns; }

  // Setters (for admin config editor)
  public void setDefaultSpawnName(String value) { this.defaultSpawnName = value; }
  public void setTeleportOnJoin(boolean value) { this.teleportOnJoin = value; }
  public void setTeleportOnRespawn(boolean value) { this.teleportOnRespawn = value; }
  public void setPerWorldSpawns(boolean value) { this.perWorldSpawns = value; }
}
