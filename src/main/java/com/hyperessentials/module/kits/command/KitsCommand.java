package com.hyperessentials.module.kits.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.module.kits.KitsModule;
import com.hyperessentials.module.kits.data.Kit;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * /kits - List available kits.
 * Opens GUI page when available, falls back to text list.
 */
public class KitsCommand extends AbstractPlayerCommand {

  private final KitsModule module;

  public KitsCommand(@NotNull KitsModule module) {
    super("kits", "List available kits");
    this.module = module;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.KIT_LIST)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to list kits."));
      return;
    }

    // Try GUI page first
    if (tryOpenGui(store, ref, playerRef)) {
      return;
    }

    // Text fallback
    List<Kit> available = module.getKitManager().getAvailableKits(playerRef.getUuid());

    if (available.isEmpty()) {
      ctx.sendMessage(CommandUtil.info("No kits available."));
      return;
    }

    ctx.sendMessage(CommandUtil.info("Available Kits (" + available.size() + "):"));
    for (Kit kit : available) {
      String cooldownInfo = kit.cooldownSeconds() > 0
        ? " (cooldown: " + kit.cooldownSeconds() + "s)"
        : "";
      String oneTimeInfo = kit.oneTime() ? " [one-time]" : "";

      Message line = CommandUtil.prefix()
        .insert(Message.raw("  " + kit.displayName()).color(CommandUtil.COLOR_GREEN))
        .insert(Message.raw(cooldownInfo + oneTimeInfo).color(CommandUtil.COLOR_GRAY));
      ctx.sendMessage(line);
    }
  }

  private boolean tryOpenGui(@NotNull Store<EntityStore> store,
                              @NotNull Ref<EntityStore> ref,
                              @NotNull PlayerRef playerRef) {
    if (!HyperEssentialsAPI.isAvailable()) return false;

    GuiManager guiManager = HyperEssentialsAPI.getInstance().getGuiManager();
    if (guiManager.getPlayerRegistry().getEntry("kits") == null) return false;

    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) return false;

    return guiManager.openPlayerPage("kits", player, ref, store, playerRef);
  }
}
