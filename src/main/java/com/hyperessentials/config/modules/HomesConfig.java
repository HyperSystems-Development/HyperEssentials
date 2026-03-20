package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

import com.hyperessentials.config.ModuleConfig;

public class HomesConfig extends ModuleConfig {

  private int defaultHomeLimit = 3;

  // Factions integration
  private boolean factionsEnabled = true;
  private boolean allowInOwnTerritory = true;
  private boolean allowInAllyTerritory = true;
  private boolean allowInNeutralTerritory = false;
  private boolean allowInEnemyTerritory = false;
  private boolean allowInWilderness = true;

  // Bed sync
  private boolean bedSyncEnabled = true;
  private String bedHomeName = "bed";

  // Sharing (deferred - config only)
  private boolean shareEnabled = true;
  private int maxSharesPerHome = 10;

  public HomesConfig(@NotNull Path filePath) { super(filePath); }

  @Override @NotNull public String getModuleName() { return "homes"; }

  @Override protected void createDefaults() {}

  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    defaultHomeLimit = getInt(root, "defaultHomeLimit", defaultHomeLimit);

    if (hasSection(root, "factions")) {
      JsonObject factions = root.getAsJsonObject("factions");
      factionsEnabled = getBool(factions, "enabled", factionsEnabled);
      allowInOwnTerritory = getBool(factions, "allowInOwnTerritory", allowInOwnTerritory);
      allowInAllyTerritory = getBool(factions, "allowInAllyTerritory", allowInAllyTerritory);
      allowInNeutralTerritory = getBool(factions, "allowInNeutralTerritory", allowInNeutralTerritory);
      allowInEnemyTerritory = getBool(factions, "allowInEnemyTerritory", allowInEnemyTerritory);
      allowInWilderness = getBool(factions, "allowInWilderness", allowInWilderness);
    }

    bedSyncEnabled = getBool(root, "bedSyncEnabled", bedSyncEnabled);
    bedHomeName = getString(root, "bedHomeName", bedHomeName);
    shareEnabled = getBool(root, "shareEnabled", shareEnabled);
    maxSharesPerHome = getInt(root, "maxSharesPerHome", maxSharesPerHome);
  }

  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("defaultHomeLimit", defaultHomeLimit);

    JsonObject factions = new JsonObject();
    factions.addProperty("enabled", factionsEnabled);
    factions.addProperty("allowInOwnTerritory", allowInOwnTerritory);
    factions.addProperty("allowInAllyTerritory", allowInAllyTerritory);
    factions.addProperty("allowInNeutralTerritory", allowInNeutralTerritory);
    factions.addProperty("allowInEnemyTerritory", allowInEnemyTerritory);
    factions.addProperty("allowInWilderness", allowInWilderness);
    root.add("factions", factions);

    root.addProperty("bedSyncEnabled", bedSyncEnabled);
    root.addProperty("bedHomeName", bedHomeName);
    root.addProperty("shareEnabled", shareEnabled);
    root.addProperty("maxSharesPerHome", maxSharesPerHome);
  }

  // Getters
  public int getDefaultHomeLimit() { return defaultHomeLimit; }
  public boolean isFactionsEnabled() { return factionsEnabled; }
  public boolean isAllowInOwnTerritory() { return allowInOwnTerritory; }
  public boolean isAllowInAllyTerritory() { return allowInAllyTerritory; }
  public boolean isAllowInNeutralTerritory() { return allowInNeutralTerritory; }
  public boolean isAllowInEnemyTerritory() { return allowInEnemyTerritory; }
  public boolean isAllowInWilderness() { return allowInWilderness; }
  public boolean isBedSyncEnabled() { return bedSyncEnabled; }
  public String getBedHomeName() { return bedHomeName; }
  public boolean isShareEnabled() { return shareEnabled; }
  public int getMaxSharesPerHome() { return maxSharesPerHome; }

  // Setters (for admin config editor)
  public void setDefaultHomeLimit(int value) { this.defaultHomeLimit = value; }
  public void setFactionsEnabled(boolean value) { this.factionsEnabled = value; }
  public void setAllowInOwnTerritory(boolean value) { this.allowInOwnTerritory = value; }
  public void setAllowInAllyTerritory(boolean value) { this.allowInAllyTerritory = value; }
  public void setAllowInNeutralTerritory(boolean value) { this.allowInNeutralTerritory = value; }
  public void setAllowInEnemyTerritory(boolean value) { this.allowInEnemyTerritory = value; }
  public void setAllowInWilderness(boolean value) { this.allowInWilderness = value; }
  public void setBedSyncEnabled(boolean value) { this.bedSyncEnabled = value; }
  public void setBedHomeName(String value) { this.bedHomeName = value; }
  public void setShareEnabled(boolean value) { this.shareEnabled = value; }
  public void setMaxSharesPerHome(int value) { this.maxSharesPerHome = value; }
}
