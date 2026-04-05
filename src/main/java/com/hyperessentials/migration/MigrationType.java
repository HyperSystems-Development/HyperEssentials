package com.hyperessentials.migration;

/**
 * Types of migrations supported by the migration framework.
 */
public enum MigrationType {
  /** Configuration file migrations (config.json, module configs). */
  CONFIG,

  /** Data structure migrations (player data, warps, spawns). */
  DATA,

  /** Database schema migrations (for future SQL support). */
  SCHEMA
}
