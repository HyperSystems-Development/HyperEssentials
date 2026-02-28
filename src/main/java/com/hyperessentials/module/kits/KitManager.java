package com.hyperessentials.module.kits;

import com.hyperessentials.Permissions;
import com.hyperessentials.command.util.CommandUtil;
import com.hyperessentials.config.ConfigManager;
import com.hyperessentials.module.kits.data.Kit;
import com.hyperessentials.module.kits.data.KitItem;
import com.hyperessentials.module.kits.storage.KitStorage;
import com.hyperessentials.util.Logger;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages kit operations: CRUD, cooldowns, claiming.
 * Uses Player component -> Inventory for item manipulation.
 */
public class KitManager {

  public enum ClaimResult {
    SUCCESS,
    ON_COOLDOWN,
    ALREADY_CLAIMED,
    NO_PERMISSION,
    KIT_NOT_FOUND
  }

  private final KitStorage storage;
  private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
  private final Set<String> oneTimeClaims = ConcurrentHashMap.newKeySet();

  public KitManager(@NotNull KitStorage storage) {
    this.storage = storage;
  }

  @Nullable
  public Kit getKit(@NotNull String name) {
    return storage.getKit(name);
  }

  @NotNull
  public Collection<Kit> getAllKits() {
    return storage.getKits().values();
  }

  @NotNull
  public List<Kit> getAvailableKits(@NotNull UUID playerUuid) {
    List<Kit> available = new ArrayList<>();
    for (Kit kit : getAllKits()) {
      if (canUseKit(playerUuid, kit)) {
        available.add(kit);
      }
    }
    return available;
  }

  public boolean canUseKit(@NotNull UUID playerUuid, @NotNull Kit kit) {
    return CommandUtil.hasPermission(playerUuid, Permissions.KIT_USE)
      || CommandUtil.hasPermission(playerUuid, kit.getEffectivePermission());
  }

  @NotNull
  public ClaimResult claimKit(@NotNull UUID playerUuid, @NotNull PlayerRef playerRef,
                @NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref,
                @NotNull Kit kit) {
    // Permission check
    if (!canUseKit(playerUuid, kit)) {
      return ClaimResult.NO_PERMISSION;
    }

    // One-time check
    if (kit.oneTime()) {
      String key = playerUuid + ":" + kit.name();
      if (oneTimeClaims.contains(key)) {
        return ClaimResult.ALREADY_CLAIMED;
      }
    }

    // Cooldown check
    if (!CommandUtil.hasPermission(playerUuid, Permissions.BYPASS_KIT_COOLDOWN)) {
      if (isOnCooldown(playerUuid, kit.name())) {
        return ClaimResult.ON_COOLDOWN;
      }
    }

    // Give items
    giveItems(store, ref, kit);

    // Track cooldown
    if (kit.cooldownSeconds() > 0) {
      String cooldownKey = playerUuid + ":" + kit.name();
      cooldowns.put(cooldownKey, System.currentTimeMillis() + (kit.cooldownSeconds() * 1000L));
    }

    // Track one-time
    if (kit.oneTime()) {
      oneTimeClaims.add(playerUuid + ":" + kit.name());
    }

    return ClaimResult.SUCCESS;
  }

  public boolean isOnCooldown(@NotNull UUID playerUuid, @NotNull String kitName) {
    String key = playerUuid + ":" + kitName;
    Long expiry = cooldowns.get(key);
    if (expiry == null) return false;
    if (System.currentTimeMillis() >= expiry) {
      cooldowns.remove(key);
      return false;
    }
    return true;
  }

  public long getRemainingCooldown(@NotNull UUID playerUuid, @NotNull String kitName) {
    String key = playerUuid + ":" + kitName;
    Long expiry = cooldowns.get(key);
    if (expiry == null) return 0;
    long remaining = expiry - System.currentTimeMillis();
    return remaining > 0 ? remaining : 0;
  }

