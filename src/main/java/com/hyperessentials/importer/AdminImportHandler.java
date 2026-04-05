package com.hyperessentials.importer;

import com.hyperessentials.backup.BackupManager;
import com.hyperessentials.storage.StorageProvider;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.HEMessageUtil;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Handles {@code /essimport} and {@code /he admin import} commands.
 *
 * <p>Parses source name, optional path, and flags ({@code --dry-run}, {@code --overwrite}),
 * then delegates to the appropriate {@link EssentialsImporter} implementation.
 *
 * <p>Supported sources:
 * <ul>
 *   <li>{@code elite} — EliteEssentials (v1.1.21)</li>
 *   <li>{@code essentials} / {@code nhulston} — Essentials by nhulston (v1.8.0)</li>
 *   <li>{@code essplus} — EssentialsPlus by fof1092 (v1.16.1)</li>
 *   <li>{@code hesx} — hyessentialsx by thelegacyvoyage (v1.4.1)</li>
 * </ul>
 */
public class AdminImportHandler {

  private static final String COLOR_GREEN = HEMessageUtil.COLOR_GREEN;
  private static final String COLOR_RED = HEMessageUtil.COLOR_RED;
  private static final String COLOR_YELLOW = HEMessageUtil.COLOR_YELLOW;
  private static final String COLOR_GRAY = HEMessageUtil.COLOR_GRAY;
  private static final String COLOR_GOLD = HEMessageUtil.COLOR_GOLD;

  private final StorageProvider storageProvider;
  private final BackupManager backupManager;

  public AdminImportHandler(@NotNull StorageProvider storageProvider,
                            @Nullable BackupManager backupManager) {
    this.storageProvider = storageProvider;
    this.backupManager = backupManager;
  }

  /**
   * Entry point for import commands.
   *
   * @param ctx the command context
   * @param player the player executing the command (null for console)
   * @param args arguments after "import" (e.g., ["elite", "--dry-run"])
   */
  public void handleImport(@NotNull CommandContext ctx, @Nullable PlayerRef player, @NotNull String[] args) {
    if (args.length == 0) {
      showImportHelp(ctx, player);
      return;
    }

    String subCmd = args[0].toLowerCase();
    String[] subArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];

