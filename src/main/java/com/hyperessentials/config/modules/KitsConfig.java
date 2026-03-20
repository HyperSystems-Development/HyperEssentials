package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import com.hyperessentials.config.ModuleConfig;

public class KitsConfig extends ModuleConfig {

  private int defaultCooldownSeconds = 300;
  private boolean oneTimeDefault = false;

  public KitsConfig(@NotNull Path filePath) { super(filePath); }

  @Override @NotNull public String getModuleName() { return "kits"; }
  @Override protected boolean getDefaultEnabled() { return false; }

  @Override
  protected void createDefaults() {
    defaultCooldownSeconds = 300;
    oneTimeDefault = false;
  }

  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    defaultCooldownSeconds = getInt(root, "defaultCooldownSeconds", 300);
    oneTimeDefault = getBool(root, "oneTimeDefault", false);
  }

  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("defaultCooldownSeconds", defaultCooldownSeconds);
    root.addProperty("oneTimeDefault", oneTimeDefault);
  }

  public int getDefaultCooldownSeconds() { return defaultCooldownSeconds; }
  public boolean isOneTimeDefault() { return oneTimeDefault; }

  // Setters (for admin config editor)
  public void setDefaultCooldownSeconds(int value) { this.defaultCooldownSeconds = value; }
  public void setOneTimeDefault(boolean value) { this.oneTimeDefault = value; }
}
