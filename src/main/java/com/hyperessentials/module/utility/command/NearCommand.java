package com.hyperessentials.module.utility.command;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.util.CommandKeys;
import com.hyperessentials.util.HEMessageUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /near [radius] - List nearby players.
 */
public class NearCommand extends AbstractPlayerCommand {

  public NearCommand() {
    super("near", "List nearby players");
    setAllowsExtraArguments(true);
  }

  @Override
  protected void execute(@NotNull CommandContext ctx,
              @NotNull Store<EntityStore> store,
              @NotNull Ref<EntityStore> ref,
              @NotNull PlayerRef playerRef,
              @NotNull World world) {
    if (!CommandUtil.hasPermission(playerRef.getUuid(), Permissions.UTILITY_NEAR)) {
      ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.NEAR_NO_PERMISSION));
      return;
    }

    int defaultRadius = ConfigManager.get().utility().getDefaultNearRadius();
    int maxRadius = ConfigManager.get().utility().getMaxNearRadius();

    String input = ctx.getInputString();
    String[] parts = input != null ? input.trim().split("\\s+") : new String[0];

    int radius = defaultRadius;
    if (parts.length >= 2) {
      try {
        radius = Integer.parseInt(parts[1]);
        if (radius < 1) radius = defaultRadius;
        if (radius > maxRadius) radius = maxRadius;
      } catch (NumberFormatException e) {
        ctx.sendMessage(HEMessageUtil.error(playerRef, CommandKeys.Utility.NEAR_INVALID_RADIUS, defaultRadius));
      }
    }

    Transform myTransform = playerRef.getTransform();
    Vector3d myPos = myTransform.getPosition();
    double px = myPos.getX();
    double py = myPos.getY();
    double pz = myPos.getZ();

    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin == null) return;

    List<String> nearby = new ArrayList<>();
    double radiusSq = (double) radius * radius;

    for (Map.Entry<UUID, PlayerRef> entry : plugin.getTrackedPlayers().entrySet()) {
      if (entry.getKey().equals(playerRef.getUuid())) continue;

      PlayerRef other = entry.getValue();
      Transform otherTransform = other.getTransform();
      Vector3d otherPos = otherTransform.getPosition();
      double dx = otherPos.getX() - px;
      double dy = otherPos.getY() - py;
      double dz = otherPos.getZ() - pz;
      double distSq = dx * dx + dy * dy + dz * dz;

      if (distSq <= radiusSq) {
        int dist = (int) Math.sqrt(distSq);
        nearby.add(other.getUsername() + " (" + dist + "m)");
      }
    }

    if (nearby.isEmpty()) {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Utility.NEAR_EMPTY, HEMessageUtil.COLOR_YELLOW, radius));
    } else {
      ctx.sendMessage(HEMessageUtil.info(playerRef, CommandKeys.Utility.NEAR_HEADER, HEMessageUtil.COLOR_YELLOW, nearby.size(), radius));
      for (String entry : nearby) {
        ctx.sendMessage(HEMessageUtil.text("  " + entry, HEMessageUtil.COLOR_GREEN));
      }
    }
  }
}
