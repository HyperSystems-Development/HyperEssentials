package com.hyperessentials.importer;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for importing essentials data from external sources.
 *
 * <p>Implementations should be thread-safe and use the global import lock
 * via {@link #isImportInProgress()} to prevent concurrent imports.
 *
 * <p>Typical usage:
 * <pre>
 *   EssentialsImporter importer = new SomePluginImporter(storageProvider);
 *   importer.setDryRun(false);
 *   importer.setOverwrite(true);
 *   importer.setProgressCallback(msg -> ctx.sendMessage(msg));
 *
 *   ImportValidationReport report = importer.validate(sourcePath);
 *   if (!report.hasBlockingIssues()) {
 *     ImportResult result = importer.importFrom(sourcePath);
 *   }
 * </pre>
 */
public interface EssentialsImporter {

  /** Global flag to prevent concurrent imports across all importer instances. */
  AtomicBoolean IMPORT_IN_PROGRESS = new AtomicBoolean(false);

  /**
   * Gets the display name of the import source (e.g., "HyperHomes", "EssentialsX").
   *
   * @return the source name
   */
  @NotNull
  String getSourceName();

  /**
   * Gets the default file/directory path for this import source,
   * relative to the server root (e.g., "mods/SomePlugin").
   *
   * @return the default import path
   */
  @NotNull
  String getDefaultPath();

  /**
   * Validates the source data without making any changes.
   * Scans the source path and reports what would be imported,
   * along with any conflicts or issues.
   *
   * @param sourcePath the path to the source data
   * @return a validation report
   */
  @NotNull
  ImportValidationReport validate(@NotNull Path sourcePath);

  /**
   * Imports data from the source path into HyperEssentials storage.
   * Respects dry-run and overwrite settings.
   *
   * <p>Implementations must:
   * <ul>
   *   <li>Check and set {@link #IMPORT_IN_PROGRESS} at the start</li>
   *   <li>Clear the flag on completion (including on error)</li>
   *   <li>Report progress via the callback if set</li>
   * </ul>
   *
   * @param sourcePath the path to the source data
   * @return the import result with counts and any issues
   */
  @NotNull
  ImportResult importFrom(@NotNull Path sourcePath);

  /**
   * Sets whether this import should be a dry run (no changes made).
   *
   * @param dryRun true for dry run
   * @return this importer for chaining
   */
  @NotNull
  EssentialsImporter setDryRun(boolean dryRun);

  /**
   * Sets whether existing data should be overwritten during import.
   *
   * @param overwrite true to overwrite existing data
   * @return this importer for chaining
   */
  @NotNull
  EssentialsImporter setOverwrite(boolean overwrite);

  /**
   * Sets a callback for receiving progress updates during import.
   *
   * @param callback the progress callback, or null to disable
   * @return this importer for chaining
   */
  @NotNull
  EssentialsImporter setProgressCallback(@Nullable Consumer<String> callback);

  /**
   * Checks if any import operation is currently in progress.
   * Uses a global atomic flag shared across all importer instances.
   *
   * @return true if an import is running
   */
  static boolean isImportInProgress() {
    return IMPORT_IN_PROGRESS.get();
  }
}
