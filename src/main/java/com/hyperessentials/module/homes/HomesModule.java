package com.hyperessentials.module.homes;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.module.AbstractModule;
import com.hyperessentials.storage.HomeStorage;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Homes module for HyperEssentials.
 */
public class HomesModule extends AbstractModule {

  private HomeManager homeManager;

  @Override
  @NotNull
  public String getName() {
    return "homes";
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return "Homes";
  }

  @Override
  public void onEnable() {
    super.onEnable();
  }

  /**
   * Initializes the home manager with the given storage.
   * Called by HyperEssentials after module is enabled.
   */
  public void initManager(@NotNull HomeStorage storage) {
    this.homeManager = new HomeManager(storage);
    Logger.info("[Homes] HomeManager initialized");
  }

  @Override
  public void onDisable() {
    if (homeManager != null) {
      homeManager.saveAll().join();
    }
    super.onDisable();
  }

  @Nullable
  public HomeManager getHomeManager() {
    return homeManager;
  }

  @Override
  @Nullable
  public ModuleConfig getModuleConfig() {
    return ConfigManager.get().homes();
  }
}
