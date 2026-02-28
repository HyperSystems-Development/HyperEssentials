package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import com.hyperessentials.config.ModuleConfig;

public class VanishConfig extends ModuleConfig {

  private boolean fakeLeaveMessage = true;
  private boolean fakeJoinMessage = true;
  private String vanishEnableMessage = "You are now vanished.";
  private String vanishDisableMessage = "You are no longer vanished.";
  private boolean silentJoin = false;

  public VanishConfig(@NotNull Path filePath) { super(filePath); }

  @Override @NotNull public String getModuleName() { return "vanish"; }
  @Override protected boolean getDefaultEnabled() { return true; }

  @Override
  protected void createDefaults() {
    fakeLeaveMessage = true;
    fakeJoinMessage = true;
    vanishEnableMessage = "You are now vanished.";
    vanishDisableMessage = "You are no longer vanished.";
    silentJoin = false;
  }

  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    fakeLeaveMessage = getBool(root, "fakeLeaveMessage", true);
    fakeJoinMessage = getBool(root, "fakeJoinMessage", true);
    vanishEnableMessage = getString(root, "vanishEnableMessage", "You are now vanished.");
    vanishDisableMessage = getString(root, "vanishDisableMessage", "You are no longer vanished.");
    silentJoin = getBool(root, "silentJoin", false);
  }

  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("fakeLeaveMessage", fakeLeaveMessage);
    root.addProperty("fakeJoinMessage", fakeJoinMessage);
    root.addProperty("vanishEnableMessage", vanishEnableMessage);
    root.addProperty("vanishDisableMessage", vanishDisableMessage);
    root.addProperty("silentJoin", silentJoin);
  }

  public boolean isFakeLeaveMessage() { return fakeLeaveMessage; }
  public boolean isFakeJoinMessage() { return fakeJoinMessage; }
  public String getVanishEnableMessage() { return vanishEnableMessage; }
  public String getVanishDisableMessage() { return vanishDisableMessage; }
  public boolean isSilentJoin() { return silentJoin; }
}
