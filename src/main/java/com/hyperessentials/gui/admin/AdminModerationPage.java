package com.hyperessentials.gui.admin;

import com.hyperessentials.gui.GuiColors;
import com.hyperessentials.gui.GuiManager;
import com.hyperessentials.gui.GuiType;
import com.hyperessentials.gui.NavBarHelper;
import com.hyperessentials.gui.UIHelper;
import com.hyperessentials.gui.UIPaths;
import com.hyperessentials.gui.data.AdminPageData;
import com.hyperessentials.module.moderation.ModerationManager;
import com.hyperessentials.util.AdminKeys;
import com.hyperessentials.util.HEMessages;
import com.hyperessentials.module.moderation.data.Punishment;
import com.hyperessentials.module.moderation.data.PunishmentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Admin moderation page — list punishments with filter and revoke.
 */
public class AdminModerationPage extends InteractiveCustomUIPage<AdminPageData> {

  private static final DateTimeFormatter DATE_FORMAT =
      DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneId.systemDefault());

  private final PlayerRef playerRef;
  private final Player player;
  private final ModerationManager moderationManager;
  private final GuiManager guiManager;
  private boolean showActiveOnly = true;

  public AdminModerationPage(
      @NotNull Player player,
      @NotNull PlayerRef playerRef,
      @NotNull ModerationManager moderationManager,
      @NotNull GuiManager guiManager
  ) {
    super(playerRef, CustomPageLifetime.CanDismiss, AdminPageData.CODEC);
    this.player = player;
    this.playerRef = playerRef;
    this.moderationManager = moderationManager;
    this.guiManager = guiManager;
  }

  @Override
  public void build(@NotNull Ref<EntityStore> ref, @NotNull UICommandBuilder cmd,
                    @NotNull UIEventBuilder events, @NotNull Store<EntityStore> store) {
    cmd.append(UIPaths.ADMIN_MODERATION);
    NavBarHelper.setupAdminBar(playerRef, "moderation", guiManager.getAdminRegistry(), cmd, events);
    buildPunishmentList(cmd, events);
  }

  private void buildPunishmentList(@NotNull UICommandBuilder cmd, @NotNull UIEventBuilder events) {
    // Filter buttons
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#FilterActive",
        EventData.of("Button", "FilterActive"), false
    );
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#FilterAll",
        EventData.of("Button", "FilterAll"), false
    );

    // Player Search button (navigates to AdminPlayerModerationPage)
    // Added inline after the filter buttons row in the list rebuild
    cmd.appendInline("#PunishmentList",
        "Group { Anchor: (Height: 28, Bottom: 6); LayoutMode: Left; "
        + "TextButton #PlayerSearchBtn { Text: \"Player Search\"; Anchor: (Width: 110, Height: 26); } }");
    events.addEventBinding(
        CustomUIEventBindingType.Activating, "#PunishmentList #PlayerSearchBtn",
        EventData.of("Button", "PlayerSearch"), false
    );

    List<Punishment> punishments = moderationManager.getAllPunishments(showActiveOnly);
    punishments.sort(Comparator.comparing(Punishment::issuedAt).reversed());

    String label = punishments.size() + " punishment" + (punishments.size() != 1 ? "s" : "");
    if (showActiveOnly) label += " (active)";
    cmd.set("#PunishmentCount.Text", label);

    cmd.clear("#PunishmentList");
    cmd.appendInline("#PunishmentList", "Group #IndexCards { LayoutMode: Top; }");

    if (punishments.isEmpty()) {
      cmd.append("#IndexCards", UIPaths.EMPTY_STATE);
      cmd.set("#IndexCards[0] #EmptyTitle.Text", showActiveOnly
          ? HEMessages.get(playerRef, AdminKeys.Moderation.EMPTY_ACTIVE_TITLE)
          : HEMessages.get(playerRef, AdminKeys.Moderation.EMPTY_ALL_TITLE));
      cmd.set("#IndexCards[0] #EmptyMessage.Text", HEMessages.get(playerRef, AdminKeys.Moderation.EMPTY_MESSAGE));
      return;
    }

    int i = 0;
    for (Punishment p : punishments) {
      cmd.append("#IndexCards", UIPaths.ADMIN_PUNISHMENT_ENTRY);
      String idx = "#IndexCards[" + i + "]";

      // Type badge and color
      String typeLabel = p.type().name();
      String typeColor = switch (p.type()) {
        case BAN -> GuiColors.DANGER;
        case IPBAN -> "#AA0000";
        case MUTE -> GuiColors.WARNING;
        case KICK -> GuiColors.INFO;
        case WARN -> "#FFFF55";
      };
      cmd.set(idx + " #TypeBadge.Text", typeLabel);
      cmd.set(idx + " #TypeBadge.Style.TextColor", typeColor);
      cmd.set(idx + " #TypeBar.Background.Color", typeColor);

      cmd.set(idx + " #PlayerName.Text", p.playerName());
      cmd.set(idx + " #IssuerName.Text", "by " + p.issuerName());
      cmd.set(idx + " #Reason.Text", p.reason() != null ? p.reason() : HEMessages.get(playerRef, AdminKeys.Moderation.NO_REASON));

      // Expires info
      if (p.isEffective()) {
        if (p.isPermanent()) {
          cmd.set(idx + " #Expires.Text", HEMessages.get(playerRef, AdminKeys.Moderation.PERMANENT));
        } else {
          cmd.set(idx + " #Expires.Text", UIHelper.formatDuration((int) (p.getRemainingMillis() / 1000)));
        }
      } else {
        cmd.set(idx + " #Expires.Text", p.active()
            ? HEMessages.get(playerRef, AdminKeys.Moderation.EXPIRED)
            : HEMessages.get(playerRef, AdminKeys.Moderation.REVOKED));
      }

      // Only show revoke button for active punishments (not kicks/warns)
      if (p.isEffective() && p.type() != PunishmentType.KICK && p.type() != PunishmentType.WARN) {
        events.addEventBinding(
            CustomUIEventBindingType.Activating,
            idx + " #RevokeBtn",
            EventData.of("Button", "Revoke").append("Target", p.id().toString()),
            false
        );
      }

      i++;
    }
  }

  @Override
  public void handleDataEvent(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store,
                              @NotNull AdminPageData data) {
    super.handleDataEvent(ref, store, data);

    if ("Nav".equals(data.button)) {
      NavBarHelper.handleNavEvent(
          data.navTarget != null ? data.navTarget : "",
          player, ref, store, playerRef, guiManager, GuiType.ADMIN
      );
      return;
    }

    if (data.button == null) {
      sendUpdate();
      return;
    }

    switch (data.button) {
      case "FilterActive" -> { showActiveOnly = true; rebuildList(); }
      case "FilterAll" -> { showActiveOnly = false; rebuildList(); }
      case "Revoke" -> { handleRevoke(data.target); }
      case "PlayerSearch" -> { openPlayerModeration(ref, store); }
      default -> sendUpdate();
    }
  }

  private void handleRevoke(String punishmentIdStr) {
    if (punishmentIdStr == null) return;

    try {
      UUID punishmentId = UUID.fromString(punishmentIdStr);
      // Find the punishment and revoke it based on type
      List<Punishment> all = moderationManager.getAllPunishments(true);
      for (Punishment p : all) {
        if (p.id().equals(punishmentId)) {
          if (p.type() == PunishmentType.BAN) {
            moderationManager.unban(p.playerUuid(), playerRef.getUuid(), playerRef.getUsername());
          } else if (p.type() == PunishmentType.MUTE) {
            moderationManager.unmute(p.playerUuid(), playerRef.getUuid(), playerRef.getUsername());
          }
          break;
        }
      }
    } catch (IllegalArgumentException ignored) {
    }

    rebuildList();
  }

  private void openPlayerModeration(@NotNull Ref<EntityStore> ref, @NotNull Store<EntityStore> store) {
    AdminPlayerModerationPage page = new AdminPlayerModerationPage(player, playerRef, guiManager);
    player.getPageManager().openCustomPage(ref, store, page);
  }

  private void rebuildList() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder events = new UIEventBuilder();
    buildPunishmentList(cmd, events);
    sendUpdate(cmd, events, false);
  }
}
