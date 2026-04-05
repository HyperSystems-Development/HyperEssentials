package com.hyperessentials.importer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hyperessentials.backup.BackupManager;
import com.hyperessentials.backup.BackupType;
import com.hyperessentials.storage.StorageProvider;
import com.hyperessentials.util.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for essentials data importers with shared boilerplate.
 *
 * <p>Provides:
 * <ul>
 *   <li>Thread-safe import locking via {@link EssentialsImporter#IMPORT_IN_PROGRESS}</li>
 *   <li>Pre-import backup creation via {@link BackupManager}</li>
 *   <li>JSON file reading utilities</li>
 *   <li>World resolution via {@link WorldResolver}</li>
 *   <li>Progress callback support</li>
 * </ul>
 *
 * <p>Subclasses implement {@link #doImport(Path, ImportResult.Builder)} and
 * {@link #doValidate(Path, ImportValidationReport.Builder)} for format-specific logic.
 */
public abstract class AbstractEssentialsImporter implements EssentialsImporter {

  protected static final Gson GSON = new GsonBuilder()
      .setPrettyPrinting()
      .disableHtmlEscaping()
      .create();

  private static final ReentrantLock IMPORT_LOCK = new ReentrantLock();

  protected final StorageProvider storageProvider;
  protected final BackupManager backupManager;
  protected final WorldResolver worldResolver;

  protected boolean dryRun = false;
  protected boolean overwrite = false;
  protected @Nullable Consumer<String> progressCallback;

  protected AbstractEssentialsImporter(@NotNull StorageProvider storageProvider,
                                       @Nullable BackupManager backupManager) {
    this.storageProvider = storageProvider;
    this.backupManager = backupManager;
    this.worldResolver = new WorldResolver();
  }

  @Override
  @NotNull
  public EssentialsImporter setDryRun(boolean dryRun) {
    this.dryRun = dryRun;
    return this;
  }

  @Override
  @NotNull
  public EssentialsImporter setOverwrite(boolean overwrite) {
    this.overwrite = overwrite;
    return this;
  }

  @Override
  @NotNull
  public EssentialsImporter setProgressCallback(@Nullable Consumer<String> callback) {
    this.progressCallback = callback;
    return this;
  }

  @Override
  @NotNull
  public ImportValidationReport validate(@NotNull Path sourcePath) {
    ImportValidationReport.Builder report = ImportValidationReport.builder();

    if (!Files.exists(sourcePath)) {
      report.error("Source path does not exist: " + sourcePath);
      return report.build();
    }

    if (!Files.isDirectory(sourcePath)) {
      report.error("Source path is not a directory: " + sourcePath);
      return report.build();
    }

    worldResolver.init();
    doValidate(sourcePath, report);

    // Add world warnings from resolver
    for (String unresolved : worldResolver.getUnresolvedWorlds()) {
      report.worldWarning("World not loaded: " + unresolved + " (data will use original reference)");
    }

    return report.build();
  }

  @Override
  @NotNull
  public ImportResult importFrom(@NotNull Path sourcePath) {
    ImportResult.Builder result = ImportResult.builder();
    result.dryRun(dryRun);

    // Acquire global import lock
    if (!IMPORT_LOCK.tryLock()) {
      result.error("Another import is already in progress");
      return result.build();
    }

    try {
      IMPORT_IN_PROGRESS.set(true);

      // Validate source path
      if (!Files.exists(sourcePath)) {
        result.error("Source path does not exist: " + sourcePath);
        return result.build();
      }

      if (!Files.isDirectory(sourcePath)) {
        result.error("Source path is not a directory: " + sourcePath);
        return result.build();
      }

      // Initialize world resolver
      worldResolver.init();

      // Create pre-import backup (unless dry run)
      if (!dryRun && backupManager != null) {
        progress("Creating pre-import backup...");
        try {
          var backupResult = backupManager.createBackup(
              BackupType.MANUAL,
              "pre-import-" + getSourceName().toLowerCase().replace(" ", "-"),
              null
          ).join();

          if (backupResult instanceof BackupManager.BackupResult.Success s) {
            progress("Backup created: " + s.file().getFileName());
          } else if (backupResult instanceof BackupManager.BackupResult.Failure f) {
            result.warning("Backup failed: " + f.error() + " (continuing import)");
          }
        } catch (Exception e) {
          result.warning("Backup failed: " + e.getMessage() + " (continuing import)");
        }
      }

      // Delegate to subclass
      doImport(sourcePath, result);

      // Report unresolved worlds as warnings
      for (String unresolved : worldResolver.getUnresolvedWorlds()) {
        result.warning("World not loaded: " + unresolved + " (data stored with original reference)");
      }

      return result.build();
    } catch (Exception e) {
      Logger.severe("[Import] Unexpected error during %s import: %s", getSourceName(), e.getMessage());
      result.error("Unexpected error: " + e.getMessage());
      return result.build();
    } finally {
      IMPORT_IN_PROGRESS.set(false);
      IMPORT_LOCK.unlock();
    }
  }

  /**
   * Performs the actual import logic. Called within the lock.
   *
   * @param sourcePath the validated source directory
   * @param result the result builder to populate
   */
  protected abstract void doImport(@NotNull Path sourcePath, @NotNull ImportResult.Builder result);

  /**
   * Performs validation logic. Called outside the lock.
   *
   * @param sourcePath the validated source directory
   * @param report the report builder to populate
   */
  protected abstract void doValidate(@NotNull Path sourcePath, @NotNull ImportValidationReport.Builder report);

  // === Utilities ===

  /**
   * Sends a progress message via the callback (if set).
   */
  protected void progress(@NotNull String message) {
    if (progressCallback != null) {
      progressCallback.accept(message);
    }
    Logger.info("[Import][%s] %s", getSourceName(), message);
  }

  /**
   * Reads and parses a JSON file, returning the root JsonObject.
   *
   * @param file the file to read
   * @return the parsed JsonObject, or null if the file doesn't exist or is invalid
   */
  @Nullable
  protected JsonObject readJsonFile(@NotNull Path file) {
    if (!Files.exists(file)) return null;
    try {
      String content = Files.readString(file);
      if (content.isBlank()) return null;
      JsonElement element = JsonParser.parseString(content);
      return element.isJsonObject() ? element.getAsJsonObject() : null;
    } catch (IOException e) {
      Logger.warn("[Import] Failed to read file %s: %s", file, e.getMessage());
      return null;
    } catch (Exception e) {
      Logger.warn("[Import] Failed to parse JSON from %s: %s", file, e.getMessage());
      return null;
    }
  }

  /**
   * Reads and parses a JSON file, returning the root JsonElement (may be array or object).
   */
  @Nullable
  protected JsonElement readJsonElement(@NotNull Path file) {
    if (!Files.exists(file)) return null;
    try {
      String content = Files.readString(file);
      if (content.isBlank()) return null;
      return JsonParser.parseString(content);
    } catch (IOException e) {
      Logger.warn("[Import] Failed to read file %s: %s", file, e.getMessage());
      return null;
    } catch (Exception e) {
      Logger.warn("[Import] Failed to parse JSON from %s: %s", file, e.getMessage());
      return null;
    }
  }

  /**
   * Safely parses a UUID string.
   *
   * @param str the UUID string
   * @return the parsed UUID, or null if invalid
   */
  @Nullable
  protected UUID parseUUID(@Nullable String str) {
    if (str == null || str.isBlank()) return null;
    try {
      return UUID.fromString(str);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Gets a string from a JsonObject safely.
   */
  @Nullable
  protected String getString(@NotNull JsonObject obj, @NotNull String key) {
    JsonElement el = obj.get(key);
    return (el != null && el.isJsonPrimitive()) ? el.getAsString() : null;
  }

  /**
   * Gets a double from a JsonObject safely.
   */
  protected double getDouble(@NotNull JsonObject obj, @NotNull String key, double defaultValue) {
    JsonElement el = obj.get(key);
    if (el != null && el.isJsonPrimitive()) {
      try {
        return el.getAsDouble();
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  /**
   * Gets a float from a JsonObject safely.
   */
  protected float getFloat(@NotNull JsonObject obj, @NotNull String key, float defaultValue) {
    JsonElement el = obj.get(key);
    if (el != null && el.isJsonPrimitive()) {
      try {
        return el.getAsFloat();
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  /**
   * Gets a long from a JsonObject safely.
   */
  protected long getLong(@NotNull JsonObject obj, @NotNull String key, long defaultValue) {
    JsonElement el = obj.get(key);
    if (el != null && el.isJsonPrimitive()) {
      try {
        return el.getAsLong();
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  /**
   * Gets an int from a JsonObject safely.
   */
  protected int getInt(@NotNull JsonObject obj, @NotNull String key, int defaultValue) {
    JsonElement el = obj.get(key);
    if (el != null && el.isJsonPrimitive()) {
      try {
        return el.getAsInt();
      } catch (NumberFormatException e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  /**
   * Gets a boolean from a JsonObject safely.
   */
  protected boolean getBoolean(@NotNull JsonObject obj, @NotNull String key, boolean defaultValue) {
    JsonElement el = obj.get(key);
    if (el != null && el.isJsonPrimitive()) {
      try {
        return el.getAsBoolean();
      } catch (Exception e) {
        return defaultValue;
      }
    }
    return defaultValue;
  }

  /**
   * Resolves a world name to (worldName, worldUuid), falling back to storing the
   * original name with a placeholder UUID if the world is not loaded.
   */
  @NotNull
  protected WorldResolver.ResolvedWorld resolveWorldByName(@NotNull String worldName) {
    WorldResolver.ResolvedWorld resolved = worldResolver.resolveByName(worldName);
    if (resolved != null) return resolved;
    // Fallback: store original name with empty UUID
    return new WorldResolver.ResolvedWorld(worldName, "");
  }

  /**
   * Resolves a world UUID to (worldName, worldUuid), falling back to storing the
   * UUID with a "Unknown" name if the world is not loaded.
   */
  @NotNull
  protected WorldResolver.ResolvedWorld resolveWorldByUuid(@NotNull String worldUuidStr) {
    WorldResolver.ResolvedWorld resolved = worldResolver.resolveByUuid(worldUuidStr);
    if (resolved != null) return resolved;
    // Fallback: store UUID with unknown name
    return new WorldResolver.ResolvedWorld("Unknown", worldUuidStr);
  }
}
