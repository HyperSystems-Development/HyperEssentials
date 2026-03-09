package com.hyperessentials.module.kits;

import com.hyperessentials.HyperEssentials;
import com.hyperessentials.api.HyperEssentialsAPI;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.config.ModuleConfig;
import com.hyperessentials.module.AbstractModule;
import com.hyperessentials.module.kits.command.*;
import com.hyperessentials.platform.HyperEssentialsPlugin;
import com.hyperessentials.storage.KitStorage;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Kits module for HyperEssentials.
 * Provides admin-defined item kits with permission checks and cooldowns.
 */
public class KitsModule extends AbstractModule {

  private KitManager kitManager;

  @Override
  @NotNull
  public String getName() {
    return "kits";
  }

  @Override
  @NotNull
  public String getDisplayName() {
    return "Kits";
  }

  @Override
  public void onEnable() {
    super.onEnable();

    HyperEssentials core = HyperEssentialsAPI.getInstance();
    if (core == null) return;

    // Initialize manager with StorageProvider's KitStorage
    KitStorage kitStorage = core.getStorageProvider().getKitStorage();
    kitManager = new KitManager(kitStorage);
    kitManager.loadKits().join();

    // Register commands
    HyperEssentialsPlugin plugin = HyperEssentialsPlugin.getInstance();
    if (plugin != null) {
      try {
        plugin.getCommandRegistry().registerCommand(new KitCommand(this));
        plugin.getCommandRegistry().registerCommand(new KitsCommand(this));
        plugin.getCommandRegistry().registerCommand(new CreateKitCommand(this));
        plugin.getCommandRegistry().registerCommand(new DeleteKitCommand(this));
        plugin.getCommandRegistry().registerCommand(new PreviewKitCommand(this));
        Logger.info("[Kits] Registered commands: /kit, /kits, /createkit, /deletekit, /previewkit");
      } catch (Exception e) {
        Logger.severe("[Kits] Failed to register commands: %s", e.getMessage());
      }
    }
  }

  @Override
  public void onDisable() {
    if (kitManager != null) {
      kitManager.shutdown();
    }
    super.onDisable();
  }

  @NotNull
  public KitManager getKitManager() {
    return kitManager;
  }

  @Override
  @Nullable
  public ModuleConfig getModuleConfig() {
    return ConfigManager.get().kits();
  }
}
