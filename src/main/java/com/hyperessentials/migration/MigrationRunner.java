package com.hyperessentials.migration;

import com.hyperessentials.util.Logger;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Executes migrations with backup and rollback support.
 *
 * <p>The runner handles:
 * <ul>
 *   <li>Discovering applicable migrations via {@link MigrationRegistry}</li>
 *   <li>Creating timestamped backups before each migration</li>
 *   <li>Executing migrations in order (chaining v1 -> v2 -> v3)</li>
 *   <li>Rolling back on failure by restoring from backup</li>
 *   <li>Reporting progress via callbacks</li>
 * </ul>
 */
public class MigrationRunner {

  private static final DateTimeFormatter BACKUP_TIMESTAMP =
    DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZoneId.systemDefault());

  private final Path dataDir;
  private final MigrationOptions.ProgressCallback progressCallback;

  public MigrationRunner(@NotNull Path dataDir) {
    this(dataDir, null);
  }

  public MigrationRunner(@NotNull Path dataDir, @Nullable MigrationOptions.ProgressCallback progressCallback) {
    this.dataDir = dataDir;
    this.progressCallback = progressCallback;
  }

  /** Runs all pending migrations of a specific type. */
  @NotNull
  public List<MigrationResult> runPendingMigrations(@NotNull MigrationType type) {
    List<Migration> migrations = MigrationRegistry.get().buildMigrationChain(type, dataDir);

    if (migrations.isEmpty()) {
      Logger.debug("[Migration] No pending %s migrations", type);
      return List.of();
    }

    Logger.info("[Migration] Found %d pending %s migration(s)", migrations.size(), type);

    List<MigrationResult> results = new ArrayList<>();
    for (Migration migration : migrations) {
      MigrationResult result = runMigration(migration);
      results.add(result);

      if (!result.success()) {
        Logger.severe("[Migration] Migration '%s' failed: %s", migration.id(), result.errorMessage());
        if (result.rolledBack()) {
          Logger.info("[Migration] Successfully rolled back to backup");
        }
        break;
      }

      Logger.info("[Migration] Migration '%s' completed successfully in %dms",
        migration.id(), result.duration().toMillis());
    }

    return results;
  }

  /** Runs a single migration with backup and rollback support. */
  @NotNull
  public MigrationResult runMigration(@NotNull Migration migration) {
    Instant startTime = Instant.now();

    Logger.info("[Migration] Running migration '%s': %s", migration.id(), migration.description());
    Logger.info("[Migration] Upgrading from v%d to v%d", migration.fromVersion(), migration.toVersion());

    // Create backup
    Path backupPath = null;
    try {
      backupPath = createBackup(migration);
      Logger.info("[Migration] Created backup at: %s", backupPath);
    } catch (IOException e) {
      Duration duration = Duration.between(startTime, Instant.now());
      Logger.severe("[Migration] Failed to create backup: %s", e.getMessage());
      return MigrationResult.failure(
        migration.id(), migration.fromVersion(), migration.toVersion(),
        null, "Failed to create backup: " + e.getMessage(), false, duration
      );
    }

    // Execute migration
    try {
      MigrationOptions options = progressCallback != null
        ? MigrationOptions.withProgress(backupPath, progressCallback)
        : MigrationOptions.defaults(backupPath);

      MigrationResult result = migration.execute(dataDir, options);

      if (!result.success() && backupPath != null) {
        try {
          rollback(migration, backupPath);
          Duration duration = Duration.between(startTime, Instant.now());
          return MigrationResult.failure(
            migration.id(), migration.fromVersion(), migration.toVersion(),
            backupPath,
            result.errorMessage() != null ? result.errorMessage() : "Migration failed",
            true, duration
          );
        } catch (IOException rollbackError) {
          Logger.severe("[Migration] Rollback failed: %s", rollbackError.getMessage());
          Duration duration = Duration.between(startTime, Instant.now());
          return MigrationResult.failure(
            migration.id(), migration.fromVersion(), migration.toVersion(),
            backupPath,
            "Migration failed and rollback also failed: " + rollbackError.getMessage(),
            false, duration
          );
        }
      }

      return result;

    } catch (Exception e) {
      Logger.severe("[Migration] Migration threw exception: %s", e.getMessage());

      boolean rolledBack = false;
      if (backupPath != null) {
        try {
          rollback(migration, backupPath);
          rolledBack = true;
        } catch (IOException rollbackError) {
          Logger.severe("[Migration] Rollback failed: %s", rollbackError.getMessage());
        }
      }

      Duration duration = Duration.between(startTime, Instant.now());
      return MigrationResult.failure(
        migration.id(), migration.fromVersion(), migration.toVersion(),
        backupPath, e.getMessage(), rolledBack, duration
      );
    }
  }

  /**
   * Creates a backup before running a migration.
   *
   * <p>Backup naming: backup_migration_v{from}-to-v{to}_{timestamp}.zip
   * <br>Location: backups/ directory
   */
  @NotNull
  private Path createBackup(@NotNull Migration migration) throws IOException {
    Path backupsDir = dataDir.resolve("backups");
    Files.createDirectories(backupsDir);

    String timestamp = BACKUP_TIMESTAMP.format(Instant.now());
    String fileName = String.format("backup_migration_v%d-to-v%d_%s.zip",
      migration.fromVersion(), migration.toVersion(), timestamp);
    Path backupFile = backupsDir.resolve(fileName);

    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile.toFile()))) {
      switch (migration.type()) {
        case CONFIG -> {
          Path configFile = dataDir.resolve("config.json");
          if (Files.exists(configFile)) {
            addFileToZip(zos, configFile, "config.json");
          }
          Path configDir = dataDir.resolve("config");
          if (Files.exists(configDir) && Files.isDirectory(configDir)) {
            addDirectoryToZip(zos, configDir, "config");
          }
        }
        case DATA -> {
          Path dataSubDir = dataDir.resolve("data");
          if (Files.exists(dataSubDir) && Files.isDirectory(dataSubDir)) {
            addDirectoryToZip(zos, dataSubDir, "data");
          }
        }
        case SCHEMA -> {
          Logger.warn("[Migration] Schema migration backup not fully implemented");
        }
      }
    }

    Logger.info("[Migration] Created ZIP backup: %s", backupFile.getFileName());
    return backupFile;
  }

  private void addFileToZip(@NotNull ZipOutputStream zos, @NotNull Path file,
      @NotNull String entryName) throws IOException {
    zos.putNextEntry(new ZipEntry(entryName));
    Files.copy(file, zos);
    zos.closeEntry();
  }

  private void addDirectoryToZip(@NotNull ZipOutputStream zos, @NotNull Path dir,
      @NotNull String zipPath) throws IOException {
    Files.walkFileTree(dir, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String entryName = zipPath + "/" + dir.relativize(file).toString().replace("\\", "/");
        addFileToZip(zos, file, entryName);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult preVisitDirectory(Path dir2, BasicFileAttributes attrs) throws IOException {
        String entryName = zipPath + "/" + dir.relativize(dir2).toString().replace("\\", "/") + "/";
        if (!entryName.equals(zipPath + "//")) {
          zos.putNextEntry(new ZipEntry(entryName));
          zos.closeEntry();
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /** Rolls back a failed migration by restoring from the ZIP backup. */
  private void rollback(@NotNull Migration migration, @NotNull Path backupPath) throws IOException {
    Logger.info("[Migration] Rolling back migration '%s' from backup: %s",
      migration.id(), backupPath.getFileName());

    if (!Files.exists(backupPath) || !backupPath.toString().endsWith(".zip")) {
      throw new IOException("Backup file not found or invalid: " + backupPath);
    }

    try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(backupPath))) {
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        Path targetPath = dataDir.resolve(entry.getName());
        if (entry.isDirectory()) {
          Files.createDirectories(targetPath);
        } else {
          Files.createDirectories(targetPath.getParent());
          Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        zis.closeEntry();
      }
    }

    Logger.info("[Migration] Rollback complete - restored from backup");
  }

  /** Convenience static method to run pending migrations. */
  @NotNull
  public static List<MigrationResult> runPendingMigrations(
      @NotNull Path dataDir, @NotNull MigrationType type) {
    return new MigrationRunner(dataDir).runPendingMigrations(type);
  }
}
