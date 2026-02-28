package com.hyperessentials.module.kits.data;

import org.jetbrains.annotations.NotNull;

/**
 * Represents an item within a kit.
 *
 * @param itemId   the item type ID (e.g., "Hytale:Wooden_Sword")
 * @param quantity the number of items
 * @param slot     the target slot index, or -1 for first available
 */
public record KitItem(
    @NotNull String itemId,
    int quantity,
    int slot
) {
    public KitItem {
        if (quantity < 1) quantity = 1;
    }
}
