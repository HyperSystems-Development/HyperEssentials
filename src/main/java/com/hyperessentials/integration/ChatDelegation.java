package com.hyperessentials.integration;

import com.hyperessentials.util.Logger;

/**
 * Coordinates chat handling across available plugins.
 *
 * <p>Priority order: Werchat (full chat system) > HyperPerms (prefix/suffix only) > built-in.
 * When a higher-priority provider is available, HyperEssentials defers to it
 * rather than duplicating functionality.
 */
public final class ChatDelegation {

  /** The active chat provider. */
  public enum ChatProvider {
    /** Werchat handles all chat: formatting, channels, nicknames, moderation. */
    WERCHAT,
    /** HyperPerms provides prefix/suffix formatting; HyperEssentials handles the rest. */
    HYPERPERMS,
    /** No external chat plugin — HyperEssentials uses its own built-in formatting. */
    BUILTIN
  }

  private static ChatProvider activeProvider = ChatProvider.BUILTIN;

  private ChatDelegation() {}

  /**
   * Determines the active chat provider based on available integrations.
   * Must be called after {@link WerchatIntegration#init()} and
   * {@link PermissionManager#init()}.
   */
  public static void init() {
    if (WerchatIntegration.isAvailable()) {
      activeProvider = ChatProvider.WERCHAT;
      Logger.info("[ChatDelegation] Chat delegated to Werchat");
    } else if (PermissionManager.get().hasProviders()) {
      activeProvider = ChatProvider.HYPERPERMS;
      Logger.info("[ChatDelegation] Chat prefix/suffix delegated to HyperPerms");
    } else {
      activeProvider = ChatProvider.BUILTIN;
      Logger.info("[ChatDelegation] Using built-in chat formatting");
    }
  }

  /** Returns the currently active chat provider. */
  public static ChatProvider getActiveProvider() {
    return activeProvider;
  }

  /**
   * Returns true if HyperEssentials should handle chat events itself.
   * When Werchat or another full chat plugin is active, this returns false.
   */
  public static boolean shouldHandleChat() {
    return activeProvider == ChatProvider.BUILTIN;
  }

  /**
   * Returns true if HyperEssentials should apply prefix/suffix formatting.
   * False when Werchat is active (it handles its own formatting).
   */
  public static boolean shouldFormatPrefix() {
    return activeProvider != ChatProvider.WERCHAT;
  }
}
