package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import com.hyperessentials.config.ModuleConfig;

public class UtilityConfig extends ModuleConfig {

  private boolean clearChatEnabled = true;
  private boolean clearInventoryEnabled = true;
  private boolean repairEnabled = true;
  private boolean nearEnabled = true;
  private boolean healEnabled = true;
  private boolean flyEnabled = true;
  private boolean godEnabled = true;
  private int defaultNearRadius = 200;
  private int maxNearRadius = 1000;
  private int clearChatLines = 100;

  public UtilityConfig(@NotNull Path filePath) { super(filePath); }

  @Override @NotNull public String getModuleName() { return "utility"; }
  @Override protected boolean getDefaultEnabled() { return false; }

  @Override
  protected void createDefaults() {
    clearChatEnabled = true;
    clearInventoryEnabled = true;
    repairEnabled = true;
    nearEnabled = true;
    healEnabled = true;
    flyEnabled = true;
    godEnabled = true;
    defaultNearRadius = 200;
    maxNearRadius = 1000;
    clearChatLines = 100;
  }

  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    clearChatEnabled = getBool(root, "clearChatEnabled", true);
    clearInventoryEnabled = getBool(root, "clearInventoryEnabled", true);
    repairEnabled = getBool(root, "repairEnabled", true);
    nearEnabled = getBool(root, "nearEnabled", true);
    healEnabled = getBool(root, "healEnabled", true);
    flyEnabled = getBool(root, "flyEnabled", true);
    godEnabled = getBool(root, "godEnabled", true);
    defaultNearRadius = getInt(root, "defaultNearRadius", 200);
    maxNearRadius = getInt(root, "maxNearRadius", 1000);
    clearChatLines = getInt(root, "clearChatLines", 100);
  }

  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("clearChatEnabled", clearChatEnabled);
    root.addProperty("clearInventoryEnabled", clearInventoryEnabled);
    root.addProperty("repairEnabled", repairEnabled);
    root.addProperty("nearEnabled", nearEnabled);
    root.addProperty("healEnabled", healEnabled);
    root.addProperty("flyEnabled", flyEnabled);
    root.addProperty("godEnabled", godEnabled);
    root.addProperty("defaultNearRadius", defaultNearRadius);
    root.addProperty("maxNearRadius", maxNearRadius);
    root.addProperty("clearChatLines", clearChatLines);
  }

  public boolean isClearChatEnabled() { return clearChatEnabled; }
  public boolean isClearInventoryEnabled() { return clearInventoryEnabled; }
  public boolean isRepairEnabled() { return repairEnabled; }
  public boolean isNearEnabled() { return nearEnabled; }
  public boolean isHealEnabled() { return healEnabled; }
  public boolean isFlyEnabled() { return flyEnabled; }
  public boolean isGodEnabled() { return godEnabled; }
  public int getDefaultNearRadius() { return defaultNearRadius; }
  public int getMaxNearRadius() { return maxNearRadius; }
  public int getClearChatLines() { return clearChatLines; }
}
