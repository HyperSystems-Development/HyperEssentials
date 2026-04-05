package com.hyperessentials.backup;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.BackupConfig;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.Logger;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages backup creation, restoration, and GFS (Grandfather-Father-Son) rotation.
 *
 * <p>Backup schedule:
 * - Hourly (Son): Every hour, keep last N hours (default: 24)
 * - Daily (Father): At midnight, keep last N days (default: 7)
 * - Weekly (Grandfather): Sunday midnight, keep last N weeks (default: 4)
 * - Manual: User-created, never auto-deleted
 *
 * <p>Backup contents:
 * - data/players/ directory
 * - data/homes/ directory
 * - data/warps/ directory
 * - data/spawns/ directory
 * - data/kits/ directory
 * - config/ directory (module configurations)
 * - config.json (core configuration)
 */
public class BackupManager {

  /** Result of a backup operation. */
  public sealed interface BackupResult permits BackupResult.Success, BackupResult.Failure {
    record Success(@NotNull BackupMetadata metadata, @NotNull Path file) implements BackupResult {}
    record Failure(@NotNull String error) implements BackupResult {}
  }

  /** Result of a restore operation. */
  public sealed interface RestoreResult permits RestoreResult.Success, RestoreResult.Failure {
    record Success(@NotNull String backupName, int filesRestored) implements RestoreResult {}
    record Failure(@NotNull String error) implements RestoreResult {}
  }

  private final Path dataDir;

  private final Path backupsDir;

  private volatile boolean initialized = false;

  private volatile boolean backupInProgress = false;

  private final Object backupLock = new Object();

  /**
   * Creates a new BackupManager.
   *
   * @param dataDir the plugin data directory
   */
  public BackupManager(@NotNull Path dataDir) {
    this.dataDir = dataDir;
    this.backupsDir = dataDir.resolve("backups");
  }

  /**
   * Initializes the backup manager (creates directories).
   */
  public void init() {
    try {
      Files.createDirectories(backupsDir);
      initialized = true;
      Logger.info("[Backup] Initialized, backup directory: %s", backupsDir);
    } catch (IOException e) {
      ErrorHandler.report("[Backup] Failed to create backups directory", e);
    }
  }

  /**
   * Shuts down the backup manager.
   * Creates a shutdown backup if configured.
   */
  public void shutdown() {
    // Wait for any in-progress backup to complete
    synchronized (backupLock) {
      if (backupInProgress) {
        Logger.info("[Backup] Waiting for in-progress backup to complete...");
        for (int i = 0; i < 20 && backupInProgress; i++) {
          try {
            backupLock.wait(500);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }

    // Create shutdown backup if enabled
    BackupConfig cfg = ConfigManager.get().backup();
    if (cfg.isEnabled() && cfg.isOnShutdown()) {
      Logger.info("[Backup] Creating shutdown backup...");
      createBackup(BackupType.MANUAL, "shutdown", null).join();
      rotateShutdownBackups();
    }
  }

  /**
   * Creates a backup asynchronously.
   *
   * @param type       the backup type
   * @param customName optional custom name for manual backups
   * @param createdBy  the UUID of the player creating the backup (null for auto)
   * @return a future containing the backup result
   */
  @NotNull
  public CompletableFuture<BackupResult> createBackup(
      @NotNull BackupType type,
      @Nullable String customName,
      @Nullable UUID createdBy) {

    return CompletableFuture.supplyAsync(() -> {
      Path dataPath = dataDir.resolve("data");
      Instant timestamp = Instant.now();
      String name;
      if (type == BackupType.MANUAL && customName != null && !customName.isEmpty()) {
        name = BackupMetadata.generateManualName(customName);
      } else {
        name = BackupMetadata.generateName(type, timestamp);
      }

      Path backupFile = backupsDir.resolve(name + ".zip");

      try {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile.toFile()))) {
          // Add data/players/ directory
          Path playersDir = dataPath.resolve("players");
          if (Files.exists(playersDir)) {
            addDirectoryToZip(zos, playersDir, "data/players");
          }

          // Add data/homes/ directory
          Path homesDir = dataPath.resolve("homes");
          if (Files.exists(homesDir)) {
            addDirectoryToZip(zos, homesDir, "data/homes");
          }

          // Add data/warps/ directory
          Path warpsDir = dataPath.resolve("warps");
          if (Files.exists(warpsDir)) {
            addDirectoryToZip(zos, warpsDir, "data/warps");
          }

          // Add data/spawns/ directory
          Path spawnsDir = dataPath.resolve("spawns");
          if (Files.exists(spawnsDir)) {
            addDirectoryToZip(zos, spawnsDir, "data/spawns");
          }

          // Add data/kits/ directory
          Path kitsDir = dataPath.resolve("kits");
          if (Files.exists(kitsDir)) {
            addDirectoryToZip(zos, kitsDir, "data/kits");
          }

          // Add config.json (core configuration)
          Path configFile = dataDir.resolve("config.json");
          if (Files.exists(configFile)) {
            addFileToZip(zos, configFile, "config.json");
          }

          // Add config/ directory (module configs)
          Path configDir = dataDir.resolve("config");
          if (Files.exists(configDir)) {
            addDirectoryToZip(zos, configDir, "config");
          }
        }

        // Create metadata
        long size = Files.size(backupFile);
        BackupMetadata metadata = new BackupMetadata(name, type, timestamp, size, createdBy);

        return new BackupResult.Success(metadata, backupFile);

      } catch (Exception e) {
        // Delete incomplete backup ZIP
        try {
          Files.deleteIfExists(backupFile);
        } catch (IOException ignored) {}
        ErrorHandler.report("[Backup] Failed to create backup", e);
        return new BackupResult.Failure("Failed to create backup: " + e.getMessage());
      }
    });
  }

