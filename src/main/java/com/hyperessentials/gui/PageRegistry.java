package com.hyperessentials.gui;

import com.hyperessentials.integration.PermissionManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for GUI pages. Modules register their pages here.
 * Pages can be auto-unregistered when a module is disabled.
 */
public class PageRegistry {

    /**
     * A registered GUI page entry.
     *
     * @param id           Unique page identifier (e.g., "homes", "warps")
     * @param displayName  UI display name
     * @param module       Owning module name
     * @param permission   Required permission node (null for no permission)
     * @param supplier     Function to create the page instance
     * @param showsInNavBar Whether this page appears in the navigation bar
     * @param order        Display order in navigation (lower = first)
     */
    public record Entry(
            @NotNull String id,
            @NotNull String displayName,
            @NotNull String module,
            @Nullable String permission,
            @NotNull PageSupplier supplier,
            boolean showsInNavBar,
            int order
    ) implements Comparable<Entry> {
        @Override
        public int compareTo(@NotNull Entry other) {
            return Integer.compare(this.order, other.order);
        }
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final List<Entry> orderedEntries = new ArrayList<>();

    /**
     * Registers a page entry.
     */
    public void registerEntry(@NotNull Entry entry) {
        entries.put(entry.id(), entry);
        orderedEntries.add(entry);
        orderedEntries.sort(Entry::compareTo);
    }

    /**
     * Unregisters all pages owned by a module.
     */
    public void unregisterModule(@NotNull String moduleName) {
        entries.values().removeIf(e -> e.module().equals(moduleName));
        orderedEntries.removeIf(e -> e.module().equals(moduleName));
    }

    /**
     * Gets an entry by ID.
     */
    @Nullable
    public Entry getEntry(@NotNull String id) {
        return entries.get(id);
    }

    /**
     * Gets all registered entries in display order.
     */
    @NotNull
    public List<Entry> getEntries() {
        return Collections.unmodifiableList(orderedEntries);
    }

    /**
     * Gets entries that should appear in the navigation bar.
     */
    @NotNull
    public List<Entry> getNavBarEntries() {
        return orderedEntries.stream()
                .filter(Entry::showsInNavBar)
                .toList();
    }

    /**
     * Gets nav bar entries accessible to a player.
     */
    @NotNull
    public List<Entry> getAccessibleNavBarEntries(@NotNull PlayerRef playerRef) {
        return orderedEntries.stream()
                .filter(Entry::showsInNavBar)
                .filter(entry -> {
                    if (entry.permission() == null) return true;
                    return PermissionManager.get().hasPermission(playerRef.getUuid(), entry.permission());
                })
                .toList();
    }

    /**
     * Clears all registered entries.
     */
    public void clear() {
        entries.clear();
        orderedEntries.clear();
    }
}
