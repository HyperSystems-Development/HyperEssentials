package com.hyperessentials.module.teleport.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.modules.TeleportConfig;
import com.hyperessentials.data.BackEntry;
import com.hyperessentials.data.Location;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.player.BackPage;
import com.hyperessentials.integration.FactionTerritoryChecker;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.warmup.WarmupManager;
import com.hyperessentials.module.warmup.WarmupTask;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * /back - Return to your previous location.
 * When backAllowSelectAny is enabled and multiple entries exist, opens a GUI.
 */
public class BackCommand extends AbstractPlayerCommand {

  private final BackManager backManager;
  private final WarmupManager warmupManager;

  public BackCommand(@NotNull BackManager backManager, @NotNull WarmupManager warmupManager) {
    super("back", "Return to your previous location");
    this.backManager = backManager;
    this.warmupManager = warmupManager;
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World currentWorld) {

    UUID uuid = playerRef.getUuid();

    if (!CommandUtil.hasPermission(uuid, Permissions.BACK)) {
      ctx.sendMessage(CommandUtil.error("You don't have permission to use /back."));
      return;
    }

    List<BackEntry> history = backManager.getBackHistory(uuid);
    if (history.isEmpty()) {
      ctx.sendMessage(CommandUtil.error("No back location found."));
      return;
    }

    // If backAllowSelectAny is enabled and >1 entry, try opening GUI
    TeleportConfig teleportConfig = ConfigManager.get().teleport();
    if (teleportConfig.isBackAllowSelectAny() && history.size() > 1) {
      if (tryOpenGui(store, ref, playerRef)) {
        return;
      }
      // GUI unavailable, fall through to text list + pop latest
    }

    // Default behavior: pop the most recent back location
    BackEntry backEntry = backManager.popBackEntry(uuid);
    if (backEntry == null) {
      ctx.sendMessage(CommandUtil.error("No back location found."));
      return;
    }

    Location destination = backEntry.location();

    // Territory/zone check on destination
    if (!CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS)
        && !CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS_BACK)) {
      FactionTerritoryChecker.Result result = FactionTerritoryChecker.canUseBack(
          uuid, destination.world(), destination.x(), destination.z());
      if (result != FactionTerritoryChecker.Result.ALLOWED) {
        String msg = switch (result) {
          case BLOCKED_OWN_TERRITORY -> "You cannot teleport back to your faction's territory.";
          case BLOCKED_ALLY_TERRITORY -> "You cannot teleport back to allied territory.";
          case BLOCKED_ENEMY_TERRITORY -> "You cannot teleport back to enemy territory.";
          case BLOCKED_NEUTRAL_TERRITORY -> "You cannot teleport back to neutral territory.";
          case BLOCKED_WILDERNESS -> "You cannot teleport back to the wilderness.";
          case BLOCKED_ZONE -> "You cannot teleport back to that zone.";
          default -> "You cannot teleport back to that location.";
        };
        ctx.sendMessage(CommandUtil.error(msg));
        return;
      }
    }

    if (warmupManager.isOnCooldown(uuid, "teleport", "back")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "teleport", "back");
      ctx.sendMessage(CommandUtil.error("On cooldown. " + remaining + "s remaining."));
      return;
    }

    WarmupTask task = warmupManager.startWarmup(uuid, "teleport", "back", () -> {
      executeTeleport(ref, destination, () -> {
        ctx.sendMessage(CommandUtil.success("Teleported to previous location!"));
      });
    });

    if (task != null) {
      ctx.sendMessage(CommandUtil.info("Teleporting in " + task.warmupSeconds() + "s... Don't move!"));
    }
  }

  private boolean tryOpenGui(@NotNull Store<EntityStore> store,
                              @NotNull Ref<EntityStore> ref,
                              @NotNull PlayerRef playerRef) {
    if (!HyperEssentialsAPI.isAvailable()) return false;

    Player player = store.getComponent(ref, Player.getComponentType());
    if (player == null) return false;

    GuiManager guiManager = HyperEssentialsAPI.getInstance().getGuiManager();
    BackPage backPage = new BackPage(player, playerRef, backManager, warmupManager, guiManager);
    player.getPageManager().openCustomPage(ref, store, backPage);
    return true;
  }

  private void executeTeleport(Ref<EntityStore> ref, Location dest, Runnable onComplete) {
    World targetWorld = Universe.get().getWorld(UUID.fromString(dest.worldUuid()));
    if (targetWorld == null) {
      return;
    }
    targetWorld.execute(() -> {
      if (!ref.isValid()) {
        return;
      }
      Store<EntityStore> store = ref.getStore();
      Vector3d position = new Vector3d(dest.x(), dest.y(), dest.z());
      Vector3f rotation = new Vector3f(dest.pitch(), dest.yaw(), 0);
      Teleport teleport = Teleport.createForPlayer(targetWorld, position, rotation);
      store.addComponent(ref, Teleport.getComponentType(), teleport);
      if (onComplete != null) {
        onComplete.run();
      }
    });
  }
}
