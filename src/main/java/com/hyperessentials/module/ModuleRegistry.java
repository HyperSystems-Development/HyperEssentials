package com.hyperessentials.module;

import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and lifecycle manager for all modules.
 */
public class ModuleRegistry {

  private final Map<String, Module> modules = new ConcurrentHashMap<>();
  private final List<String> enableOrder = new ArrayList<>();

  /**
   * Registers a module.
   */
  public void register(@NotNull Module module) {
    modules.put(module.getName(), module);
    enableOrder.add(module.getName());
    Logger.debug("[ModuleRegistry] Registered module: %s", module.getName());
  }

  /**
   * Enables all modules that are configured as enabled.
   */
  public void enableAll() {
    ConfigManager config = ConfigManager.get();
    int enabled = 0;

    for (String name : enableOrder) {
      Module module = modules.get(name);
      if (module != null && config.isModuleEnabled(name)) {
        try {
          module.onEnable();
          enabled++;
        } catch (Exception e) {
          Logger.severe("[ModuleRegistry] Failed to enable module %s: %s", name, e.getMessage());
        }
      }
    }

    Logger.info("[ModuleRegistry] Enabled %d/%d modules", enabled, modules.size());
  }

  /**
   * Disables all enabled modules in reverse order.
   */
  public void disableAll() {
    List<String> reversed = new ArrayList<>(enableOrder);
    Collections.reverse(reversed);

    for (String name : reversed) {
      Module module = modules.get(name);
      if (module != null && module.isEnabled()) {
        try {
          module.onDisable();
        } catch (Exception e) {
          Logger.severe("[ModuleRegistry] Failed to disable module %s: %s", name, e.getMessage());
        }
      }
    }
  }

  /**
   * Gets a module by name.
   */
  @Nullable
  public Module getModule(@NotNull String name) {
    return modules.get(name);
  }

  /**
   * Gets a module by class.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <T extends Module> T getModule(@NotNull Class<T> clazz) {
    for (Module module : modules.values()) {
      if (clazz.isInstance(module)) {
        return (T) module;
      }
    }
    return null;
  }

  /**
   * Gets all registered modules.
   */
  @NotNull
  public Collection<Module> getModules() {
    return Collections.unmodifiableCollection(modules.values());
  }

  /**
   * Gets all enabled modules.
   */
  @NotNull
  public List<Module> getEnabledModules() {
    return modules.values().stream()
        .filter(Module::isEnabled)
        .toList();
  }
}
