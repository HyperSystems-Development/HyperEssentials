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
 * Supports hotbar, storage, armor, and utility inventory sections.
 */
public class KitManager {

  public enum ClaimResult {
    SUCCESS,
    ON_COOLDOWN,
    ALREADY_CLAIMED,
    NO_PERMISSION,
    KIT_NOT_FOUND,
    INSUFFICIENT_SPACE
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

    // Space check before giving items
    Player playerComponent = store.getComponent(ref, Player.getComponentType());
    if (playerComponent == null) {
      Logger.warn("[KitManager] Player component not found");
      return ClaimResult.KIT_NOT_FOUND;
    }

    if (!hasSpaceForKit(playerComponent.getInventory(), kit)) {
      return ClaimResult.INSUFFICIENT_SPACE;
    }

    // Give items
    giveItems(playerComponent.getInventory(), kit);

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
        captureContainer(inventory.getHotbar(), items, KitItem.HOTBAR);

        // Capture storage items
        captureContainer(inventory.getStorage(), items, KitItem.STORAGE);

        // Capture armor items
        captureContainer(inventory.getArmor(), items, KitItem.ARMOR);

        // Capture utility items
        captureContainer(inventory.getUtility(), items, KitItem.UTILITY);
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

  /**
   * Checks if the player has enough inventory space for the kit.
   * Displaced armor/utility items need room in hotbar/storage.
   */
  private boolean hasSpaceForKit(@NotNull Inventory inventory, @NotNull Kit kit) {
    ItemContainer hotbar = inventory.getHotbar();
    ItemContainer storageContainer = inventory.getStorage();
    ItemContainer armor = inventory.getArmor();
    ItemContainer utility = inventory.getUtility();

    int slotsNeeded = 0;

    for (KitItem kitItem : kit.items()) {
      switch (kitItem.section()) {
        case KitItem.ARMOR -> {
          // If armor slot is occupied, existing item needs to move to hotbar/storage
          if (kitItem.slot() >= 0 && kitItem.slot() < armor.getCapacity()) {
            ItemStack existing = armor.getItemStack((short) kitItem.slot());
            if (!ItemStack.isEmpty(existing)) {
              slotsNeeded++;
            }
          }
        }
        case KitItem.UTILITY -> {
          // If utility slot is occupied, existing item needs to move to hotbar/storage
          if (kitItem.slot() >= 0 && kitItem.slot() < utility.getCapacity()) {
            ItemStack existing = utility.getItemStack((short) kitItem.slot());
            if (!ItemStack.isEmpty(existing)) {
              slotsNeeded++;
            }
          }
        }
        case KitItem.HOTBAR, KitItem.STORAGE -> {
          // Specific slot items overwrite — no extra space needed
          // But slot=-1 items need a free slot
          if (kitItem.slot() < 0) {
            slotsNeeded++;
          }
        }
      }
    }

    if (slotsNeeded == 0) {
      return true;
    }

    // Count free slots in hotbar + storage
    int freeSlots = countFreeSlots(hotbar) + countFreeSlots(storageContainer);

    // Subtract slots that will be taken by kit items with specific hotbar/storage slots
    // going into currently-empty slots (those free slots will no longer be available)
    for (KitItem kitItem : kit.items()) {
      if (kitItem.slot() >= 0) {
        if (kitItem.section().equals(KitItem.HOTBAR)
            && kitItem.slot() < hotbar.getCapacity()) {
          ItemStack existing = hotbar.getItemStack((short) kitItem.slot());
          if (ItemStack.isEmpty(existing)) {
            freeSlots--; // This free slot will be used by the kit item
          }
        } else if (kitItem.section().equals(KitItem.STORAGE)
            && kitItem.slot() < storageContainer.getCapacity()) {
          ItemStack existing = storageContainer.getItemStack((short) kitItem.slot());
          if (ItemStack.isEmpty(existing)) {
            freeSlots--; // This free slot will be used by the kit item
          }
        }
      }
    }

    return freeSlots >= slotsNeeded;
  }

