package com.hyperessentials.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;

/**
 * Validation report generated before an import operation.
 *
 * <p>Provides a summary of what would be imported, along with any name conflicts,
 * world warnings, and blocking errors. Used by {@code --dry-run} and pre-import checks.
 */
public record ImportValidationReport(
    int totalHomes,
    int totalWarps,
    int totalSpawns,
    int totalKits,
    int totalPlayers,
    @NotNull List<String> nameConflicts,
    @NotNull List<String> worldWarnings,
    @NotNull List<String> warnings,
    @NotNull List<String> errors,
    boolean valid
) {
  /**
   * Creates a builder for constructing an ImportValidationReport.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Checks if there are blocking issues that prevent import.
   * An import should not proceed if this returns true.
   *
   * @return true if there are errors
   */
  public boolean hasBlockingIssues() {
    return !errors.isEmpty();
  }

  /**
   * Checks if there are name conflicts with existing data.
   *
   * @return true if name conflicts exist
   */
  public boolean hasConflicts() {
    return !nameConflicts.isEmpty();
  }

  /**
   * Gets all issues (errors, warnings, name conflicts, world warnings) combined.
   *
   * @return combined list of all issues
   */
  @NotNull
  public List<String> getAllIssues() {
    return Stream.of(errors, nameConflicts, worldWarnings, warnings)
        .flatMap(List::stream)
        .toList();
  }

  /**
   * Gets a human-readable summary of the validation report.
   *
   * @return summary string
   */
  @NotNull
  public String getSummary() {
    StringBuilder sb = new StringBuilder();
    sb.append("Found: ");

    List<String> parts = new ArrayList<>();
    if (totalHomes > 0) parts.add(totalHomes + " home(s)");
    if (totalWarps > 0) parts.add(totalWarps + " warp(s)");
    if (totalSpawns > 0) parts.add(totalSpawns + " spawn(s)");
    if (totalKits > 0) parts.add(totalKits + " kit(s)");
    if (totalPlayers > 0) parts.add(totalPlayers + " player(s)");

    if (parts.isEmpty()) {
      sb.append("no data");
    } else {
      sb.append(String.join(", ", parts));
    }

    if (!nameConflicts.isEmpty()) {
      sb.append(" | ").append(nameConflicts.size()).append(" conflict(s)");
    }
    if (!worldWarnings.isEmpty()) {
      sb.append(" | ").append(worldWarnings.size()).append(" world warning(s)");
    }
    if (!errors.isEmpty()) {
      sb.append(" | ").append(errors.size()).append(" error(s)");
    }

    return sb.toString();
  }

  /**
   * Builder for ImportValidationReport.
   */
  public static class Builder {

    private int totalHomes = 0;

    private int totalWarps = 0;

    private int totalSpawns = 0;

    private int totalKits = 0;

    private int totalPlayers = 0;

    private final List<String> nameConflicts = new ArrayList<>();

    private final List<String> worldWarnings = new ArrayList<>();

    private final List<String> warnings = new ArrayList<>();

    private final List<String> errors = new ArrayList<>();

    private boolean valid = true;

    /** Sets total homes found in source. */
    public Builder totalHomes(int count) {
      this.totalHomes = count;
      return this;
    }

    /** Sets total warps found in source. */
    public Builder totalWarps(int count) {
      this.totalWarps = count;
      return this;
    }

    /** Sets total spawns found in source. */
    public Builder totalSpawns(int count) {
      this.totalSpawns = count;
      return this;
    }

    /** Sets total kits found in source. */
    public Builder totalKits(int count) {
      this.totalKits = count;
      return this;
    }

    /** Sets total players found in source data. */
    public Builder totalPlayers(int count) {
      this.totalPlayers = count;
      return this;
    }

    /** Adds a name conflict (e.g., warp name already exists). */
    public Builder nameConflict(String message) {
      this.nameConflicts.add(message);
      return this;
    }

    /** Adds a world warning (e.g., referenced world not loaded). */
    public Builder worldWarning(String message) {
      this.worldWarnings.add(message);
      return this;
    }

    /** Adds a general warning. */
    public Builder warning(String message) {
      this.warnings.add(message);
      return this;
    }

    /** Adds an error that blocks import. */
    public Builder error(String message) {
      this.errors.add(message);
      this.valid = false;
      return this;
    }

    /** Marks the report as invalid without adding an error message. */
    public Builder invalid() {
      this.valid = false;
      return this;
    }

    /** Builds an immutable ImportValidationReport. */
    public ImportValidationReport build() {
      return new ImportValidationReport(
          totalHomes,
          totalWarps,
          totalSpawns,
          totalKits,
          totalPlayers,
          Collections.unmodifiableList(new ArrayList<>(nameConflicts)),
          Collections.unmodifiableList(new ArrayList<>(worldWarnings)),
          Collections.unmodifiableList(new ArrayList<>(warnings)),
          Collections.unmodifiableList(new ArrayList<>(errors)),
          valid
      );
    }
  }
}