  /**
   * Restores from a backup asynchronously.
   *
   * @param backupName the name of the backup to restore
   * @return a future containing the restore result
   */
  @NotNull
  public CompletableFuture<RestoreResult> restoreBackup(@NotNull String backupName) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Path backupFile = findBackupFile(backupName);
        if (backupFile == null) {
          return new RestoreResult.Failure("Backup not found: " + backupName);
        }

        int filesRestored = 0;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(backupFile))) {
          ZipEntry entry;
          while ((entry = zis.getNextEntry()) != null) {
            if (entry.isDirectory()) {
              Files.createDirectories(dataDir.resolve(entry.getName()));
            } else {
              Path targetFile = dataDir.resolve(entry.getName());
              Files.createDirectories(targetFile.getParent());
              Files.copy(zis, targetFile, StandardCopyOption.REPLACE_EXISTING);
              filesRestored++;
            }
            zis.closeEntry();
          }
        }

        Logger.info("[Backup] Restored %d files from backup '%s'", filesRestored, backupName);
        return new RestoreResult.Success(backupName, filesRestored);

      } catch (Exception e) {
        ErrorHandler.report("[Backup] Failed to restore backup", e);
        return new RestoreResult.Failure("Failed to restore backup: " + e.getMessage());
      }
    });
  }

  /**
   * Deletes a backup.
   *
   * @param backupName the name of the backup to delete
   * @return true if deleted successfully
   */
  @NotNull
  public CompletableFuture<Boolean> deleteBackup(@NotNull String backupName) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        Path backupFile = findBackupFile(backupName);
        if (backupFile == null) {
          return false;
        }
        Files.delete(backupFile);
        Logger.info("[Backup] Deleted backup: %s", backupName);
        return true;
      } catch (Exception e) {
        ErrorHandler.report(String.format("[Backup] Failed to delete backup '%s'", backupName), e);
        return false;
      }
    });
  }

  /**
   * Lists all available backups.
   *
   * @return list of backup metadata, sorted by timestamp (newest first)
   */
  @NotNull
  public List<BackupMetadata> listBackups() {
    List<BackupMetadata> backups = new ArrayList<>();

    try {
      if (!Files.exists(backupsDir)) {
        return backups;
      }

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupsDir, "backup_*.zip")) {
        for (Path file : stream) {
          try {
            long size = Files.size(file);
            BackupMetadata metadata = BackupMetadata.fromFilename(file.getFileName().toString(), size);
            if (metadata != null) {
              Instant fileTime = Files.getLastModifiedTime(file).toInstant();
              backups.add(new BackupMetadata(
                metadata.name(), metadata.type(), fileTime, size, metadata.createdBy()
              ));
            }
          } catch (Exception e) {
            ErrorHandler.report(
                String.format("[Backup] Could not read backup metadata for %s", file.getFileName()), e);
          }
        }
      }
    } catch (IOException e) {
      ErrorHandler.report("[Backup] Failed to list backups", e);
    }

    // Sort by timestamp, newest first
    backups.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));
    return backups;
  }

  /**
   * Gets backups grouped by type.
   *
   * @return map of backup type to list of backups
   */
  @NotNull
  public Map<BackupType, List<BackupMetadata>> getBackupsGroupedByType() {
    return listBackups().stream()
      .collect(Collectors.groupingBy(BackupMetadata::type));
  }

  /**
   * Performs GFS rotation, pruning old backups according to retention settings.
   */
  public void performRotation() {
    BackupConfig cfg = ConfigManager.get().backup();
    Map<BackupType, List<BackupMetadata>> grouped = getBackupsGroupedByType();

    rotateBackups(grouped.getOrDefault(BackupType.HOURLY, List.of()),
      cfg.getHourlyRetention());
    rotateBackups(grouped.getOrDefault(BackupType.DAILY, List.of()),
      cfg.getDailyRetention());
    rotateBackups(grouped.getOrDefault(BackupType.WEEKLY, List.of()),
      cfg.getWeeklyRetention());

    int manualRetention = cfg.getManualRetention();
    if (manualRetention > 0) {
      rotateBackups(grouped.getOrDefault(BackupType.MANUAL, List.of()), manualRetention);
    }
  }

  /**
   * Rotates backups of a specific type, keeping only the most recent N.
   */
  private void rotateBackups(@NotNull List<BackupMetadata> backups, int retentionCount) {
    if (retentionCount <= 0 || backups.size() <= retentionCount) {
      return;
    }

    List<BackupMetadata> sorted = new ArrayList<>(backups);
    sorted.sort((a, b) -> b.timestamp().compareTo(a.timestamp()));

    for (int i = retentionCount; i < sorted.size(); i++) {
      BackupMetadata toDelete = sorted.get(i);
      deleteBackup(toDelete.name()).thenAccept(success -> {
        if (success) {
          Logger.debug("[Backup] Rotated out old backup: %s", toDelete.name());
        }
      });
    }
  }

  /**
   * Rotates shutdown backups, keeping only the most recent N.
   */
  private void rotateShutdownBackups() {
    int retention = ConfigManager.get().backup().getShutdownRetention();
    if (retention <= 0) {
      return;
    }

    try {
      List<Path> shutdownBackups = new ArrayList<>();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupsDir, "backup_manual_shutdown_*.zip")) {
        for (Path file : stream) {
          shutdownBackups.add(file);
        }
      }

      if (shutdownBackups.size() <= retention) {
        return;
      }

      shutdownBackups.sort((a, b) -> {
        try {
          return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
        } catch (IOException e) {
          return 0;
        }
      });

      for (int i = retention; i < shutdownBackups.size(); i++) {
        Path toDelete = shutdownBackups.get(i);
        try {
          Files.delete(toDelete);
          Logger.debug("[Backup] Rotated out old shutdown backup: %s", toDelete.getFileName());
        } catch (IOException e) {
          ErrorHandler.report(
              String.format("[Backup] Failed to delete old shutdown backup %s", toDelete.getFileName()), e);
        }
      }

      int deleted = shutdownBackups.size() - retention;
      Logger.info("[Backup] Cleaned up %d old shutdown backup(s), keeping %d", deleted, retention);
    } catch (IOException e) {
      ErrorHandler.report("[Backup] Failed to rotate shutdown backups", e);
    }
  }

  /**
   * Finds a backup file by name.
   */
  @Nullable
  private Path findBackupFile(@NotNull String name) {
    Path exact = backupsDir.resolve(name + ".zip");
    if (Files.exists(exact)) {
      return exact;
    }

    Path withPrefix = backupsDir.resolve("backup_" + name + ".zip");
    if (Files.exists(withPrefix)) {
      return withPrefix;
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupsDir, "*" + name + "*.zip")) {
      for (Path file : stream) {
        return file;
      }
    } catch (IOException ignored) {}

    return null;
  }

  /**
   * Adds a directory to a ZIP output stream.
   */
  private void addDirectoryToZip(@NotNull ZipOutputStream zos, @NotNull Path dir, @NotNull String zipPath)
      throws IOException {
    Files.walkFileTree(dir, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        String fileName = file.getFileName().toString();
        if (fileName.endsWith(".tmp") || fileName.endsWith(".bak")) {
          return FileVisitResult.CONTINUE;
        }
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

      @Override
      public FileVisitResult visitFileFailed(Path file, IOException exc) {
        Logger.debug("[Backup] Skipping vanished file during backup: %s", file.getFileName());
        return FileVisitResult.CONTINUE;
      }
    });
  }

  /**
   * Adds a single file to a ZIP output stream.
   */
  private void addFileToZip(@NotNull ZipOutputStream zos, @NotNull Path file, @NotNull String entryName)
      throws IOException {
    zos.putNextEntry(new ZipEntry(entryName));
    Files.copy(file, zos);
    zos.closeEntry();
  }

  /**
   * Gets the backups directory path.
   *
   * @return the backups directory
   */
  @NotNull
  public Path getBackupsDir() {
    return backupsDir;
  }
}
