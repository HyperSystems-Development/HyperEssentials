package com.hyperessentials.config.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hyperessentials.config.ModuleConfig;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the Teleport module (TPA, back, and RTP).
 */
public class TeleportConfig extends ModuleConfig {

  private int tpaTimeout = 60;
  private int tpaCooldown = 30;
  private int maxPendingTpa = 5;
  private int backHistorySize = 5;
  private boolean saveBackOnDeath = true;
  private boolean saveBackOnTeleport = true;
  private boolean backAllowSelectAny = true;

  // RTP settings
  private int rtpCenterX = 0;
  private int rtpCenterZ = 0;
  private int rtpMinRadius = 100;
  private int rtpMaxRadius = 5000;
  private int rtpMaxAttempts = 10;
  private boolean rtpPlayerRelative = true;
  private List<String> rtpBlacklistedWorlds = new ArrayList<>();

  // RTP faction avoidance
  private boolean rtpFactionAvoidanceEnabled = true;
  private int rtpFactionAvoidanceBufferRadius = 2;

  // RTP safety
  private boolean rtpSafetyAvoidWater = true;
  private boolean rtpSafetyAvoidDangerousFluids = true;
  private int rtpSafetyMinY = 5;
  private int rtpSafetyMaxY = 300;
  private int rtpSafetyAirAboveHead = 10;

  public TeleportConfig(@NotNull Path filePath) {
    super(filePath);
  }

  @Override
  @NotNull
  public String getModuleName() {
    return "teleport";
  }

  @Override
  protected void createDefaults() {}

  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    tpaTimeout = getInt(root, "tpaTimeout", tpaTimeout);
    tpaCooldown = getInt(root, "tpaCooldown", tpaCooldown);
    maxPendingTpa = getInt(root, "maxPendingTpa", maxPendingTpa);
    backHistorySize = getInt(root, "backHistorySize", backHistorySize);
    saveBackOnDeath = getBool(root, "saveBackOnDeath", saveBackOnDeath);
    saveBackOnTeleport = getBool(root, "saveBackOnTeleport", saveBackOnTeleport);
    backAllowSelectAny = getBool(root, "backAllowSelectAny", backAllowSelectAny);

