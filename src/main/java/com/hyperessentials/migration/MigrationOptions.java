package com.hyperessentials.migration;

import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Options for migration execution.
 *
 * @param backupPath       path where backup was created
 * @param dryRun           if true, don't actually modify files
 * @param verbose          if true, log detailed progress
 * @param progressCallback callback for progress updates (may be null)
 */
public record MigrationOptions(
  Path backupPath,
  boolean dryRun,
  boolean verbose,
  ProgressCallback progressCallback
) {
  /** Callback interface for migration progress updates. */
  @FunctionalInterface
  public interface ProgressCallback {
    void onProgress(@NotNull String step, int current, int total);
  }

  /** Creates default options with no callback. */
  public static MigrationOptions defaults(Path backupPath) {
    return new MigrationOptions(backupPath, false, false, null);
  }

  /** Creates options with progress callback. */
  public static MigrationOptions withProgress(Path backupPath, ProgressCallback callback) {
    return new MigrationOptions(backupPath, false, false, callback);
  }

  /** Reports progress if a callback is registered. */
  public void reportProgress(@NotNull String step, int current, int total) {
    if (progressCallback != null) {
      progressCallback.onProgress(step, current, total);
    }
  }
}