    switch (subCmd) {
      case "elite", "eliteessentials" ->
          runImporter(ctx, player, new EliteEssentialsImporter(storageProvider, backupManager), subArgs);
      case "essentials", "nhulston" ->
          runImporter(ctx, player, new NhulstonEssentialsImporter(storageProvider, backupManager), subArgs);
      case "essplus", "essentialsplus" ->
          runImporter(ctx, player, new EssentialsPlusImporter(storageProvider, backupManager), subArgs);
      case "hesx", "hyessentialsx" ->
          runImporter(ctx, player, new HyEssentialsXImporter(storageProvider, backupManager), subArgs);
      case "help", "?" -> showImportHelp(ctx, player);
      default -> {
        ctx.sendMessage(HEMessageUtil.error(player, CommandKeys.Import.UNKNOWN_SOURCE, subCmd));
        showImportHelp(ctx, player);
      }
    }
  }

  /**
   * Runs an importer with parsed flags and optional custom path.
   * Handles the full lifecycle: flag parsing, lock check, async execution, result reporting.
   *
   * @param ctx the command context
   * @param player the player (for i18n)
   * @param importer the importer to run
   * @param args remaining args (optional path + flags)
   */
  private void runImporter(@NotNull CommandContext ctx, @Nullable PlayerRef player,
                           @NotNull EssentialsImporter importer, @NotNull String[] args) {
    // Check global import lock
    if (EssentialsImporter.isImportInProgress()) {
      ctx.sendMessage(HEMessageUtil.error(player, CommandKeys.Import.ALREADY_IN_PROGRESS));
      return;
    }

    // Parse optional path (first non-flag argument)
    String pathStr = importer.getDefaultPath();
    int flagStartIndex = 0;

    if (args.length > 0 && !args[0].startsWith("-")) {
      pathStr = args[0];
      flagStartIndex = 1;
    }

    Path dataPath = Paths.get(pathStr);

    // Parse flags
    boolean dryRun = false;
    boolean overwrite = false;

    for (int i = flagStartIndex; i < args.length; i++) {
      String flag = args[i].toLowerCase();
      switch (flag) {
        case "--dry-run", "-n" -> dryRun = true;
        case "--overwrite" -> overwrite = true;
        default -> {
          ctx.sendMessage(HEMessageUtil.error(player, CommandKeys.Import.UNKNOWN_FLAG, args[i]));
          return;
        }
      }
    }

    // Configure importer
    importer.setDryRun(dryRun);
    importer.setOverwrite(overwrite);
    importer.setProgressCallback(msg -> ctx.sendMessage(HEMessageUtil.info(msg, COLOR_GRAY)));

    // Announce start
    String sourceName = importer.getSourceName();
    ctx.sendMessage(HEMessageUtil.info(player, CommandKeys.Import.STARTING, COLOR_YELLOW, sourceName));
    ctx.sendMessage(HEMessageUtil.info("  Path: " + dataPath, COLOR_GRAY));
    if (dryRun) {
      ctx.sendMessage(HEMessageUtil.info(player, CommandKeys.Import.DRY_RUN_NOTICE, COLOR_GRAY));
    }

    // Run async
    final boolean finalDryRun = dryRun;
    CompletableFuture.supplyAsync(() -> importer.importFrom(dataPath))
        .thenAccept(result -> reportResult(ctx, player, result, finalDryRun, sourceName))
        .exceptionally(e -> {
          ErrorHandler.report("[Import] Import from " + sourceName + " failed", e);
          ctx.sendMessage(HEMessageUtil.error(player, CommandKeys.Import.FAILED, sourceName));
          return null;
        });
  }

  /**
   * Reports the result of an import operation to the command sender.
   */
  private void reportResult(@NotNull CommandContext ctx, @Nullable PlayerRef player,
                            @NotNull ImportResult result, boolean dryRun, @NotNull String sourceName) {
    if (!result.hasErrors()) {
      String mode = dryRun ? "simulation " : "";
      ctx.sendMessage(HEMessageUtil.success(player, CommandKeys.Import.COMPLETE, sourceName, mode));

      // Report counts
      if (result.homesImported() > 0 || result.homesSkipped() > 0) {
        ctx.sendMessage(HEMessageUtil.info("  Homes: " + result.homesImported()
            + (result.homesSkipped() > 0 ? " (skipped: " + result.homesSkipped() + ")" : ""), COLOR_GRAY));
      }
      if (result.warpsImported() > 0 || result.warpsSkipped() > 0) {
        ctx.sendMessage(HEMessageUtil.info("  Warps: " + result.warpsImported()
            + (result.warpsSkipped() > 0 ? " (skipped: " + result.warpsSkipped() + ")" : ""), COLOR_GRAY));
      }
      if (result.spawnsImported() > 0) {
        ctx.sendMessage(HEMessageUtil.info("  Spawns: " + result.spawnsImported(), COLOR_GRAY));
      }
      if (result.kitsImported() > 0 || result.kitsSkipped() > 0) {
        ctx.sendMessage(HEMessageUtil.info("  Kits: " + result.kitsImported()
            + (result.kitsSkipped() > 0 ? " (skipped: " + result.kitsSkipped() + ")" : ""), COLOR_GRAY));
      }
      if (result.punishmentsImported() > 0) {
        ctx.sendMessage(HEMessageUtil.info("  Punishments: " + result.punishmentsImported(), COLOR_GRAY));
      }

      if (result.hasWarnings()) {
        ctx.sendMessage(HEMessageUtil.info("  Warnings: " + result.warnings().size()
            + " (check server logs)", COLOR_YELLOW));
        // Log warnings to console
        for (String warning : result.warnings()) {
          Logger.warn("[Import] %s", warning);
        }
      }
    } else {
      ctx.sendMessage(HEMessageUtil.error(player, CommandKeys.Import.FAILED, sourceName));
      for (String error : result.errors()) {
        ctx.sendMessage(HEMessageUtil.info("  - " + error, COLOR_RED));
      }
    }
  }

  /**
   * Shows help text listing supported import sources and flags.
   */
  private void showImportHelp(@NotNull CommandContext ctx, @Nullable PlayerRef player) {
    ctx.sendMessage(HEMessageUtil.info("=== Data Import ===", COLOR_GOLD));
    ctx.sendMessage(HEMessageUtil.info(player, CommandKeys.Import.HELP_USAGE, COLOR_YELLOW));
    ctx.sendMessage(HEMessageUtil.info("", COLOR_GRAY));
    ctx.sendMessage(HEMessageUtil.info(player, CommandKeys.Import.HELP_SOURCES_HEADER, COLOR_YELLOW));
    ctx.sendMessage(HEMessageUtil.info("  elite          EliteEssentials (v1.1.21)", COLOR_GRAY));
    ctx.sendMessage(HEMessageUtil.info("  essentials     Essentials by nhulston (v1.8.0)", COLOR_GRAY));
    ctx.sendMessage(HEMessageUtil.info("  essplus        EssentialsPlus by fof1092 (v1.16.1)", COLOR_GRAY));
    ctx.sendMessage(HEMessageUtil.info("  hesx           hyessentialsx by thelegacyvoyage (v1.4.1)", COLOR_GRAY));
    ctx.sendMessage(HEMessageUtil.info("", COLOR_GRAY));
    ctx.sendMessage(HEMessageUtil.info(player, CommandKeys.Import.HELP_FLAGS_HEADER, COLOR_YELLOW));
    ctx.sendMessage(HEMessageUtil.info("  --dry-run / -n  " + "Simulate without changes", COLOR_GRAY));
    ctx.sendMessage(HEMessageUtil.info("  --overwrite     " + "Overwrite existing data", COLOR_GRAY));
  }
}
