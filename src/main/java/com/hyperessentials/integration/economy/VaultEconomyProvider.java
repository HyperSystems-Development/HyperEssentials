package com.hyperessentials.integration.economy;

import com.hyperessentials.util.Logger;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Economy provider for VaultUnlocked (Vault2 Economy API).
 * Uses reflection to integrate with VaultUnlocked's Economy interface.
 *
 * <p>Follows lazy-init + permanentFailure pattern: safe to call init() multiple
 * times. ClassNotFoundException means VaultUnlocked is not installed (permanent).
 *
 * <p>Actual class paths (VaultUnlocked-Hytale 2.19.0):
 * <ul>
 *   <li>Main: net.cfh.vault.VaultUnlocked</li>
 *   <li>Economy: net.milkbowl.vault2.economy.Economy</li>
 *   <li>EconomyResponse: net.milkbowl.vault2.economy.EconomyResponse</li>
 * </ul>
 */
public class VaultEconomyProvider {

  private static final String PLUGIN_NAME = "HyperEssentials";

  private volatile boolean available = false;
  private volatile boolean permanentFailure = false;

  // Reflection references
  private Object economyService = null;
  private Method getBalanceMethod = null;
  private Method hasMethod = null;
  private Method withdrawMethod = null;
  private Method depositMethod = null;
  private Method transactionSuccessMethod = null;

  /**
   * Initializes the VaultUnlocked economy provider.
   * Safe to call multiple times.
   */
  public void init() {
    if (available || permanentFailure) {
      return;
    }

    try {
      Class<?> vaultUnlockedClass = Class.forName("net.cfh.vault.VaultUnlocked");

      Method economyMethod = vaultUnlockedClass.getMethod("economy");
      Object econOptional = economyMethod.invoke(null);
      if (econOptional instanceof Optional<?> opt) {
        economyService = opt.orElse(null);
      }

      if (economyService == null) {
        Logger.debug("[VaultEconomyProvider] Economy service not available yet");
        return;
      }

      Class<?> economyClass = Class.forName("net.milkbowl.vault2.economy.Economy");
      getBalanceMethod = economyClass.getMethod("getBalance", String.class, UUID.class);
      hasMethod = economyClass.getMethod("has", String.class, UUID.class, BigDecimal.class);
      withdrawMethod = economyClass.getMethod("withdraw", String.class, UUID.class, BigDecimal.class);
      depositMethod = economyClass.getMethod("deposit", String.class, UUID.class, BigDecimal.class);

      Class<?> responseClass = Class.forName("net.milkbowl.vault2.economy.EconomyResponse");
      transactionSuccessMethod = responseClass.getMethod("transactionSuccess");

      available = true;
      Logger.info("[Integration] VaultUnlocked economy provider initialized");

    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      permanentFailure = true;
      Logger.debug("[VaultEconomyProvider] VaultUnlocked not installed");
    } catch (Exception e) {
      Logger.debug("[VaultEconomyProvider] Init deferred: %s", e.getMessage());
    }
  }

  private void ensureInitialized() {
    if (!available && !permanentFailure) {
      init();
    }
  }

  /** Checks if the economy provider is available. */
  public boolean isAvailable() {
    ensureInitialized();
    return available;
  }

  /** Checks if initialization permanently failed (VaultUnlocked not installed). */
  public boolean isPermanentFailure() {
    return permanentFailure;
  }

  /** Gets the name of the economy plugin registered with VaultUnlocked. */
  @Nullable
  public String getEconomyName() {
    ensureInitialized();
    if (!available || economyService == null) {
      return null;
    }
    try {
      Method getNameMethod = economyService.getClass().getMethod("getName");
      Object result = getNameMethod.invoke(economyService);
      return result instanceof String s ? s : null;
    } catch (Exception e) {
      Logger.debug("[VaultEconomyProvider] Could not get economy name: %s", e.getMessage());
      return null;
    }
  }

  /** Gets a player's balance as BigDecimal. */
  @NotNull
  public BigDecimal getBalanceBigDecimal(@NotNull UUID playerUuid) {
    ensureInitialized();
    if (!available || economyService == null || getBalanceMethod == null) {
      return BigDecimal.ZERO;
    }
    try {
      Object result = getBalanceMethod.invoke(economyService, PLUGIN_NAME, playerUuid);
      if (result instanceof BigDecimal bd) {
        return bd;
      }
      return BigDecimal.ZERO;
    } catch (Exception e) {
      Logger.debug("[VaultEconomyProvider] Exception getting balance: %s", e.getMessage());
      return BigDecimal.ZERO;
    }
  }

  /** Gets a player's balance as double. */
  public double getBalance(@NotNull UUID playerUuid) {
    return getBalanceBigDecimal(playerUuid).doubleValue();
  }

  /** Checks if a player has at least the specified amount. */
  public boolean has(@NotNull UUID playerUuid, @NotNull BigDecimal amount) {
    ensureInitialized();
    if (!available || economyService == null || hasMethod == null) {
      return false;
    }
    try {
      Object result = hasMethod.invoke(economyService, PLUGIN_NAME, playerUuid, amount);
      return result instanceof Boolean b && b;
    } catch (Exception e) {
      Logger.debug("[VaultEconomyProvider] Exception checking has: %s", e.getMessage());
      return false;
    }
  }

  /** Checks if a player has at least the specified amount (double overload). */
  public boolean has(@NotNull UUID playerUuid, double amount) {
    return has(playerUuid, BigDecimal.valueOf(amount));
  }

  /** Withdraws money from a player's account. */
  public boolean withdraw(@NotNull UUID playerUuid, @NotNull BigDecimal amount) {
    ensureInitialized();
    if (!available || economyService == null || withdrawMethod == null) {
      return false;
    }
    try {
      Object response = withdrawMethod.invoke(economyService, PLUGIN_NAME, playerUuid, amount);
      return isSuccess(response);
    } catch (Exception e) {
      Logger.debug("[VaultEconomyProvider] Exception withdrawing: %s", e.getMessage());
      return false;
    }
  }

  /** Withdraws money from a player's account (double overload). */
  public boolean withdraw(@NotNull UUID playerUuid, double amount) {
    return withdraw(playerUuid, BigDecimal.valueOf(amount));
  }

  /** Deposits money into a player's account. */
  public boolean deposit(@NotNull UUID playerUuid, @NotNull BigDecimal amount) {
    ensureInitialized();
    if (!available || economyService == null || depositMethod == null) {
      return false;
    }
    try {
      Object response = depositMethod.invoke(economyService, PLUGIN_NAME, playerUuid, amount);
      return isSuccess(response);
    } catch (Exception e) {
      Logger.debug("[VaultEconomyProvider] Exception depositing: %s", e.getMessage());
      return false;
    }
  }

  /** Deposits money into a player's account (double overload). */
  public boolean deposit(@NotNull UUID playerUuid, double amount) {
    return deposit(playerUuid, BigDecimal.valueOf(amount));
  }

  private boolean isSuccess(Object response) {
    if (response == null || transactionSuccessMethod == null) {
      return false;
    }
    try {
      Object result = transactionSuccessMethod.invoke(response);
      return result instanceof Boolean b && b;
    } catch (Exception e) {
      return false;
    }
  }
}
