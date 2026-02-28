package com.hyperessentials.migration;

import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Core interface for all migrations.
 * Migrations handle versioned changes to configuration files, data structures,
 * or database schemas.
 */
public interface Migration {

  /** Gets the unique identifier (e.g., "config-v1-to-v2"). */
  @NotNull
  String id();

  /** Gets the type of this migration. */
  @NotNull
  MigrationType type();

  /** Gets the source version this migration upgrades from. */
  int fromVersion();

  /** Gets the target version this migration upgrades to. */
  int toVersion();

  /** Gets a human-readable description of what this migration does. */
  @NotNull
  String description();

  /** Checks if this migration is applicable to the current data directory. */
  boolean isApplicable(@NotNull Path dataDir);

  /** Executes the migration. */
  @NotNull
  MigrationResult execute(@NotNull Path dataDir, @NotNull MigrationOptions options);
}
