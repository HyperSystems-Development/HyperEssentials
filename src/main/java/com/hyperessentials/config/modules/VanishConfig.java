package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import com.hyperessentials.config.ModuleConfig;

public class VanishConfig extends ModuleConfig {
    public VanishConfig(@NotNull Path filePath) { super(filePath); }
    @Override @NotNull public String getModuleName() { return "vanish"; }
    @Override protected boolean getDefaultEnabled() { return false; }
    @Override protected void createDefaults() {}
    @Override protected void loadModuleSettings(@NotNull JsonObject root) {}
    @Override protected void writeModuleSettings(@NotNull JsonObject root) {}
}
