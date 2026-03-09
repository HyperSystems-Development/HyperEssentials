package com.hyperessentials.ecs;

import com.hyperessentials.HyperEssentials;
import com.hyperessentials.data.Location;
import com.hyperessentials.module.teleport.BackManager;
import com.hyperessentials.module.teleport.TeleportModule;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * ECS system that detects player death via DeathComponent addition.
 * Saves the death location to back history for /back support.
 */
public class PlayerDeathTrackingSystem extends RefChangeSystem<EntityStore, DeathComponent> {

  private final HyperEssentials hyperEssentials;

  public PlayerDeathTrackingSystem(@NotNull HyperEssentials hyperEssentials) {
    this.hyperEssentials = hyperEssentials;
  }

  @NotNull
  @Override
  public ComponentType<EntityStore, DeathComponent> componentType() {
    return DeathComponent.getComponentType();
  }

  @NotNull
  @Override
  public Query<EntityStore> getQuery() {
    return Player.getComponentType();
  }

  @Override
  public void onComponentAdded(@NotNull Ref<EntityStore> ref,
                 @NotNull DeathComponent component,
                 @NotNull Store<EntityStore> store,
                 @NotNull CommandBuffer<EntityStore> commandBuffer) {
    try {
      PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
      if (playerRef == null) return;

      TransformComponent transform = commandBuffer.getComponent(ref, TransformComponent.getComponentType());
      if (transform == null) return;

      TeleportModule tm = hyperEssentials.getTeleportModule();
      if (tm == null || !tm.isEnabled()) return;

      BackManager backManager = tm.getBackManager();
      if (backManager == null) return;

      UUID uuid = playerRef.getUuid();
      Vector3d pos = transform.getPosition();
      World world = store.getExternalData().getWorld();

      Location deathLocation = new Location(world.getName(),
          world.getWorldConfig().getUuid().toString(),
          pos.getX(), pos.getY(), pos.getZ(), 0, 0);

      backManager.onDeath(uuid, deathLocation);
      Logger.debug("[Death] Saved death location for %s", playerRef.getUsername());
    } catch (Exception e) {
      Logger.debug("[Death] Failed to save death location: %s", e.getMessage());
    }
  }

  @Override
  public void onComponentSet(@NotNull Ref<EntityStore> ref,
                @Nullable DeathComponent oldComponent,
                @NotNull DeathComponent newComponent,
                @NotNull Store<EntityStore> store,
                @NotNull CommandBuffer<EntityStore> commandBuffer) {
    // Not used for death tracking
  }

  @Override
  public void onComponentRemoved(@NotNull Ref<EntityStore> ref,
                  @NotNull DeathComponent component,
                  @NotNull Store<EntityStore> store,
                  @NotNull CommandBuffer<EntityStore> commandBuffer) {
    // Not used for death tracking
  }
}
