package com.hyperessentials.util;

/**
 * Chunk math utility for RTP location generation.
 * Hytale uses 32-block chunks.
 */
public final class RtpChunkUtil {

  public static final int CHUNK_SIZE = 32;
  private static final int CHUNK_SHIFT = 5;

  private RtpChunkUtil() {}

  /**
   * Converts a world coordinate to a chunk coordinate.
   *
   * @param coord world coordinate (X or Z)
   * @return chunk coordinate
   */
  public static int toChunkCoord(double coord) {
    return (int) Math.floor(coord) >> CHUNK_SHIFT;
  }

  /**
   * Converts a chunk coordinate to the minimum block coordinate of that chunk.
   *
   * @param chunkCoord chunk coordinate
   * @return minimum block coordinate
   */
  public static int chunkToBlockMin(int chunkCoord) {
    return chunkCoord << CHUNK_SHIFT;
  }
}
