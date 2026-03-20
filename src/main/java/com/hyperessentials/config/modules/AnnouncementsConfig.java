package com.hyperessentials.config.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hyperessentials.module.announcements.data.Announcement;
import com.hyperessentials.module.announcements.data.AnnouncementEvent;
import com.hyperessentials.module.announcements.data.AnnouncementType;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.hyperessentials.config.ModuleConfig;

/**
 * Configuration for the announcements module.
 * Supports structured announcements with types, cron scheduling, event announcements,
 * and automatic migration from the old plain-string format.
 */
public class AnnouncementsConfig extends ModuleConfig {

  private int intervalSeconds = 300;
  private boolean randomize = false;
  private String prefixText = "Announcement";
  private String prefixColor = "#FFAA00";
  private String messageColor = "#FFFFFF";

  // Structured announcements (replaces old List<String> messages)
  private List<Announcement> announcements = new ArrayList<>();

  // Event-triggered announcements
  private List<AnnouncementEvent> eventAnnouncements = new ArrayList<>();
  private boolean joinMessagesEnabled = false;
  private boolean leaveMessagesEnabled = false;
  private boolean welcomeMessagesEnabled = false;

  public AnnouncementsConfig(@NotNull Path filePath) { super(filePath); }

  @Override @NotNull public String getModuleName() { return "announcements"; }
  @Override protected boolean getDefaultEnabled() { return false; }

  @Override
  protected void createDefaults() {
    intervalSeconds = 300;
    randomize = false;
    prefixText = "Announcement";
    prefixColor = "#FFAA00";
    messageColor = "#FFFFFF";

    announcements = new ArrayList<>();
    announcements.add(new Announcement(
        UUID.randomUUID(), "Welcome to the server! Type /help for commands.",
        AnnouncementType.CHAT, true, 0, null, null, null, 0
    ));
    announcements.add(new Announcement(
        UUID.randomUUID(), "Join our Discord at discord.gg/example",
        AnnouncementType.CHAT, true, 0, null, null, null, 1
    ));

    eventAnnouncements = new ArrayList<>();
    eventAnnouncements.add(new AnnouncementEvent(
        UUID.randomUUID(), "join", "{player} joined the server!",
        AnnouncementType.CHAT, false, null
    ));
    eventAnnouncements.add(new AnnouncementEvent(
        UUID.randomUUID(), "leave", "{player} left the server.",
        AnnouncementType.CHAT, false, null
    ));
    eventAnnouncements.add(new AnnouncementEvent(
        UUID.randomUUID(), "first_join", "Welcome {player} to the server for the first time!",
        AnnouncementType.NOTIFICATION, false, null
    ));

    joinMessagesEnabled = false;
    leaveMessagesEnabled = false;
    welcomeMessagesEnabled = false;
  }

  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    intervalSeconds = getInt(root, "intervalSeconds", 300);
    randomize = getBool(root, "randomize", false);
    prefixText = getString(root, "prefixText", "Announcement");
    prefixColor = getString(root, "prefixColor", "#FFAA00");
    messageColor = getString(root, "messageColor", "#FFFFFF");

    // Event settings
    joinMessagesEnabled = getBool(root, "joinMessagesEnabled", false);
    leaveMessagesEnabled = getBool(root, "leaveMessagesEnabled", false);
    welcomeMessagesEnabled = getBool(root, "welcomeMessagesEnabled", false);

    // Check for old format migration: "messages" array of plain strings
    if (root.has("messages") && root.get("messages").isJsonArray()
        && !root.has("announcements")) {
      migrateFromLegacyFormat(root);
      return;
    }

    // Load structured announcements
    announcements = new ArrayList<>();
    if (root.has("announcements") && root.get("announcements").isJsonArray()) {
      for (JsonElement el : root.getAsJsonArray("announcements")) {
        if (el.isJsonObject()) {
          Announcement ann = deserializeAnnouncement(el.getAsJsonObject());
          if (ann != null) {
            announcements.add(ann);
          }
        }
      }
    } else {
      needsSave = true;
      createDefaultAnnouncements();
    }

