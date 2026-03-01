package com.hyperessentials.module.teleport;

import com.hyperessentials.config.modules.TeleportConfig;
import com.hyperessentials.data.Location;
import com.hyperessentials.integration.BorderHook;
import com.hyperessentials.integration.HyperFactionsIntegration;
import com.hyperessentials.util.Logger;
import com.hyperessentials.util.RtpChunkUtil;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Manages random teleport location generation with chunk-based safety verification,
 * faction claim avoidance, and world border enforcement.
 * <p>
 * Must be called on the world thread (requires chunk/block access).
 */
public class RtpManager {

  private final TeleportConfig config;
  private BorderHook borderHook = (world, x, z) -> true;

  public RtpManager(@NotNull TeleportConfig config) {
    this.config = config;
  }

  /**
   * Sets the border hook for world border enforcement.
   *
   * @param hook the border hook implementation
   */
  public void setBorderHook(@NotNull BorderHook hook) {
    this.borderHook = hook;
  }

  /**
   * Result of an RTP search attempt.
   */
  public sealed interface RtpResult {
    record Success(@NotNull Location location) implements RtpResult {}
    record Failure(@NotNull String reason) implements RtpResult {}
  }

  /**
   * Finds a safe random location. Must be called on the world thread.
   *
   * @param world          the world instance (for chunk/block access)
   * @param worldName      the world name
   * @param playerX        player's current X coordinate (used when playerRelative=true)
   * @param playerZ        player's current Z coordinate (used when playerRelative=true)
   * @param bypassFactions whether to skip faction claim checks
   * @return the result of the search
   */
  @NotNull
  public RtpResult findSafeRandomLocation(@NotNull World world, @NotNull String worldName,
                                           double playerX, double playerZ,
                                           boolean bypassFactions) {
    int maxAttempts = config.getRtpMaxAttempts();
    int minRadius = config.getRtpMinRadius();
    int maxRadius = config.getRtpMaxRadius();
    boolean checkFactions = config.isRtpFactionAvoidanceEnabled() && !bypassFactions;
    int factionBuffer = config.getRtpFactionAvoidanceBufferRadius();

    // Center: player position or configured center
    double centerX = config.isRtpPlayerRelative() ? playerX : config.getRtpCenterX();
    double centerZ = config.isRtpPlayerRelative() ? playerZ : config.getRtpCenterZ();

    Logger.debugRtp("Starting RTP search: center=(%.0f, %.0f), radius=[%d, %d], maxAttempts=%d, factions=%s",
      centerX, centerZ, minRadius, maxRadius, maxAttempts, checkFactions ? "check" : "bypass");

    ThreadLocalRandom random = ThreadLocalRandom.current();

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      // Step 1: Random direction (continuous angle)
      double angle = random.nextDouble() * 2 * Math.PI;

      // Step 2: Random distance within ring
      double distance = minRadius + random.nextDouble() * (maxRadius - minRadius);

      // Step 3: Compute target world coordinates
      double targetX = centerX + distance * Math.cos(angle);
      double targetZ = centerZ + distance * Math.sin(angle);

      Logger.debugRtp("Attempt %d: angle=%.1f°, dist=%.0f, target=(%.0f, %.0f)",
        attempt, Math.toDegrees(angle), distance, targetX, targetZ);

      // Step 4: Border check
      if (!borderHook.isWithinBorder(worldName, targetX, targetZ)) {
        Logger.debugRtp("Attempt %d: outside world border, retrying", attempt);
        continue;
      }

      // Step 5: Faction claim check (buffer radius around target chunk)
      int targetChunkX = RtpChunkUtil.toChunkCoord(targetX);
      int targetChunkZ = RtpChunkUtil.toChunkCoord(targetZ);

      if (checkFactions && isAnyClaimNearby(worldName, targetChunkX, targetChunkZ, factionBuffer)) {
        Logger.debugRtp("Attempt %d: faction claim within buffer radius %d, retrying", attempt, factionBuffer);
        continue;
      }

      // Step 6: Random position within the target chunk
      int blockX = RtpChunkUtil.chunkToBlockMin(targetChunkX) + random.nextInt(RtpChunkUtil.CHUNK_SIZE);
      int blockZ = RtpChunkUtil.chunkToBlockMin(targetChunkZ) + random.nextInt(RtpChunkUtil.CHUNK_SIZE);

      // Step 7: Load chunk + safety scan
      long chunkIndex = ChunkUtil.indexChunkFromBlock(blockX, blockZ);
      WorldChunk chunk = world.getNonTickingChunk(chunkIndex);
      if (chunk == null) {
        Logger.debugRtp("Attempt %d: chunk not loaded at (%d, %d), retrying", attempt, blockX, blockZ);
        continue;
      }

      int safeY = findSafeY(world, chunk, blockX, blockZ);
      if (safeY < 0) {
        Logger.debugRtp("Attempt %d: no safe Y found at (%d, %d), retrying", attempt, blockX, blockZ);
        continue;
      }

      // Center player on block
      double finalX = blockX + 0.5;
      double finalZ = blockZ + 0.5;
      Location location = new Location(worldName, finalX, safeY, finalZ, 0, 0);

      Logger.debugRtp("RTP success on attempt %d: (%.1f, %d, %.1f)", attempt, finalX, safeY, finalZ);
      return new RtpResult.Success(location);
    }

