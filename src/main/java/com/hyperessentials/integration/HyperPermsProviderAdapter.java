package com.hyperessentials.integration;

import com.hyperessentials.util.ErrorHandler;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter wrapping HyperPerms as a PermissionProvider.
 * Uses reflection to avoid hard dependency.
 */
public class HyperPermsProviderAdapter implements PermissionProvider {

  private boolean available = false;
  private Object hyperPermsInstance = null;
  private Method hasPermissionMethod = null;
  private Method getUserManagerMethod = null;
  private Method getGroupManagerMethod = null;

  @Override
  @NotNull
  public String getName() {
    return "HyperPerms";
  }

  public void init() {
    try {
      Class<?> bootstrapClass = Class.forName("com.hyperperms.HyperPermsBootstrap");
      Method getInstanceMethod = bootstrapClass.getMethod("getInstance");
      hyperPermsInstance = getInstanceMethod.invoke(null);

      if (hyperPermsInstance == null) {
        available = false;
        return;
      }

      Class<?> instanceClass = hyperPermsInstance.getClass();
      hasPermissionMethod = instanceClass.getMethod("hasPermission", UUID.class, String.class);
      getUserManagerMethod = instanceClass.getMethod("getUserManager");
      getGroupManagerMethod = instanceClass.getMethod("getGroupManager");

      available = true;
      Logger.info("[PermissionManager] HyperPerms provider initialized");

    } catch (ClassNotFoundException e) {
      available = false;
      Logger.debug("[HyperPermsProvider] HyperPerms not found");
    } catch (Exception e) {
      available = false;
      ErrorHandler.report("[HyperPermsProvider] Failed to initialize", e);
    }
  }

  @Override
  public boolean isAvailable() {
    return available;
  }

