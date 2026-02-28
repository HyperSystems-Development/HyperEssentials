package com.hyperessentials.module.spawns;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.config.modules.SpawnsConfig;
import com.hyperessentials.module.AbstractModule;
import com.hyperessentials.storage.SpawnStorage;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Spawns module for HyperEssentials.
 */
public class SpawnsModule extends AbstractModule {

  private SpawnManager spawnManager;

  @Override
  @NotNull
  public String getName() {
    return "spawns";
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return "Spawns";
  }

  @Override
  public void onEnable() {
    super.onEnable();
  }

  public void initManager(@NotNull SpawnStorage storage) {
    SpawnsConfig config = ConfigManager.get().spawns();
    this.spawnManager = new SpawnManager(storage, config);
    spawnManager.loadSpawns().join();
    Logger.info("[Spawns] SpawnManager initialized");
  }

  @Override
  public void onDisable() {
    if (spawnManager != null) {
      spawnManager.saveSpawns().join();
    }
    super.onDisable();
  }

  @Nullable
  public SpawnManager getSpawnManager() {
    return spawnManager;
  }

  @Override
  @Nullable
  public ModuleConfig getModuleConfig() {
    return ConfigManager.get().spawns();
  }
}