    // Load event announcements
    eventAnnouncements = new ArrayList<>();
    if (root.has("eventAnnouncements") && root.get("eventAnnouncements").isJsonArray()) {
      for (JsonElement el : root.getAsJsonArray("eventAnnouncements")) {
        if (el.isJsonObject()) {
          AnnouncementEvent evt = deserializeAnnouncementEvent(el.getAsJsonObject());
          if (evt != null) {
            eventAnnouncements.add(evt);
          }
        }
      }
    } else {
      needsSave = true;
      createDefaultEventAnnouncements();
    }
  }

  /**
   * Migrates from the old List-of-strings format to structured announcements.
   */
  private void migrateFromLegacyFormat(@NotNull JsonObject root) {
    Logger.info("[Announcements] Migrating from legacy string-list format to structured announcements...");

    announcements = new ArrayList<>();
    JsonArray oldMessages = root.getAsJsonArray("messages");
    int order = 0;
    for (JsonElement el : oldMessages) {
      String text = el.getAsString();
      announcements.add(new Announcement(
          UUID.randomUUID(), text, AnnouncementType.CHAT,
          true, 0, null, null, null, order++
      ));
    }

    // Create default event announcements since they didn't exist before
    createDefaultEventAnnouncements();

    Logger.info("[Announcements] Migrated %d messages to structured format.", announcements.size());
    needsSave = true;
  }

  private void createDefaultAnnouncements() {
    announcements.add(new Announcement(
        UUID.randomUUID(), "Welcome to the server! Type /help for commands.",
        AnnouncementType.CHAT, true, 0, null, null, null, 0
    ));
    announcements.add(new Announcement(
        UUID.randomUUID(), "Join our Discord at discord.gg/example",
        AnnouncementType.CHAT, true, 0, null, null, null, 1
    ));
  }

  private void createDefaultEventAnnouncements() {
    eventAnnouncements = new ArrayList<>();
    eventAnnouncements.add(new AnnouncementEvent(
        UUID.randomUUID(), "join", "{player} joined the server!",
        AnnouncementType.CHAT, false, null
    ));
    eventAnnouncements.add(new AnnouncementEvent(
        UUID.randomUUID(), "leave", "{player} left the server.",
        AnnouncementType.CHAT, false, null
    ));
    eventAnnouncements.add(new AnnouncementEvent(
        UUID.randomUUID(), "first_join", "Welcome {player} to the server for the first time!",
        AnnouncementType.NOTIFICATION, false, null
    ));
  }

  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("intervalSeconds", intervalSeconds);
    root.addProperty("randomize", randomize);
    root.addProperty("prefixText", prefixText);
    root.addProperty("prefixColor", prefixColor);
    root.addProperty("messageColor", messageColor);
    root.addProperty("joinMessagesEnabled", joinMessagesEnabled);
    root.addProperty("leaveMessagesEnabled", leaveMessagesEnabled);
    root.addProperty("welcomeMessagesEnabled", welcomeMessagesEnabled);

    // Structured announcements
    JsonArray annArray = new JsonArray();
    for (Announcement ann : announcements) {
      annArray.add(serializeAnnouncement(ann));
    }
    root.add("announcements", annArray);

    // Event announcements
    JsonArray evtArray = new JsonArray();
    for (AnnouncementEvent evt : eventAnnouncements) {
      evtArray.add(serializeAnnouncementEvent(evt));
    }
    root.add("eventAnnouncements", evtArray);
  }

  // === Serialization ===

  @NotNull
  private JsonObject serializeAnnouncement(@NotNull Announcement ann) {
    JsonObject obj = new JsonObject();
    obj.addProperty("id", ann.id().toString());
    obj.addProperty("message", ann.message());
    obj.addProperty("type", ann.type().name());
    obj.addProperty("enabled", ann.enabled());
    obj.addProperty("priority", ann.priority());
    if (ann.permission() != null) obj.addProperty("permission", ann.permission());
    if (ann.world() != null) obj.addProperty("world", ann.world());
    if (ann.cronExpression() != null) obj.addProperty("cronExpression", ann.cronExpression());
    obj.addProperty("order", ann.order());
    return obj;
  }

  @Nullable
  private Announcement deserializeAnnouncement(@NotNull JsonObject obj) {
    try {
      UUID id = obj.has("id") ? UUID.fromString(obj.get("id").getAsString()) : UUID.randomUUID();
      String message = obj.get("message").getAsString();
      AnnouncementType type = AnnouncementType.valueOf(
          obj.has("type") ? obj.get("type").getAsString() : "CHAT"
      );
      boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
      int priority = obj.has("priority") ? obj.get("priority").getAsInt() : 0;
      String permission = obj.has("permission") ? obj.get("permission").getAsString() : null;
      String world = obj.has("world") ? obj.get("world").getAsString() : null;
      String cron = obj.has("cronExpression") ? obj.get("cronExpression").getAsString() : null;
      int order = obj.has("order") ? obj.get("order").getAsInt() : 0;
      return new Announcement(id, message, type, enabled, priority, permission, world, cron, order);
    } catch (Exception e) {
      Logger.warn("[Announcements] Failed to parse announcement: %s", e.getMessage());
      return null;
    }
  }

  @NotNull
  private JsonObject serializeAnnouncementEvent(@NotNull AnnouncementEvent evt) {
    JsonObject obj = new JsonObject();
    obj.addProperty("id", evt.id().toString());
    obj.addProperty("eventType", evt.eventType());
    obj.addProperty("message", evt.message());
    obj.addProperty("type", evt.type().name());
    obj.addProperty("enabled", evt.enabled());
    if (evt.permission() != null) obj.addProperty("permission", evt.permission());
    return obj;
  }

  @Nullable
  private AnnouncementEvent deserializeAnnouncementEvent(@NotNull JsonObject obj) {
    try {
      UUID id = obj.has("id") ? UUID.fromString(obj.get("id").getAsString()) : UUID.randomUUID();
      String eventType = obj.get("eventType").getAsString();
      String message = obj.get("message").getAsString();
      AnnouncementType type = AnnouncementType.valueOf(
          obj.has("type") ? obj.get("type").getAsString() : "CHAT"
      );
      boolean enabled = !obj.has("enabled") || obj.get("enabled").getAsBoolean();
      String permission = obj.has("permission") ? obj.get("permission").getAsString() : null;
      return new AnnouncementEvent(id, eventType, message, type, enabled, permission);
    } catch (Exception e) {
      Logger.warn("[Announcements] Failed to parse event announcement: %s", e.getMessage());
      return null;
    }
  }

  // === Getters ===

  public int getIntervalSeconds() { return intervalSeconds; }
  public boolean isRandomize() { return randomize; }
  public String getPrefixText() { return prefixText; }
  public String getPrefixColor() { return prefixColor; }
  public String getMessageColor() { return messageColor; }

  @NotNull public List<Announcement> getAnnouncements() { return announcements; }
  @NotNull public List<AnnouncementEvent> getEventAnnouncements() { return eventAnnouncements; }

  public boolean isJoinMessagesEnabled() { return joinMessagesEnabled; }
  public boolean isLeaveMessagesEnabled() { return leaveMessagesEnabled; }
  public boolean isWelcomeMessagesEnabled() { return welcomeMessagesEnabled; }

  public void setJoinMessagesEnabled(boolean joinMessagesEnabled) {
    this.joinMessagesEnabled = joinMessagesEnabled;
  }

  public void setLeaveMessagesEnabled(boolean leaveMessagesEnabled) {
    this.leaveMessagesEnabled = leaveMessagesEnabled;
  }

  public void setWelcomeMessagesEnabled(boolean welcomeMessagesEnabled) {
    this.welcomeMessagesEnabled = welcomeMessagesEnabled;
  }

  // Setters (for admin config editor)
  public void setIntervalSeconds(int value) { this.intervalSeconds = value; }
  public void setRandomize(boolean value) { this.randomize = value; }
  public void setPrefixText(String value) { this.prefixText = value; }
  public void setPrefixColor(String value) { this.prefixColor = value; }
  public void setMessageColor(String value) { this.messageColor = value; }

  /**
   * Returns flat message list for backward compatibility with /announce command.
   * Extracts the message text from each enabled CHAT announcement.
   */
  @NotNull
  public List<String> getMessages() {
    List<String> messages = new ArrayList<>();
    for (Announcement ann : announcements) {
      if (ann.enabled()) {
        messages.add(ann.message());
      }
    }
    return messages;
  }
}
