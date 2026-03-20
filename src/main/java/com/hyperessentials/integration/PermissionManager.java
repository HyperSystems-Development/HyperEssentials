package com.hyperessentials.integration;

import com.hyperessentials.Permissions;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Unified permission manager with chain-of-responsibility pattern.
 */
public class PermissionManager {

  private static final PermissionManager INSTANCE = new PermissionManager();

  private final List<PermissionProvider> providers = new ArrayList<>();
  private @Nullable HyperPermsProviderAdapter hyperPermsAdapter;
  private boolean initialized = false;

  private PermissionManager() {}

  public static PermissionManager get() {
    return INSTANCE;
  }

  public void init() {
    if (initialized) return;
    providers.clear();

    HyperPermsProviderAdapter hyperPermsProvider = new HyperPermsProviderAdapter();
    hyperPermsProvider.init();
    if (hyperPermsProvider.isAvailable()) {
      providers.add(hyperPermsProvider);
      hyperPermsAdapter = hyperPermsProvider;
    }

    initialized = true;

    if (providers.isEmpty()) {
      Logger.info("[PermissionManager] No permission providers found - using fallback mode");
    } else {
      Logger.info("[PermissionManager] Initialized with %d provider(s): %s",
        providers.size(), getProviderNames());
    }
  }

  /**
   * Gets the HyperPerms provider adapter, if available.
   * Used by admin GUI pages for group permission management.
   *
   * @return the adapter, or null if HyperPerms is not installed
   */
  @Nullable
  public HyperPermsProviderAdapter getHyperPermsAdapter() {
    return hyperPermsAdapter;
  }

  public boolean hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
    boolean isUserLevel = isUserLevelPermission(permission);

    for (PermissionProvider provider : providers) {
      Optional<Boolean> result = provider.hasPermission(playerUuid, permission);
      if (result.isPresent()) {
        if (result.get()) return true;
        if (!isUserLevel) return false;
        break;
      }
    }

    String categoryWildcard = getCategoryWildcard(permission);
    if (categoryWildcard != null) {
      for (PermissionProvider provider : providers) {
        Optional<Boolean> result = provider.hasPermission(playerUuid, categoryWildcard);
        if (result.isPresent() && result.get()) return true;
      }
    }

    for (PermissionProvider provider : providers) {
      Optional<Boolean> result = provider.hasPermission(playerUuid, Permissions.ROOT + ".*");
      if (result.isPresent() && result.get()) return true;
    }

    return handleFallback(playerUuid, permission);
  }

  public int getPermissionValue(@NotNull UUID playerUuid, @NotNull String prefix, int defaultValue) {
    for (PermissionProvider provider : providers) {
      int value = provider.getPermissionValue(playerUuid, prefix, Integer.MIN_VALUE);
      if (value != Integer.MIN_VALUE) return value;
    }
    return defaultValue;
  }

  @NotNull
  public String getPrimaryGroup(@NotNull UUID playerUuid) {
    for (PermissionProvider provider : providers) {
      String group = provider.getPrimaryGroup(playerUuid);
      if (group != null && !group.isEmpty() && !"default".equals(group)) return group;
    }
    return "default";
  }

  private String getCategoryWildcard(@NotNull String permission) {
    if (!permission.startsWith(Permissions.ROOT + ".")) return null;
    int lastDot = permission.lastIndexOf('.');
    if (lastDot <= Permissions.ROOT.length()) return null;
    return permission.substring(0, lastDot) + ".*";
  }

  private boolean isUserLevelPermission(@NotNull String permission) {
    if (permission.startsWith(Permissions.ADMIN)) return false;
    if (permission.startsWith(Permissions.BYPASS)) return false;
    return permission.startsWith(Permissions.ROOT + ".");
  }

  private boolean handleFallback(@NotNull UUID playerUuid, @NotNull String permission) {
    ConfigManager config = ConfigManager.get();

    if (permission.startsWith(Permissions.ADMIN)) {
      return isPlayerOp(playerUuid);
    }
    if (permission.startsWith(Permissions.BYPASS)) {
      return false;
    }

    return config.core().isAllowWithoutPermissionMod();
  }

  private boolean isPlayerOp(@NotNull UUID playerUuid) {
    try {
      Class<?> permModuleClass = Class.forName("com.hypixel.hytale.server.core.permissions.PermissionsModule");
      java.lang.reflect.Method getMethod = permModuleClass.getMethod("get");
      Object permModule = getMethod.invoke(null);
      if (permModule == null) return false;

      java.lang.reflect.Method getGroupsMethod = permModuleClass.getMethod("getGroupsForUser", UUID.class);
      Object groupsObj = getGroupsMethod.invoke(permModule, playerUuid);
      if (groupsObj instanceof java.util.Set<?> groups) {
        if (groups.contains("OP")) return true;
      }

      java.lang.reflect.Method hasPermMethod = permModuleClass.getMethod("hasPermission", UUID.class, String.class);
      Object result = hasPermMethod.invoke(permModule, playerUuid, "*");
      if (result instanceof Boolean) return (Boolean) result;
    } catch (Exception e) {
      Logger.debug("[PermissionManager] Error checking OP status: %s", e.getMessage());
    }
    return false;
  }

  @NotNull
  public String getProviderNames() {
    if (providers.isEmpty()) return "none";
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < providers.size(); i++) {
      if (i > 0) sb.append(", ");
      sb.append(providers.get(i).getName());
    }
    return sb.toString();
  }

  public boolean hasProviders() {
    return !providers.isEmpty();
  }
}