  @Override
  @NotNull
  public Optional<Boolean> hasPermission(@NotNull UUID playerUuid, @NotNull String permission) {
    if (!available || hyperPermsInstance == null || hasPermissionMethod == null) {
      return Optional.empty();
    }
    try {
      Object result = hasPermissionMethod.invoke(hyperPermsInstance, playerUuid, permission);
      if (result instanceof Boolean) {
        return Optional.of((Boolean) result);
      }
      return Optional.empty();
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public int getPermissionValue(@NotNull UUID playerUuid, @NotNull String prefix, int defaultValue) {
    if (!available || hyperPermsInstance == null || getUserManagerMethod == null) {
      return defaultValue;
    }
    try {
      Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
      if (userManager == null) return defaultValue;

      Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
      Object user = getUserMethod.invoke(userManager, playerUuid);
      if (user == null) return defaultValue;

      Method getPermissionsMethod = user.getClass().getMethod("getEffectivePermissions");
      Object permissions = getPermissionsMethod.invoke(user);

      if (permissions instanceof Iterable<?> iterable) {
        int highestValue = defaultValue;
        for (Object perm : iterable) {
          String permStr = perm.toString();
          if (permStr.startsWith(prefix)) {
            try {
              int value = Integer.parseInt(permStr.substring(prefix.length()));
              if (value > highestValue) highestValue = value;
            } catch (NumberFormatException ignored) {}
          }
        }
        return highestValue;
      }
    } catch (Exception e) {
      ErrorHandler.report("[HyperPermsProvider] Failed to get permission value", e);
    }
    return defaultValue;
  }

  @Override
  @NotNull
  public String getPrimaryGroup(@NotNull UUID playerUuid) {
    if (!available || hyperPermsInstance == null || getUserManagerMethod == null) {
      return "default";
    }
    try {
      Object userManager = getUserManagerMethod.invoke(hyperPermsInstance);
      if (userManager == null) return "default";

      Method getUserMethod = userManager.getClass().getMethod("getUser", UUID.class);
      Object user = getUserMethod.invoke(userManager, playerUuid);
      if (user == null) return "default";

      Method getPrimaryGroupMethod = user.getClass().getMethod("getPrimaryGroup");
      Object result = getPrimaryGroupMethod.invoke(user);
      return result != null ? result.toString() : "default";
    } catch (Exception e) {
      return "default";
    }
  }

  // ==================== Group Permission Management ====================

  /**
   * Gets all available group names from HyperPerms.
   *
   * @return sorted list of group names, or empty list if unavailable
   */
  @NotNull
  public List<String> getGroupNames() {
    if (!available || hyperPermsInstance == null || getGroupManagerMethod == null) {
      return Collections.emptyList();
    }
    try {
      Object groupManager = getGroupManagerMethod.invoke(hyperPermsInstance);
      if (groupManager == null) return Collections.emptyList();

      Method getGroupNamesMethod = groupManager.getClass().getMethod("getGroupNames");
      Object result = getGroupNamesMethod.invoke(groupManager);

      if (result instanceof Set<?> names) {
        List<String> sorted = new ArrayList<>();
        for (Object name : names) {
          sorted.add(name.toString());
        }
        Collections.sort(sorted);
        return sorted;
      }
      return Collections.emptyList();
    } catch (Exception e) {
      ErrorHandler.report("[HyperPermsProvider] Failed to get group names", e);
      return Collections.emptyList();
    }
  }

  /**
   * Checks if a group has a specific permission node.
   *
   * @param groupName  the group name
   * @param permission the permission node
   * @return true if the group has the permission directly assigned
   */
  public boolean groupHasPermission(@NotNull String groupName, @NotNull String permission) {
    if (!available || hyperPermsInstance == null || getGroupManagerMethod == null) {
      return false;
    }
    try {
      Object groupManager = getGroupManagerMethod.invoke(hyperPermsInstance);
      if (groupManager == null) return false;

      Method getGroupMethod = groupManager.getClass().getMethod("getGroup", String.class);
      Object group = getGroupMethod.invoke(groupManager, groupName);
      if (group == null) return false;

      // Get the group's nodes and check for a matching permission
      Method getNodesMethod = group.getClass().getMethod("getNodes");
      Object nodesObj = getNodesMethod.invoke(group);

      if (nodesObj instanceof Iterable<?> nodes) {
        String lowerPerm = permission.toLowerCase();
        for (Object node : nodes) {
          Method getPermMethod = node.getClass().getMethod("getPermission");
          Method getValueMethod = node.getClass().getMethod("getValue");
          Method isGroupNodeMethod = node.getClass().getMethod("isGroupNode");

          boolean isGroupNode = (boolean) isGroupNodeMethod.invoke(node);
          if (isGroupNode) continue;

          String nodePerm = (String) getPermMethod.invoke(node);
          boolean nodeValue = (boolean) getValueMethod.invoke(node);

          if (nodePerm.equals(lowerPerm) && nodeValue) {
            return true;
          }
        }
      }
      return false;
    } catch (Exception e) {
      ErrorHandler.report("[HyperPermsProvider] Failed to check group permission", e);
      return false;
    }
  }

  /**
   * Adds a permission node to a group via HyperPerms.
   *
   * @param groupName  the group name
   * @param permission the permission node to add
   * @return true if successfully added
   */
  public boolean addPermissionToGroup(@NotNull String groupName, @NotNull String permission) {
    if (!available || hyperPermsInstance == null || getGroupManagerMethod == null) {
      return false;
    }
    try {
      Object groupManager = getGroupManagerMethod.invoke(hyperPermsInstance);
      if (groupManager == null) return false;

      // Use modifyGroup(name, action) for thread-safe mutation + auto-save
      // Since we cannot pass a Consumer via reflection easily, use getGroup + addNode + saveGroup
      Method getGroupMethod = groupManager.getClass().getMethod("getGroup", String.class);
      Object group = getGroupMethod.invoke(groupManager, groupName);
      if (group == null) return false;

      // Build a Node: Node.builder(permission).build()
      Class<?> nodeClass = Class.forName("com.hyperperms.model.Node");
      Method builderMethod = nodeClass.getMethod("builder", String.class);
      Object builder = builderMethod.invoke(null, permission);
      Method buildMethod = builder.getClass().getMethod("build");
      Object node = buildMethod.invoke(builder);

      // Add the node
      Method addNodeMethod = group.getClass().getMethod("addNode", nodeClass);
      Object result = addNodeMethod.invoke(group, node);

      // Save the group
      Method saveGroupMethod = groupManager.getClass().getMethod("saveGroup", group.getClass());
      saveGroupMethod.invoke(groupManager, group);

      Logger.info("[HyperPermsProvider] Added permission '%s' to group '%s'", permission, groupName);
      return true;
    } catch (Exception e) {
      ErrorHandler.report("[HyperPermsProvider] Failed to add permission to group", e);
      return false;
    }
  }

  /**
   * Removes a permission node from a group via HyperPerms.
   *
   * @param groupName  the group name
   * @param permission the permission node to remove
   * @return true if successfully removed
   */
  public boolean removePermissionFromGroup(@NotNull String groupName, @NotNull String permission) {
    if (!available || hyperPermsInstance == null || getGroupManagerMethod == null) {
      return false;
    }
    try {
      Object groupManager = getGroupManagerMethod.invoke(hyperPermsInstance);
      if (groupManager == null) return false;

      Method getGroupMethod = groupManager.getClass().getMethod("getGroup", String.class);
      Object group = getGroupMethod.invoke(groupManager, groupName);
      if (group == null) return false;

      // Use removeNode(String) overload
      Method removeNodeMethod = group.getClass().getMethod("removeNode", String.class);
      removeNodeMethod.invoke(group, permission);

      // Save the group
      Method saveGroupMethod = groupManager.getClass().getMethod("saveGroup", group.getClass());
      saveGroupMethod.invoke(groupManager, group);

      Logger.info("[HyperPermsProvider] Removed permission '%s' from group '%s'", permission, groupName);
      return true;
    } catch (Exception e) {
      ErrorHandler.report("[HyperPermsProvider] Failed to remove permission from group", e);
      return false;
    }
  }
}
