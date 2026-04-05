package com.hyperessentials.storage;

import com.hyperessentials.util.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for bulletproof file storage operations.
 * Provides atomic writes with checksums, verification, and backup recovery.
 */
public final class StorageUtils {

  private StorageUtils() {}

  /** Counter for unique temp file names to prevent race conditions. */
  private static final AtomicLong TEMP_COUNTER = new AtomicLong(System.currentTimeMillis());

  /** File extension for temporary files during writes. */
  private static final String TMP_SUFFIX = ".tmp";

  /** File extension for backup files. */
  private static final String BAK_SUFFIX = ".bak";

  /**
   * Sealed interface representing the result of a write operation.
   */
  public sealed interface WriteResult permits WriteResult.Success, WriteResult.Failure {

    /**
     * Successful write result.
     *
     * @param file     the target file that was written
     * @param checksum the SHA-256 checksum of the written content
     */
    record Success(@NotNull Path file, @NotNull String checksum) implements WriteResult {}

    /**
     * Failed write result.
     *
     * @param file  the target file that failed to write
     * @param error human-readable error message
     * @param cause the underlying exception (may be null)
     */
    record Failure(@NotNull Path file, @NotNull String error, @Nullable Exception cause) implements WriteResult {}
  }

  /**
   * Atomically writes content to a file using a temp file and rename pattern.
   * This ensures the file is never in a corrupted state, even if the process crashes.
   *
   * <p>Steps:
   * 1. Write content to file.tmp
   * 2. Calculate SHA-256 checksum
   * 3. Verify by reading back and comparing checksum
   * 4. Backup existing file to file.bak
   * 5. Atomic rename: tmp -> target
   *
   * @param targetFile the final destination file
   * @param content    the content to write
   * @return WriteResult indicating success or failure
   */
  @NotNull
  public static WriteResult writeAtomic(@NotNull Path targetFile, @NotNull String content) {
    long uniqueId = TEMP_COUNTER.incrementAndGet();
    Path tempFile = targetFile.resolveSibling(targetFile.getFileName() + "." + uniqueId + TMP_SUFFIX);
    Path backupFile = targetFile.resolveSibling(targetFile.getFileName() + BAK_SUFFIX);

    try {
      // Step 1: Ensure parent directory exists
      Path parent = targetFile.getParent();
      if (parent != null && !Files.exists(parent)) {
        Files.createDirectories(parent);
      }

      // Step 2: Write to temp file
      Files.writeString(tempFile, content);

      // Step 3: Calculate checksum of original content
      String expectedChecksum = computeChecksum(content);

      // Step 4: Verify by reading back the temp file
      String writtenContent = Files.readString(tempFile);
      String actualChecksum = computeChecksum(writtenContent);

      if (!expectedChecksum.equals(actualChecksum)) {
        Files.deleteIfExists(tempFile);
        String error = "Checksum verification failed: expected " + expectedChecksum + ", got " + actualChecksum;
        Logger.severe("[Storage] %s for %s", error, targetFile);
        return new WriteResult.Failure(targetFile, error, null);
      }

      // Step 5: Backup existing file (if it exists)
      if (Files.exists(targetFile)) {
        try {
          Files.copy(targetFile, backupFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
          Logger.warn("[Storage] Could not create backup for %s: %s", targetFile, e.getMessage());
        }
      }

      // Step 6: Atomic rename temp -> target
      moveWithRetry(tempFile, targetFile);

      // Step 7: Clean up .bak file after successful write
      try {
        Files.deleteIfExists(backupFile);
      } catch (IOException e) {
        Logger.debug("[Storage] Could not delete .bak file for %s: %s", targetFile.getFileName(), e.getMessage());
      }

      Logger.debug("[Storage] Atomic write successful: %s (checksum: %s)", targetFile.getFileName(), expectedChecksum.substring(0, 8));
      return new WriteResult.Success(targetFile, expectedChecksum);

    } catch (IOException e) {
      try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
      String error = "I/O error during atomic write: " + e.getMessage();
      Logger.severe("[Storage] %s for %s", error, targetFile);
      return new WriteResult.Failure(targetFile, error, e);

    } catch (Exception e) {
      try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
      String error = "Unexpected error during atomic write: " + e.getMessage();
      Logger.severe("[Storage] %s for %s", error, targetFile);
      return new WriteResult.Failure(targetFile, error, e);
    }
  }

  /** Maximum number of retry attempts for atomic move before falling back. */
  private static final int MOVE_MAX_RETRIES = 3;

  /** Base delay in milliseconds between retry attempts (multiplied by attempt number). */
  private static final long MOVE_RETRY_BASE_DELAY_MS = 50;

  /**
   * Moves a temp file to the target, retrying on failure and falling back to
   * a non-atomic move if the atomic move keeps failing (common on Windows when
   * antivirus or indexer holds a brief handle on the target file).
   */
  private static void moveWithRetry(@NotNull Path tempFile, @NotNull Path targetFile) throws IOException {
    try {
      Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      return;
    } catch (IOException firstEx) {
      Logger.debug("[Storage] Atomic move failed for %s, retrying (%s)", targetFile.getFileName(), firstEx.getMessage());

      for (int attempt = 1; attempt <= MOVE_MAX_RETRIES; attempt++) {
        try {
          Thread.sleep(MOVE_RETRY_BASE_DELAY_MS * attempt);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted during move retry", ie);
        }

        try {
          Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
          Logger.debug("[Storage] Atomic move succeeded on retry %d for %s", attempt, targetFile.getFileName());
          return;
        } catch (IOException retryEx) {
          Logger.debug("[Storage] Atomic move retry %d/%d failed for %s", attempt, MOVE_MAX_RETRIES, targetFile.getFileName());
        }
      }

      // All atomic attempts failed — fall back to non-atomic move
      try {
        Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        Logger.warn("[Storage] Used non-atomic move fallback for %s (file contention)", targetFile.getFileName());
      } catch (IOException fallbackEx) {
        throw new IOException(tempFile.getFileName() + " -> " + targetFile.getFileName(), fallbackEx);
      }
    }
  }

  /**
   * Computes the SHA-256 checksum of the given content.
   */
  @NotNull
  public static String computeChecksum(@NotNull String content) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return bytesToHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  /**
   * Attempts to recover a file from its backup.
   */
  public static boolean recoverFromBackup(@NotNull Path targetFile) {
    Path backupFile = targetFile.resolveSibling(targetFile.getFileName() + BAK_SUFFIX);

    if (!Files.exists(backupFile)) {
      Logger.warn("[Storage] No backup file found for %s", targetFile);
      return false;
    }

    try {
      String backupContent = Files.readString(backupFile);
      if (backupContent.isEmpty()) {
        Logger.warn("[Storage] Backup file is empty for %s", targetFile);
        return false;
      }

      Files.copy(backupFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
      Logger.info("[Storage] Recovered %s from backup (size: %d bytes)", targetFile.getFileName(), backupContent.length());
      return true;

    } catch (IOException e) {
      Logger.severe("[Storage] Failed to recover %s from backup: %s", targetFile, e.getMessage());
      return false;
    }
  }

  /**
   * Checks if a backup file exists for the given target file.
   */
  public static boolean hasBackup(@NotNull Path targetFile) {
    Path backupFile = targetFile.resolveSibling(targetFile.getFileName() + BAK_SUFFIX);
    return Files.exists(backupFile);
  }

  /**
   * Gets the backup file path for a given target file.
   */
  @NotNull
  public static Path getBackupPath(@NotNull Path targetFile) {
    return targetFile.resolveSibling(targetFile.getFileName() + BAK_SUFFIX);
  }

  /**
   * Deletes a file and its associated backup file.
   */
  public static boolean deleteWithBackup(@NotNull Path targetFile) {
    Path backupFile = getBackupPath(targetFile);
    boolean mainDeleted = false;

    try {
      mainDeleted = Files.deleteIfExists(targetFile);
    } catch (IOException e) {
      Logger.warn("[Storage] Failed to delete %s: %s", targetFile.getFileName(), e.getMessage());
    }

    try {
      if (Files.deleteIfExists(backupFile)) {
        Logger.debug("[Storage] Deleted backup file: %s", backupFile.getFileName());
      }
    } catch (IOException e) {
      Logger.warn("[Storage] Failed to delete backup %s: %s", backupFile.getFileName(), e.getMessage());
    }

    return mainDeleted;
  }

  /**
   * Cleans up orphaned temporary and backup files in a directory.
   * Call this on startup to remove leftover files from crashes.
   */
  public static int cleanupOrphanedFiles(@NotNull Path directory) {
    if (!Files.exists(directory) || !Files.isDirectory(directory)) {
      return 0;
    }

    int cleaned = 0;

    try (var stream = Files.newDirectoryStream(directory)) {
      for (Path file : stream) {
        String fileName = file.getFileName().toString();

        if (fileName.endsWith(TMP_SUFFIX)) {
          try {
            Files.delete(file);
            cleaned++;
            Logger.debug("[Storage] Cleaned orphaned temp file: %s", fileName);
          } catch (IOException e) {
            Logger.warn("[Storage] Failed to clean temp file %s: %s", fileName, e.getMessage());
          }
          continue;
        }

        if (fileName.endsWith(BAK_SUFFIX)) {
          String baseName = fileName.substring(0, fileName.length() - BAK_SUFFIX.length());
          Path mainFile = directory.resolve(baseName);
          if (!Files.exists(mainFile)) {
            try {
              Files.delete(file);
              cleaned++;
              Logger.debug("[Storage] Cleaned orphaned backup file: %s", fileName);
            } catch (IOException e) {
              Logger.warn("[Storage] Failed to clean backup file %s: %s", fileName, e.getMessage());
            }
          }
        }
      }
    } catch (IOException e) {
      Logger.warn("[Storage] Failed to scan directory for cleanup: %s", e.getMessage());
    }

    if (cleaned > 0) {
      Logger.info("[Storage] Cleaned up %d orphaned file(s) from %s", cleaned, directory.getFileName());
    }

    return cleaned;
  }

  @NotNull
  private static String bytesToHex(@NotNull byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
