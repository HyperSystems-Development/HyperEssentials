package com.hyperessentials.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Stores teleportation-related data for a player.
 * This includes TPA toggle state, back history, and timestamps.
 */
public class PlayerTeleportData {

    private final UUID uuid;
    private String username;
    private boolean tpToggle; // true = accepting TPA requests
    private List<Location> backHistory;
    private long lastTpaRequest;
    private long lastTeleport;

    public PlayerTeleportData(@NotNull UUID uuid, @NotNull String username) {
        this.uuid = uuid;
        this.username = username;
        this.tpToggle = true;
        this.backHistory = new ArrayList<>();
        this.lastTpaRequest = 0;
        this.lastTeleport = 0;
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @NotNull
    public String getUsername() {
        return username;
    }

    public void setUsername(@NotNull String username) {
        this.username = username;
    }

    public boolean isTpToggle() {
        return tpToggle;
    }

    public void setTpToggle(boolean tpToggle) {
        this.tpToggle = tpToggle;
    }

    public boolean toggleTpToggle() {
        tpToggle = !tpToggle;
        return tpToggle;
    }

    public long getLastTpaRequest() {
        return lastTpaRequest;
    }

    public void setLastTpaRequest(long lastTpaRequest) {
        this.lastTpaRequest = lastTpaRequest;
    }

    public long getLastTeleport() {
        return lastTeleport;
    }

    public void setLastTeleport(long lastTeleport) {
        this.lastTeleport = lastTeleport;
    }

    @NotNull
    public List<Location> getBackHistory() {
        return Collections.unmodifiableList(backHistory);
    }

    @Nullable
    public Location getLastBackLocation() {
        return backHistory.isEmpty() ? null : backHistory.get(0);
    }

    public void addBackLocation(@NotNull Location location, int maxSize) {
        backHistory.add(0, location);
        while (backHistory.size() > maxSize) {
            backHistory.remove(backHistory.size() - 1);
        }
    }

    @Nullable
    public Location popBackLocation() {
        if (backHistory.isEmpty()) {
            return null;
        }
        return backHistory.remove(0);
    }

    public void clearBackHistory() {
        backHistory.clear();
    }

    public void setBackHistory(@NotNull List<Location> history) {
        this.backHistory = new ArrayList<>(history);
    }
}
