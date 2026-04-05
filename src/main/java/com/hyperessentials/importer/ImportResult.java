package com.hyperessentials.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Results of an essentials data import operation.
 *
 * <p>Tracks counts for each data type (homes, warps, spawns, kits, punishments)
 * along with warnings, errors, and whether the import was a dry run.
 */
public record ImportResult(
    int homesImported,
    int homesSkipped,
    int warpsImported,
    int warpsSkipped,
    int spawnsImported,
    int kitsImported,
    int kitsSkipped,
    int punishmentsImported,
    @NotNull List<String> warnings,
    @NotNull List<String> errors,
    boolean dryRun
) {
  /**
   * Creates a builder for constructing an ImportResult.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Checks if the import had any errors.
   *
   * @return true if there were errors
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Checks if the import had any warnings.
   *
   * @return true if there were warnings
   */
  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  /**
   * Gets the total number of items imported across all data types.
   *
   * @return total count of imported items
   */
  public int getTotalImported() {
    return homesImported + warpsImported + spawnsImported + kitsImported + punishmentsImported;
  }

  /**
   * Gets the total number of items skipped across all data types.
   *
   * @return total count of skipped items
   */
  public int getTotalSkipped() {
    return homesSkipped + warpsSkipped + kitsSkipped;
  }

  /**
   * Builder for ImportResult with increment methods for use during import.
   */
  public static class Builder {

    private int homesImported = 0;

    private int homesSkipped = 0;

    private int warpsImported = 0;

    private int warpsSkipped = 0;

    private int spawnsImported = 0;

    private int kitsImported = 0;

    private int kitsSkipped = 0;

    private int punishmentsImported = 0;

    private final List<String> warnings = new ArrayList<>();

    private final List<String> errors = new ArrayList<>();

    private boolean dryRun = false;

    /** Sets the homes imported count. */
    public Builder homesImported(int count) {
      this.homesImported = count;
      return this;
    }

    /** Increments homes imported by one. */
    public Builder incrementHomesImported() {
      this.homesImported++;
      return this;
    }

    /** Sets the homes skipped count. */
    public Builder homesSkipped(int count) {
      this.homesSkipped = count;
      return this;
    }

    /** Increments homes skipped by one. */
    public Builder incrementHomesSkipped() {
      this.homesSkipped++;
      return this;
    }

    /** Sets the warps imported count. */
    public Builder warpsImported(int count) {
      this.warpsImported = count;
      return this;
    }

    /** Increments warps imported by one. */
    public Builder incrementWarpsImported() {
      this.warpsImported++;
      return this;
    }

    /** Sets the warps skipped count. */
    public Builder warpsSkipped(int count) {
      this.warpsSkipped = count;
      return this;
    }

    /** Increments warps skipped by one. */
    public Builder incrementWarpsSkipped() {
      this.warpsSkipped++;
      return this;
    }

    /** Sets the spawns imported count. */
    public Builder spawnsImported(int count) {
      this.spawnsImported = count;
      return this;
    }

    /** Increments spawns imported by one. */
    public Builder incrementSpawnsImported() {
      this.spawnsImported++;
      return this;
    }

    /** Sets the kits imported count. */
    public Builder kitsImported(int count) {
      this.kitsImported = count;
      return this;
    }

    /** Increments kits imported by one. */
    public Builder incrementKitsImported() {
      this.kitsImported++;
      return this;
    }

    /** Sets the kits skipped count. */
    public Builder kitsSkipped(int count) {
      this.kitsSkipped = count;
      return this;
    }

    /** Increments kits skipped by one. */
    public Builder incrementKitsSkipped() {
      this.kitsSkipped++;
      return this;
    }

    /** Sets the punishments imported count. */
    public Builder punishmentsImported(int count) {
      this.punishmentsImported = count;
      return this;
    }

    /** Increments punishments imported by one. */
    public Builder incrementPunishmentsImported() {
      this.punishmentsImported++;
      return this;
    }

    /** Adds a warning message. */
    public Builder warning(String message) {
      this.warnings.add(message);
      return this;
    }

    /** Adds an error message. */
    public Builder error(String message) {
      this.errors.add(message);
      return this;
    }

    /** Sets whether this was a dry run. */
    public Builder dryRun(boolean dryRun) {
      this.dryRun = dryRun;
      return this;
    }

    /** Builds an immutable ImportResult. */
    public ImportResult build() {
      return new ImportResult(
          homesImported,
          homesSkipped,
          warpsImported,
          warpsSkipped,
          spawnsImported,
          kitsImported,
          kitsSkipped,
          punishmentsImported,
          Collections.unmodifiableList(new ArrayList<>(warnings)),
          Collections.unmodifiableList(new ArrayList<>(errors)),
          dryRun
      );
    }
  }
}
