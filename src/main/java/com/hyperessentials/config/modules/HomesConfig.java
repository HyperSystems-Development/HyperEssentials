package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import com.hyperessentials.config.ModuleConfig;

public class HomesConfig extends ModuleConfig {

  private int defaultHomeLimit = 3;
  private boolean restrictInEnemyTerritory = false;
  private boolean bedSyncEnabled = true;
  private String bedHomeName = "bed";
  private boolean shareEnabled = true;
  private int maxSharesPerHome = 10;

  public HomesConfig(@NotNull Path filePath) { super(filePath); }

  @Override @NotNull public String getModuleName() { return "homes"; }

  @Override protected void createDefaults() {}

  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    defaultHomeLimit = getInt(root, "defaultHomeLimit", defaultHomeLimit);
    restrictInEnemyTerritory = getBool(root, "restrictInEnemyTerritory", restrictInEnemyTerritory);
    bedSyncEnabled = getBool(root, "bedSyncEnabled", bedSyncEnabled);
    bedHomeName = getString(root, "bedHomeName", bedHomeName);
    shareEnabled = getBool(root, "shareEnabled", shareEnabled);
    maxSharesPerHome = getInt(root, "maxSharesPerHome", maxSharesPerHome);
  }

  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("defaultHomeLimit", defaultHomeLimit);
    root.addProperty("restrictInEnemyTerritory", restrictInEnemyTerritory);
    root.addProperty("bedSyncEnabled", bedSyncEnabled);
    root.addProperty("bedHomeName", bedHomeName);
    root.addProperty("shareEnabled", shareEnabled);
    root.addProperty("maxSharesPerHome", maxSharesPerHome);
  }

  public int getDefaultHomeLimit() { return defaultHomeLimit; }
  public boolean isRestrictInEnemyTerritory() { return restrictInEnemyTerritory; }
  public boolean isBedSyncEnabled() { return bedSyncEnabled; }
  public String getBedHomeName() { return bedHomeName; }
  public boolean isShareEnabled() { return shareEnabled; }
  public int getMaxSharesPerHome() { return maxSharesPerHome; }
}
