package com.hyperessentials.gui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.Nullable;

/**
 * Functional interface for creating GUI page instances.
 */
@FunctionalInterface
public interface PageSupplier {

  /**
   * Creates a new page instance.
   *
   * @param player    The player entity
   * @param ref       Entity reference
   * @param store     Entity store
   * @param playerRef Player reference
   * @param guiManager The GUI manager
   * @return The created page, or null if page cannot be created
   */
  @Nullable
  InteractiveCustomUIPage<?> create(
      Player player,
      Ref<EntityStore> ref,
      Store<EntityStore> store,
      PlayerRef playerRef,
      GuiManager guiManager
  );
}
