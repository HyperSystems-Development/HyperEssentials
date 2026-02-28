package com.hyperessentials.config;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Core configuration for HyperEssentials.
 */
public class CoreConfig extends ConfigFile {

  private String prefixText = "HyperEssentials";
  private String prefixColor = "#FFAA00";
  private String prefixBracketColor = "#AAAAAA";
  private String primaryColor = "#55FFFF";
  private String secondaryColor = "#55FF55";
  private String errorColor = "#FF5555";

  private boolean adminRequiresOp = true;
  private boolean allowWithoutPermissionMod = true;
  private String storageType = "json";
  private boolean updateCheck = true;

  private int configVersion = 1;

  public CoreConfig(@NotNull Path filePath) {
    super(filePath);
  }

  @Override
  protected void createDefaults() {
    // All defaults are set via field initializers
  }

  @Override
  protected void loadFromJson(@NotNull JsonObject root) {
    prefixText = getString(root, "prefixText", prefixText);
    prefixColor = getString(root, "prefixColor", prefixColor);
    prefixBracketColor = getString(root, "prefixBracketColor", prefixBracketColor);
    primaryColor = getString(root, "primaryColor", primaryColor);
    secondaryColor = getString(root, "secondaryColor", secondaryColor);
    errorColor = getString(root, "errorColor", errorColor);
    adminRequiresOp = getBool(root, "adminRequiresOp", adminRequiresOp);
    allowWithoutPermissionMod = getBool(root, "allowWithoutPermissionMod", allowWithoutPermissionMod);
    storageType = getString(root, "storageType", storageType);
    updateCheck = getBool(root, "updateCheck", updateCheck);
    configVersion = getInt(root, "configVersion", configVersion);
  }

  @Override
  @NotNull
  protected JsonObject toJson() {
    JsonObject root = new JsonObject();
    root.addProperty("prefixText", prefixText);
    root.addProperty("prefixColor", prefixColor);
    root.addProperty("prefixBracketColor", prefixBracketColor);
    root.addProperty("primaryColor", primaryColor);
    root.addProperty("secondaryColor", secondaryColor);
    root.addProperty("errorColor", errorColor);
    root.addProperty("adminRequiresOp", adminRequiresOp);
    root.addProperty("allowWithoutPermissionMod", allowWithoutPermissionMod);
    root.addProperty("storageType", storageType);
    root.addProperty("updateCheck", updateCheck);
    root.addProperty("configVersion", configVersion);
    return root;
  }

  @Override
  @NotNull
  public ValidationResult validate() {
    ValidationResult result = new ValidationResult();
    validateHexColor(result, "prefixColor", prefixColor);
    validateHexColor(result, "prefixBracketColor", prefixBracketColor);
    validateHexColor(result, "primaryColor", primaryColor);
    validateHexColor(result, "secondaryColor", secondaryColor);
    validateHexColor(result, "errorColor", errorColor);
    storageType = validateEnum(result, "storageType", storageType,
        new String[]{"json"}, "json");
    return result;
  }

  // Getters
  public String getPrefixText() { return prefixText; }
  public String getPrefixColor() { return prefixColor; }
  public String getPrefixBracketColor() { return prefixBracketColor; }
  public String getPrimaryColor() { return primaryColor; }
  public String getSecondaryColor() { return secondaryColor; }
  public String getErrorColor() { return errorColor; }
  public boolean isAdminRequiresOp() { return adminRequiresOp; }
  public boolean isAllowWithoutPermissionMod() { return allowWithoutPermissionMod; }
  public String getStorageType() { return storageType; }
  public boolean isUpdateCheck() { return updateCheck; }
  public int getConfigVersion() { return configVersion; }
}