  private int countFreeSlots(@NotNull ItemContainer container) {
    int free = 0;
    for (short i = 0; i < container.getCapacity(); i++) {
      if (ItemStack.isEmpty(container.getItemStack(i))) {
        free++;
      }
    }
    return free;
  }

  private void captureContainer(@NotNull ItemContainer container, @NotNull List<KitItem> items,
                  @NotNull String section) {
    short capacity = container.getCapacity();
    for (short i = 0; i < capacity; i++) {
      try {
        ItemStack stack = container.getItemStack(i);
        if (!ItemStack.isEmpty(stack)) {
          items.add(new KitItem(
            stack.getItemId(),
            stack.getQuantity(),
            i,
            section
          ));
        }
      } catch (Exception e) {
        // Skip slot on error
      }
    }
  }

  private void giveItems(@NotNull Inventory inventory, @NotNull Kit kit) {
    ItemContainer hotbar = inventory.getHotbar();
    ItemContainer storageContainer = inventory.getStorage();
    ItemContainer armor = inventory.getArmor();
    ItemContainer utility = inventory.getUtility();

    // Collect displaced items that need to go to hotbar/storage
    List<ItemStack> displaced = new ArrayList<>();

    for (KitItem kitItem : kit.items()) {
      try {
        ItemStack stack = new ItemStack(kitItem.itemId(), kitItem.quantity());
        int slot = kitItem.slot();

        switch (kitItem.section()) {
          case KitItem.ARMOR -> {
            if (slot >= 0 && slot < armor.getCapacity()) {
              ItemStack existing = armor.getItemStack((short) slot);
              if (!ItemStack.isEmpty(existing)) {
                displaced.add(existing);
              }
              armor.setItemStackForSlot((short) slot, stack);
            }
          }
          case KitItem.UTILITY -> {
            if (slot >= 0 && slot < utility.getCapacity()) {
              ItemStack existing = utility.getItemStack((short) slot);
              if (!ItemStack.isEmpty(existing)) {
                displaced.add(existing);
              }
              utility.setItemStackForSlot((short) slot, stack);
            }
          }
          case KitItem.HOTBAR -> {
            if (slot >= 0 && slot < hotbar.getCapacity()) {
              hotbar.setItemStackForSlot((short) slot, stack);
            } else if (slot < 0) {
              placeInFirstAvailable(hotbar, storageContainer, stack);
            }
          }
          case KitItem.STORAGE -> {
            if (slot >= 0 && slot < storageContainer.getCapacity()) {
              storageContainer.setItemStackForSlot((short) slot, stack);
            } else if (slot < 0) {
              placeInFirstAvailable(hotbar, storageContainer, stack);
            }
          }
        }
      } catch (Exception e) {
        Logger.warn("[KitManager] Failed to give item %s: %s", kitItem.itemId(), e.getMessage());
      }
    }

    // Place displaced armor/utility items into hotbar/storage
    for (ItemStack item : displaced) {
      placeInFirstAvailable(hotbar, storageContainer, item);
    }
  }

  /**
   * Places an item in the first available slot, checking hotbar then storage.
   */
  private void placeInFirstAvailable(@NotNull ItemContainer hotbar,
                    @NotNull ItemContainer storage,
                    @NotNull ItemStack stack) {
    for (short h = 0; h < hotbar.getCapacity(); h++) {
      if (ItemStack.isEmpty(hotbar.getItemStack(h))) {
        hotbar.setItemStackForSlot(h, stack);
        return;
      }
    }
    for (short s = 0; s < storage.getCapacity(); s++) {
      if (ItemStack.isEmpty(storage.getItemStack(s))) {
        storage.setItemStackForSlot(s, stack);
        return;
      }
    }
    Logger.warn("[KitManager] No space for displaced item: %s", stack.getItemId());
  }
}