    // RTP subsection
    if (hasSection(root, "rtp")) {
      JsonObject rtp = root.getAsJsonObject("rtp");
      rtpCenterX = getInt(rtp, "centerX", rtpCenterX);
      rtpCenterZ = getInt(rtp, "centerZ", rtpCenterZ);
      rtpMinRadius = getInt(rtp, "minRadius", rtpMinRadius);
      rtpMaxRadius = getInt(rtp, "maxRadius", rtpMaxRadius);
      rtpMaxAttempts = getInt(rtp, "maxAttempts", rtpMaxAttempts);
      rtpPlayerRelative = getBool(rtp, "playerRelative", rtpPlayerRelative);

      rtpBlacklistedWorlds = new ArrayList<>();
      if (rtp.has("blacklistedWorlds") && rtp.get("blacklistedWorlds").isJsonArray()) {
        JsonArray arr = rtp.getAsJsonArray("blacklistedWorlds");
        for (int i = 0; i < arr.size(); i++) {
          rtpBlacklistedWorlds.add(arr.get(i).getAsString().toLowerCase());
        }
      }

      // Faction avoidance subsection
      if (hasSection(rtp, "factionAvoidance")) {
        JsonObject fa = rtp.getAsJsonObject("factionAvoidance");
        rtpFactionAvoidanceEnabled = getBool(fa, "enabled", rtpFactionAvoidanceEnabled);
        rtpFactionAvoidanceBufferRadius = getInt(fa, "bufferRadius", rtpFactionAvoidanceBufferRadius);
      }

      // Safety subsection
      if (hasSection(rtp, "safety")) {
        JsonObject safety = rtp.getAsJsonObject("safety");
        rtpSafetyAvoidWater = getBool(safety, "avoidWater", rtpSafetyAvoidWater);
        rtpSafetyAvoidDangerousFluids = getBool(safety, "avoidDangerousFluids", rtpSafetyAvoidDangerousFluids);
        rtpSafetyMinY = getInt(safety, "minY", rtpSafetyMinY);
        rtpSafetyMaxY = getInt(safety, "maxY", rtpSafetyMaxY);
        rtpSafetyAirAboveHead = getInt(safety, "airAboveHead", rtpSafetyAirAboveHead);
      }
    }
  }

  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("tpaTimeout", tpaTimeout);
    root.addProperty("tpaCooldown", tpaCooldown);
    root.addProperty("maxPendingTpa", maxPendingTpa);
    root.addProperty("backHistorySize", backHistorySize);
    root.addProperty("saveBackOnDeath", saveBackOnDeath);
    root.addProperty("saveBackOnTeleport", saveBackOnTeleport);
    root.addProperty("backAllowSelectAny", backAllowSelectAny);

    // RTP subsection
    JsonObject rtp = new JsonObject();
    rtp.addProperty("centerX", rtpCenterX);
    rtp.addProperty("centerZ", rtpCenterZ);
    rtp.addProperty("minRadius", rtpMinRadius);
    rtp.addProperty("maxRadius", rtpMaxRadius);
    rtp.addProperty("maxAttempts", rtpMaxAttempts);
    rtp.addProperty("playerRelative", rtpPlayerRelative);
    JsonArray arr = new JsonArray();
    for (String world : rtpBlacklistedWorlds) {
      arr.add(world);
    }
    rtp.add("blacklistedWorlds", arr);

    // Faction avoidance
    JsonObject fa = new JsonObject();
    fa.addProperty("enabled", rtpFactionAvoidanceEnabled);
    fa.addProperty("bufferRadius", rtpFactionAvoidanceBufferRadius);
    rtp.add("factionAvoidance", fa);

    // Safety
    JsonObject safety = new JsonObject();
    safety.addProperty("avoidWater", rtpSafetyAvoidWater);
    safety.addProperty("avoidDangerousFluids", rtpSafetyAvoidDangerousFluids);
    safety.addProperty("minY", rtpSafetyMinY);
    safety.addProperty("maxY", rtpSafetyMaxY);
    safety.addProperty("airAboveHead", rtpSafetyAirAboveHead);
    rtp.add("safety", safety);

    root.add("rtp", rtp);
  }

  // TPA getters
  public int getTpaTimeout() { return tpaTimeout; }
  public int getTpaCooldown() { return tpaCooldown; }
  public int getMaxPendingTpa() { return maxPendingTpa; }
  public int getBackHistorySize() { return backHistorySize; }
  public boolean isSaveBackOnDeath() { return saveBackOnDeath; }
  public boolean isSaveBackOnTeleport() { return saveBackOnTeleport; }
  public boolean isBackAllowSelectAny() { return backAllowSelectAny; }

  // RTP getters
  public int getRtpCenterX() { return rtpCenterX; }
  public int getRtpCenterZ() { return rtpCenterZ; }
  public int getRtpMinRadius() { return rtpMinRadius; }
  public int getRtpMaxRadius() { return rtpMaxRadius; }
  public int getRtpMaxAttempts() { return rtpMaxAttempts; }
  public boolean isRtpPlayerRelative() { return rtpPlayerRelative; }
  public List<String> getRtpBlacklistedWorlds() { return rtpBlacklistedWorlds; }

  // RTP faction avoidance getters
  public boolean isRtpFactionAvoidanceEnabled() { return rtpFactionAvoidanceEnabled; }
  public int getRtpFactionAvoidanceBufferRadius() { return rtpFactionAvoidanceBufferRadius; }

  // RTP safety getters
  public boolean isRtpSafetyAvoidWater() { return rtpSafetyAvoidWater; }
  public boolean isRtpSafetyAvoidDangerousFluids() { return rtpSafetyAvoidDangerousFluids; }
  public int getRtpSafetyMinY() { return rtpSafetyMinY; }
  public int getRtpSafetyMaxY() { return rtpSafetyMaxY; }
  public int getRtpSafetyAirAboveHead() { return rtpSafetyAirAboveHead; }
}
