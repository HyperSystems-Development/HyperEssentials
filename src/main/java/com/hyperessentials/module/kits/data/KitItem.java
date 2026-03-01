package com.hyperessentials.module.kits.data;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an item within a kit.
 *
 * @param itemId   the item type ID (e.g., "Weapon_Sword_Adamantite")
 * @param quantity the number of items
 * @param slot     the target slot index within the section, or -1 for first available
 * @param section  the inventory section: "hotbar", "storage", "armor", or "utility"
 */
public record KitItem(
  @NotNull String itemId,
  int quantity,
  int slot,
  @NotNull String section
) {
  public static final String HOTBAR = "hotbar";
  public static final String STORAGE = "storage";
  public static final String ARMOR = "armor";
  public static final String UTILITY = "utility";

  public KitItem {
    if (quantity < 1) quantity = 1;
  }
}
