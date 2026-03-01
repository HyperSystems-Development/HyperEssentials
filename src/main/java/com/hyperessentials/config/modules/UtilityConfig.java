package com.hyperessentials.config.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
  private boolean durabilityEnabled = true;

  // New command toggles
  private boolean motdEnabled = true;
  private boolean rulesEnabled = true;
  private boolean discordEnabled = true;
  private boolean listEnabled = true;
  private boolean playtimeEnabled = true;
  private boolean joindateEnabled = true;
  private boolean afkEnabled = true;
  private boolean invseeEnabled = true;
  private boolean staminaEnabled = true;
  private boolean trashEnabled = true;
  private boolean maxstackEnabled = true;
  private boolean sleepPercentageEnabled = true;

  // MOTD/Rules/Discord content
  private List<String> motdLines = List.of("Welcome to the server!");
  private List<String> ruleLines = List.of("1. Be respectful", "2. No griefing", "3. Have fun!");
  private String discordUrl = "";

  // AFK
  private int afkTimeoutSeconds = 300;

  // Sleep percentage
  private int sleepPercentage = 50;
  private Map<String, Integer> worldSleepPercentages = new HashMap<>();

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
    durabilityEnabled = true;
    motdEnabled = true;
    rulesEnabled = true;
    discordEnabled = true;
    listEnabled = true;
    playtimeEnabled = true;
    joindateEnabled = true;
    afkEnabled = true;
    invseeEnabled = true;
    staminaEnabled = true;
    trashEnabled = true;
    maxstackEnabled = true;
    sleepPercentageEnabled = true;
    motdLines = List.of("Welcome to the server!");
    ruleLines = List.of("1. Be respectful", "2. No griefing", "3. Have fun!");
    discordUrl = "";
    afkTimeoutSeconds = 300;
    sleepPercentage = 50;
    worldSleepPercentages = new HashMap<>();
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
    durabilityEnabled = getBool(root, "durabilityEnabled", true);

    motdEnabled = getBool(root, "motdEnabled", true);
    rulesEnabled = getBool(root, "rulesEnabled", true);
    discordEnabled = getBool(root, "discordEnabled", true);
    listEnabled = getBool(root, "listEnabled", true);
    playtimeEnabled = getBool(root, "playtimeEnabled", true);
    joindateEnabled = getBool(root, "joindateEnabled", true);
    afkEnabled = getBool(root, "afkEnabled", true);
    invseeEnabled = getBool(root, "invseeEnabled", true);
    staminaEnabled = getBool(root, "staminaEnabled", true);
    trashEnabled = getBool(root, "trashEnabled", true);
    maxstackEnabled = getBool(root, "maxstackEnabled", true);
    sleepPercentageEnabled = getBool(root, "sleepPercentageEnabled", true);

    discordUrl = getString(root, "discordUrl", "");
    afkTimeoutSeconds = getInt(root, "afkTimeoutSeconds", 300);
    sleepPercentage = getInt(root, "sleepPercentage", 50);

    // Load motdLines
    if (root.has("motdLines") && root.get("motdLines").isJsonArray()) {
      motdLines = new ArrayList<>();
      root.getAsJsonArray("motdLines").forEach(e -> motdLines.add(e.getAsString()));
    }

    // Load ruleLines
    if (root.has("ruleLines") && root.get("ruleLines").isJsonArray()) {
      ruleLines = new ArrayList<>();
      root.getAsJsonArray("ruleLines").forEach(e -> ruleLines.add(e.getAsString()));
    }

    // Load world sleep percentages
    if (root.has("worldSleepPercentages") && root.get("worldSleepPercentages").isJsonObject()) {
      worldSleepPercentages = new HashMap<>();
      JsonObject wsp = root.getAsJsonObject("worldSleepPercentages");
      for (var entry : wsp.entrySet()) {
        worldSleepPercentages.put(entry.getKey(), entry.getValue().getAsInt());
      }
    }
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
    root.addProperty("durabilityEnabled", durabilityEnabled);

    root.addProperty("motdEnabled", motdEnabled);
    root.addProperty("rulesEnabled", rulesEnabled);
    root.addProperty("discordEnabled", discordEnabled);
    root.addProperty("listEnabled", listEnabled);
    root.addProperty("playtimeEnabled", playtimeEnabled);
    root.addProperty("joindateEnabled", joindateEnabled);
    root.addProperty("afkEnabled", afkEnabled);
    root.addProperty("invseeEnabled", invseeEnabled);
    root.addProperty("staminaEnabled", staminaEnabled);
    root.addProperty("trashEnabled", trashEnabled);
    root.addProperty("maxstackEnabled", maxstackEnabled);
    root.addProperty("sleepPercentageEnabled", sleepPercentageEnabled);

    root.addProperty("discordUrl", discordUrl);
    root.addProperty("afkTimeoutSeconds", afkTimeoutSeconds);
    root.addProperty("sleepPercentage", sleepPercentage);

    JsonArray motdArr = new JsonArray();
    motdLines.forEach(motdArr::add);
    root.add("motdLines", motdArr);

    JsonArray rulesArr = new JsonArray();
    ruleLines.forEach(rulesArr::add);
    root.add("ruleLines", rulesArr);

    JsonObject wsp = new JsonObject();
    worldSleepPercentages.forEach(wsp::addProperty);
    root.add("worldSleepPercentages", wsp);
  }

  // Existing getters
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
  public boolean isDurabilityEnabled() { return durabilityEnabled; }

  // New getters
  public boolean isMotdEnabled() { return motdEnabled; }
  public boolean isRulesEnabled() { return rulesEnabled; }
  public boolean isDiscordEnabled() { return discordEnabled; }
  public boolean isListEnabled() { return listEnabled; }
  public boolean isPlaytimeEnabled() { return playtimeEnabled; }
  public boolean isJoindateEnabled() { return joindateEnabled; }
  public boolean isAfkEnabled() { return afkEnabled; }
  public boolean isInvseeEnabled() { return invseeEnabled; }
  public boolean isStaminaEnabled() { return staminaEnabled; }
  public boolean isTrashEnabled() { return trashEnabled; }
  public boolean isMaxstackEnabled() { return maxstackEnabled; }
  public boolean isSleepPercentageEnabled() { return sleepPercentageEnabled; }
  public List<String> getMotdLines() { return motdLines; }
  public List<String> getRuleLines() { return ruleLines; }
  public String getDiscordUrl() { return discordUrl; }
  public int getAfkTimeoutSeconds() { return afkTimeoutSeconds; }
  public int getSleepPercentage() { return sleepPercentage; }
  public void setSleepPercentage(int sleepPercentage) { this.sleepPercentage = sleepPercentage; }
  public Map<String, Integer> getWorldSleepPercentages() { return worldSleepPercentages; }
}
