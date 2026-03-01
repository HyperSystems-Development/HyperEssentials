package com.hyperessentials.gui.player;

import com.hyperessentials.data.PlayerStats;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.PlayerPageData;
import com.hyperessentials.module.utility.UtilityManager;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Player stats page — displays playtime, join date, session info, and status indicators.
 */
public class StatsPage extends InteractiveCustomUIPage<PlayerPageData> {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("MMM d, yyyy 'at' HH:mm").withZone(ZoneId.systemDefault());

  private final PlayerRef playerRef;
  private final Player player;
  private final GuiManager guiManager;
  @Nullable private final UtilityManager utilityManager;

  public StatsPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull GuiManager guiManager,
      @Nullable UtilityManager utilityManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, PlayerPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.guiManager = guiManager;
    this.utilityManager = utilityManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.PLAYER_STATS);
    NavBarHelper.setupBar(playerRef, "stats", guiManager.getPlayerRegistry(), cmd, events);
    populateStats(cmd);
  }

  private void populateStats(@NotNull UICommandBuilder cmd) {
    UUID uuid = playerRef.getUuid();

    cmd.set("#PlayerName.Text", playerRef.getUsername());

    // Stats rows
    cmd.clear("#StatsList");
    int statIdx = 0;

    if (utilityManager != null) {
      PlayerStats stats = utilityManager.getPlayerStats(uuid);

      // First join date
      if (stats != null) {
        appendStatRow(cmd, statIdx++, "First Joined", DATE_FORMAT.format(stats.firstJoin()));
        appendStatRow(cmd, statIdx++, "Last Login", DATE_FORMAT.format(stats.lastJoin()));
      }

      // Total playtime
      long playtimeMs = utilityManager.getTotalPlaytimeMs(uuid);
      appendStatRow(cmd, statIdx++, "Total Playtime", UIHelper.formatPlaytime(playtimeMs));

      // Session time
      Instant sessionStart = utilityManager.getSessionStart(uuid);
      if (sessionStart != null) {
        long sessionMs = System.currentTimeMillis() - sessionStart.toEpochMilli();
        appendStatRow(cmd, statIdx++, "Current Session", UIHelper.formatPlaytime(sessionMs));
      }
    }

    // Status indicators
    cmd.clear("#StatusList");
    int statusIdx = 0;

    if (utilityManager != null) {
      appendStatusRow(cmd, statusIdx++, "AFK", utilityManager.isAfk(uuid));
      appendStatusRow(cmd, statusIdx++, "Fly Mode", utilityManager.isFlying(uuid));
      appendStatusRow(cmd, statusIdx++, "God Mode", utilityManager.isGod(uuid));
      appendStatusRow(cmd, statusIdx++, "Infinite Stamina", utilityManager.isInfiniteStamina(uuid));
    }
  }

  private void appendStatRow(@NotNull UICommandBuilder cmd, int index, String label, String value) {
    cmd.append("#StatsList", UIPaths.STAT_ROW);
    String idx = "#StatsList[" + index + "]";
    cmd.set(idx + " #StatLabel.Text", label);
    cmd.set(idx + " #StatValue.Text", value);
  }

  private void appendStatusRow(@NotNull UICommandBuilder cmd, int index, String label, boolean active) {
    cmd.append("#StatusList", UIPaths.STAT_ROW);
    String idx = "#StatusList[" + index + "]";
    cmd.set(idx + " #StatLabel.Text", label);
    cmd.set(idx + " #StatValue.Text", active ? "Active" : "Inactive");
    if (active) {
      cmd.set(idx + " #StatValue.Style.TextColor", "#4aff7f");
    }
  }

  @Override
  public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                              @NotNull PlayerPageData data) {
    super.handleDataEvent(ref, store, data);

    if ("Nav".equals(data.button)) {
      NavBarHelper.handleNavEvent(
          data.navTarget != null ? data.navTarget : "",
          player, ref, store, playerRef, guiManager, GuiType.PLAYER
      );
      return;
    }

    sendUpdate();
  }
}