    Logger.debugRtp("RTP search FAILED after %d attempts", maxAttempts);
    return new RtpResult.Failure("Could not find a safe random location after " + maxAttempts + " attempts.");
  }

  /**
   * Scans from heightmap downward to find a safe Y coordinate.
   * Checks for solid ground with 2 empty non-fluid blocks for the player body,
   * plus additional air blocks above to ensure surface placement (not caves).
   * The number of air blocks above head is configurable via safety.airAboveHead.
   *
   * @return safe Y coordinate (feet level), or -1 if none found
   */
  private int findSafeY(@NotNull World world, @NotNull WorldChunk chunk, int blockX, int blockZ) {
    int topY = chunk.getHeight(blockX, blockZ);
    int minY = config.getRtpSafetyMinY();
    int maxY = config.getRtpSafetyMaxY();
    int airAboveHead = config.getRtpSafetyAirAboveHead();
    boolean avoidWater = config.isRtpSafetyAvoidWater();
    boolean avoidDangerous = config.isRtpSafetyAvoidDangerousFluids();

    // Clamp scan start to maxY
    int scanStart = Math.min(topY, maxY);

    for (int y = scanStart; y >= minY; y--) {
      BlockType ground = world.getBlockType(blockX, y, blockZ);
      BlockType feet = world.getBlockType(blockX, y + 1, blockZ);
      BlockType head = world.getBlockType(blockX, y + 2, blockZ);

      // Ground must be solid
      boolean groundSolid = ground != null && ground.getMaterial() == BlockMaterial.Solid;
      if (!groundSolid) continue;

      // Feet and head must be empty
      boolean feetClear = feet == null || feet.getMaterial() == BlockMaterial.Empty;
      boolean headClear = head == null || head.getMaterial() == BlockMaterial.Empty;
      if (!feetClear || !headClear) continue;

      // Check additional air blocks above head to avoid caves
      boolean aboveClear = true;
      for (int above = 3; above <= 2 + airAboveHead; above++) {
        BlockType aboveBlock = world.getBlockType(blockX, y + above, blockZ);
        if (aboveBlock != null && aboveBlock.getMaterial() == BlockMaterial.Solid) {
          aboveClear = false;
          break;
        }
      }
      if (!aboveClear) {
        Logger.debugRtp("Rejected y=%d at (%d, %d): solid block within %d blocks above head (cave)",
          y, blockX, blockZ, airAboveHead);
        continue;
      }

      // Fluid checks
      if (avoidWater) {
        int feetFluid = chunk.getFluidId(blockX, y + 1, blockZ);
        int headFluid = chunk.getFluidId(blockX, y + 2, blockZ);
        if (feetFluid != Fluid.EMPTY_ID || headFluid != Fluid.EMPTY_ID) continue;
      }

      // Dangerous fluid check below ground (prevents thin-crust-over-lava)
      if (avoidDangerous && y > minY) {
        int belowFluid = chunk.getFluidId(blockX, y - 1, blockZ);
        if (belowFluid != Fluid.EMPTY_ID) {
          Fluid fluid = Fluid.getAssetMap().getAsset(belowFluid);
          if (fluid != null && fluid.getDamageToEntities() > 0) continue;
        }
      }

      return y + 1; // Feet level
    }

    return -1; // No safe position found
  }

  /**
   * Checks whether any faction claim exists within the buffer radius of the target chunk.
   * Uses world-coordinate lookups via HyperFactionsIntegration.
   *
   * @param worldName   the world name
   * @param chunkX      target chunk X
   * @param chunkZ      target chunk Z
   * @param radiusChunks buffer radius in chunks (e.g. 2 = 5x5 check area)
   * @return true if any claim is nearby
   */
  private boolean isAnyClaimNearby(@NotNull String worldName, int chunkX, int chunkZ, int radiusChunks) {
    for (int dx = -radiusChunks; dx <= radiusChunks; dx++) {
      for (int dz = -radiusChunks; dz <= radiusChunks; dz++) {
        // Convert chunk center to world coordinates for the integration lookup
        int worldX = RtpChunkUtil.chunkToBlockMin(chunkX + dx) + RtpChunkUtil.CHUNK_SIZE / 2;
        int worldZ = RtpChunkUtil.chunkToBlockMin(chunkZ + dz) + RtpChunkUtil.CHUNK_SIZE / 2;

        String faction = HyperFactionsIntegration.getFactionAtLocation(worldName, worldX, worldZ);
        if (faction != null) {
          Logger.debugRtp("Claim found at chunk (%d, %d): %s", chunkX + dx, chunkZ + dz, faction);
          return true;
        }
      }
    }
    return false;
  }

  public boolean isWorldBlacklisted(@NotNull String worldName) {
    return config.getRtpBlacklistedWorlds().contains(worldName.toLowerCase());
  }

  public int getMaxAttempts() {
    return config.getRtpMaxAttempts();
  }
}
