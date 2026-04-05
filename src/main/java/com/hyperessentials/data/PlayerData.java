package com.hyperessentials.data;

import com.hyperessentials.module.moderation.data.Punishment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Unified per-player data: teleportation state, stats, and punishments.
 * Stored in {@code data/players/<uuid>.json}.
 */
public class PlayerData {

  private final UUID uuid;
  private String username;

  // === Teleport ===
  private boolean tpToggle; // true = accepting TPA requests
  private List<BackEntry> backHistory;
  private long lastTpaRequest;
  private long lastTeleport;

  // === Stats ===
  private Instant firstJoin;
  private long totalPlaytimeMs;
  private Instant lastJoin;

  // === Language ===
  private String languagePreference; // null = use server default / client detection

  // === Punishments ===
  private final List<Punishment> punishments;

  public PlayerData(@NotNull UUID uuid, @NotNull String username) {
    this.uuid = uuid;
    this.username = username;
    this.tpToggle = true;
    this.backHistory = new ArrayList<>();
    this.lastTpaRequest = 0;
    this.lastTeleport = 0;
    this.firstJoin = Instant.now();
    this.totalPlaytimeMs = 0;
    this.lastJoin = Instant.now();
    this.languagePreference = null;
    this.punishments = new ArrayList<>();
  }

  // === Identity ===

  @NotNull public UUID getUuid() { return uuid; }
  @NotNull public String getUsername() { return username; }
  public void setUsername(@NotNull String username) { this.username = username; }

  // === Teleport ===

  public boolean isTpToggle() { return tpToggle; }
  public void setTpToggle(boolean tpToggle) { this.tpToggle = tpToggle; }

  public boolean toggleTpToggle() {
    tpToggle = !tpToggle;
    return tpToggle;
  }

  public long getLastTpaRequest() { return lastTpaRequest; }
  public void setLastTpaRequest(long lastTpaRequest) { this.lastTpaRequest = lastTpaRequest; }

  public long getLastTeleport() { return lastTeleport; }
  public void setLastTeleport(long lastTeleport) { this.lastTeleport = lastTeleport; }

  @NotNull
  public List<BackEntry> getBackHistory() {
    return Collections.unmodifiableList(backHistory);
  }

  @Nullable
  public BackEntry getLastBackEntry() {
    return backHistory.isEmpty() ? null : backHistory.getFirst();
  }

  public void addBackEntry(@NotNull BackEntry entry, int maxSize) {
    backHistory.addFirst(entry);
    while (backHistory.size() > maxSize) {
      backHistory.removeLast();
    }
  }

  @Nullable
  public BackEntry popBackEntry() {
    if (backHistory.isEmpty()) return null;
    return backHistory.removeFirst();
  }

  @Nullable
  public BackEntry removeBackEntry(int index) {
    if (index < 0 || index >= backHistory.size()) return null;
    return backHistory.remove(index);
  }

  public void clearBackHistory() { backHistory.clear(); }

  public void setBackHistory(@NotNull List<BackEntry> history) {
    this.backHistory = new ArrayList<>(history);
  }

  // === Stats ===

  @NotNull public Instant getFirstJoin() { return firstJoin; }
  public void setFirstJoin(@NotNull Instant firstJoin) { this.firstJoin = firstJoin; }

  public long getTotalPlaytimeMs() { return totalPlaytimeMs; }
  public void setTotalPlaytimeMs(long totalPlaytimeMs) { this.totalPlaytimeMs = totalPlaytimeMs; }
  public void addPlaytimeMs(long ms) { this.totalPlaytimeMs += ms; }

  @NotNull public Instant getLastJoin() { return lastJoin; }
  public void setLastJoin(@NotNull Instant lastJoin) { this.lastJoin = lastJoin; }

  // === Language ===

  @Nullable public String getLanguagePreference() { return languagePreference; }
  public void setLanguagePreference(@Nullable String languagePreference) { this.languagePreference = languagePreference; }

  // === Punishments ===

  @NotNull
  public List<Punishment> getPunishments() {
    return Collections.unmodifiableList(punishments);
  }

  public void addPunishment(@NotNull Punishment punishment) {
    punishments.add(punishment);
  }

  public void setPunishments(@NotNull List<Punishment> list) {
    this.punishments.clear();
    this.punishments.addAll(list);
  }

  /**
   * Revokes a punishment by ID and returns true if found.
   */
  public boolean revokePunishment(@NotNull UUID punishmentId, @Nullable UUID revokerUuid,
                                  @NotNull String revokerName) {
    for (int i = 0; i < punishments.size(); i++) {
      Punishment p = punishments.get(i);
      if (p.id().equals(punishmentId) && p.active()) {
        punishments.set(i, p.revoke(revokerUuid, revokerName));
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the currently active ban, or null if not banned.
   */
  @Nullable
  public Punishment getActiveBan() {
    for (Punishment p : punishments) {
      if (p.type() == com.hyperessentials.module.moderation.data.PunishmentType.BAN && p.isEffective()) {
        return p;
      }
    }
    return null;
  }

  /**
   * Returns the currently active mute, or null if not muted.
   */
  @Nullable
  public Punishment getActiveMute() {
    for (Punishment p : punishments) {
      if (p.type() == com.hyperessentials.module.moderation.data.PunishmentType.MUTE && p.isEffective()) {
        return p;
      }
    }
    return null;
  }
}