  @NotNull
  public Kit captureFromInventory(@NotNull PlayerRef playerRef, @NotNull Store<EntityStore> store,
                  @NotNull Ref<EntityStore> ref, @NotNull String kitName) {
    List<KitItem> items = new ArrayList<>();

    try {
      Player playerComponent = store.getComponent(ref, Player.getComponentType());
      if (playerComponent != null) {
        Inventory inventory = playerComponent.getInventory();

        // Capture hotbar items
        captureContainer(inventory.getHotbar(), items, 0);

        // Capture storage items (offset by hotbar capacity)
        ItemContainer hotbar = inventory.getHotbar();
        captureContainer(inventory.getStorage(), items, hotbar.getCapacity());
      }
    } catch (Exception e) {
      Logger.warn("[KitManager] Failed to capture inventory: %s", e.getMessage());
    }

    int defaultCooldown = ConfigManager.get().kits().getDefaultCooldownSeconds();
    boolean defaultOneTime = ConfigManager.get().kits().isOneTimeDefault();

    Kit kit = new Kit(kitName.toLowerCase(), kitName, items, defaultCooldown, defaultOneTime, null);
    storage.addKit(kit);
    return kit;
  }

  public boolean deleteKit(@NotNull String name) {
    return storage.removeKit(name);
  }

  public void clearPlayerCooldowns(@NotNull UUID playerUuid) {
    String prefix = playerUuid + ":";
    cooldowns.keySet().removeIf(key -> key.startsWith(prefix));
  }

  public void shutdown() {
    storage.save();
    cooldowns.clear();
    oneTimeClaims.clear();
  }

  private void captureContainer(@NotNull ItemContainer container, @NotNull List<KitItem> items, int slotOffset) {
    short capacity = container.getCapacity();
    for (short i = 0; i < capacity; i++) {
      try {
        ItemStack stack = container.getItemStack(i);
        if (stack != null && !stack.isEmpty()) {
          items.add(new KitItem(
            stack.getItemId(),
            stack.getQuantity(),
            slotOffset + i
          ));
        }
      } catch (Exception e) {
        // Skip slot on error
      }
    }
  }

  private void giveItems(@NotNull Store<EntityStore> store, @NotNull Ref<EntityStore> ref, @NotNull Kit kit) {
    try {
      Player playerComponent = store.getComponent(ref, Player.getComponentType());
      if (playerComponent == null) {
        Logger.warn("[KitManager] Player component not found");
        return;
      }

      Inventory inventory = playerComponent.getInventory();
      ItemContainer storage = inventory.getStorage();
      ItemContainer hotbar = inventory.getHotbar();
      short hotbarCapacity = hotbar.getCapacity();

      for (KitItem kitItem : kit.items()) {
        try {
          ItemStack stack = new ItemStack(kitItem.itemId(), kitItem.quantity());
          int slot = kitItem.slot();

          if (slot >= 0 && slot < hotbarCapacity) {
            // Place in hotbar
            hotbar.setItemStackForSlot((short) slot, stack);
          } else if (slot >= hotbarCapacity) {
            // Place in storage (offset by hotbar size)
            short storageSlot = (short) (slot - hotbarCapacity);
            if (storageSlot < storage.getCapacity()) {
              storage.setItemStackForSlot(storageSlot, stack);
            }
          } else {
            // slot == -1: first available in hotbar, then storage
            // Try hotbar first
            boolean placed = false;
            for (short h = 0; h < hotbar.getCapacity(); h++) {
              if (hotbar.getItemStack(h) == null) {
                hotbar.setItemStackForSlot(h, stack);
                placed = true;
                break;
              }
            }
            if (!placed) {
              for (short s = 0; s < storage.getCapacity(); s++) {
                if (storage.getItemStack(s) == null) {
                  storage.setItemStackForSlot(s, stack);
                  break;
                }
              }
            }
          }
        } catch (Exception e) {
          Logger.warn("[KitManager] Failed to give item %s: %s", kitItem.itemId(), e.getMessage());
        }
      }
    } catch (Exception e) {
      Logger.warn("[KitManager] Failed to give kit items: %s", e.getMessage());
    }
  }
}
