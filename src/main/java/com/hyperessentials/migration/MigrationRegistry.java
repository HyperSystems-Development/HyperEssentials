package com.hyperessentials.migration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.jetbrains.annotations.NotNull;

/**
 * Registry for all available migrations.
 *
 * <p>Migrations are registered by type and can be queried for applicable
 * migrations based on current data state.
 */
public class MigrationRegistry {

  private static final MigrationRegistry INSTANCE = new MigrationRegistry();

  private final Map<MigrationType, List<Migration>> migrations = new ConcurrentHashMap<>();

  private MigrationRegistry() {
    // No built-in migrations yet — register them here as they are created
  }

  /** Gets the singleton registry instance. */
  @NotNull
  public static MigrationRegistry get() {
    return INSTANCE;
  }

  /** Registers a migration. */
  public void register(@NotNull Migration migration) {
    migrations.computeIfAbsent(migration.type(), k -> new ArrayList<>()).add(migration);
  }

  /** Gets all migrations of a specific type, sorted by fromVersion. */
  @NotNull
  public List<Migration> getMigrations(@NotNull MigrationType type) {
    List<Migration> typeMigrations = migrations.get(type);
    if (typeMigrations == null) {
      return List.of();
    }
    return typeMigrations.stream()
      .sorted(Comparator.comparingInt(Migration::fromVersion))
      .toList();
  }

  /** Gets all applicable migrations for the given data directory, in execution order. */
  @NotNull
  public List<Migration> getApplicableMigrations(@NotNull MigrationType type, @NotNull Path dataDir) {
    return getMigrations(type).stream()
      .filter(m -> m.isApplicable(dataDir))
      .toList();
  }

  /** Builds a migration chain from current version to latest. */
  @NotNull
  public List<Migration> buildMigrationChain(@NotNull MigrationType type, @NotNull Path dataDir) {
    List<Migration> applicable = getApplicableMigrations(type, dataDir);
    if (applicable.isEmpty()) {
      return List.of();
    }
    // For now, return applicable migrations in order.
    // Future: build a proper chain checking version continuity.
    return applicable;
  }

  /** Checks if any migrations are pending for a type. */
  public boolean hasPendingMigrations(@NotNull MigrationType type, @NotNull Path dataDir) {
    return !getApplicableMigrations(type, dataDir).isEmpty();
  }
}
