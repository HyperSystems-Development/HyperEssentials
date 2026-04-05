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
    this.spawnManager = new SpawnManager(storage);
    spawnManager.loadSpawns().join();

    // On first startup (no spawns configured), auto-detect from server world configs
    spawnManager.autoDetectWorldSpawns();

    // Ensure a global spawn always exists
    spawnManager.ensureGlobalSpawn();

    Logger.info("[Spawns] SpawnManager initialized");
  }

  @Override
  public void onDisable() {
    // Spawns are saved individually on each create/update/delete — no bulk save needed
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
