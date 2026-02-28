package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import com.hyperessentials.config.ModuleConfig;

public class WarpsConfig extends ModuleConfig {
  private String defaultCategory = "general";

  public WarpsConfig(@NotNull Path filePath) { super(filePath); }
  @Override @NotNull public String getModuleName() { return "warps"; }
  @Override protected void createDefaults() {}

  @Override protected void loadModuleSettings(@NotNull JsonObject root) {
    defaultCategory = getString(root, "defaultCategory", defaultCategory);
  }

  @Override protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("defaultCategory", defaultCategory);
  }

  public String getDefaultCategory() { return defaultCategory; }
}
