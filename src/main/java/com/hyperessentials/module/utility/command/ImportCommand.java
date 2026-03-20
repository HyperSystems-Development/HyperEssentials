package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.importer.AdminImportHandler;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

/**
 * /essimport &lt;source&gt; [path] [--dry-run] [--overwrite]
 *
 * <p>Admin command to import data from other essentials-type plugins.
 * Delegates to {@link AdminImportHandler} for source resolution and execution.
 *
 * <p>Permission: {@code hyperessentials.admin.import}
 */
public class ImportCommand extends AbstractPlayerCommand {

  private final AdminImportHandler importHandler;

  public ImportCommand(@NotNull AdminImportHandler importHandler) {
    super("essimport", "Import data from other plugins");
    setAllowsExtraArguments(true);
    this.importHandler = importHandler;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
                         @NotNull Store<EntityStore> store,
                         @NotNull Ref<EntityStore> ref,
                         @NotNull PlayerRef playerRef,
                         @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.ADMIN_IMPORT)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.NO_PERMISSION));
      return;
    }

    // Parse arguments after command name
    String input = ctx.getInputString();
    String[] args = new String[0];
    if (input != null && !input.isBlank()) {
      String[] parts = input.trim().split("\\s+");
      if (parts.length > 1) {
        args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, parts.length - 1);
      }
    }

    importHandler.handleImport(ctx, playerRef, args);
  }
}
