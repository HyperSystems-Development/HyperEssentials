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
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Back.NO_PERMISSION));
      return;
    }

    List<BackEntry> history = backManager.getBackHistory(uuid);
    if (history.isEmpty()) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Back.NO_LOCATION));
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
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Back.NO_LOCATION));
      return;
    }

    Location destination = backEntry.location();

    // Territory/zone check on destination
    if (!CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS)
        && !CommandUtil.hasPermission(uuid, Permissions.BYPASS_FACTIONS_BACK)) {
      FactionTerritoryChecker.Result result = FactionTerritoryChecker.canUseBack(
          uuid, destination.world(), destination.x(), destination.z());
      if (result != FactionTerritoryChecker.Result.ALLOWED) {
        String key = switch (result) {
          case BLOCKED_OWN_TERRITORY -> CommandKeys.Back.BLOCKED_OWN;
          case BLOCKED_ALLY_TERRITORY -> CommandKeys.Back.BLOCKED_ALLY;
          case BLOCKED_ENEMY_TERRITORY -> CommandKeys.Back.BLOCKED_ENEMY;
          case BLOCKED_NEUTRAL_TERRITORY -> CommandKeys.Back.BLOCKED_NEUTRAL;
          case BLOCKED_WILDERNESS -> CommandKeys.Back.BLOCKED_WILDERNESS;
          case BLOCKED_ZONE -> CommandKeys.Back.BLOCKED_ZONE;
          default -> CommandKeys.Back.BLOCKED_GENERIC;
        };
        ctx.sendMessage(HEMessageUtil.error(playerRef, key));
        return;
      }
    }

    if (warmupManager.isOnCooldown(uuid, "teleport", "back")) {
      int remaining = warmupManager.getRemainingCooldown(uuid, "teleport", "back");
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Common.ON_COOLDOWN, remaining));
      return;
    }

    WarmupTask task = warmupManager.startWarmup(uuid, "teleport", "back", () -> {
      executeTeleport(ref, destination, () -> {
        ctx.sendMessage(HEMessageUtil.success(playerRef, CommandKeys.Back.TELEPORTED));
      });
    });

    if (task != null) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Common.WARMUP_STARTING, HEMessageUtil.COLOR_YELLOW, task.warmupSeconds()));
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
