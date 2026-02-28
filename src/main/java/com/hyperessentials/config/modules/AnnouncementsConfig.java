package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import com.hyperessentials.config.ModuleConfig;

public class AnnouncementsConfig extends ModuleConfig {

  private int intervalSeconds = 300;
  private boolean randomize = false;
  private String prefixText = "Announcement";
  private String prefixColor = "#FFAA00";
  private String messageColor = "#FFFFFF";
  private List<String> messages = new ArrayList<>();

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
    messages = new ArrayList<>();
    messages.add("Welcome to the server! Type /help for commands.");
    messages.add("Join our Discord at discord.gg/example");
  }

  @Override
  protected void loadModuleSettings(@NotNull JsonObject root) {
    intervalSeconds = getInt(root, "intervalSeconds", 300);
    randomize = getBool(root, "randomize", false);
    prefixText = getString(root, "prefixText", "Announcement");
    prefixColor = getString(root, "prefixColor", "#FFAA00");
    messageColor = getString(root, "messageColor", "#FFFFFF");
    messages = new ArrayList<>(getStringList(root, "messages"));
    if (messages.isEmpty() && !root.has("messages")) {
      messages.add("Welcome to the server! Type /help for commands.");
      messages.add("Join our Discord at discord.gg/example");
    }
  }

  @Override
  protected void writeModuleSettings(@NotNull JsonObject root) {
    root.addProperty("intervalSeconds", intervalSeconds);
    root.addProperty("randomize", randomize);
    root.addProperty("prefixText", prefixText);
    root.addProperty("prefixColor", prefixColor);
    root.addProperty("messageColor", messageColor);
    root.add("messages", toJsonArray(messages));
  }

  public int getIntervalSeconds() { return intervalSeconds; }
  public boolean isRandomize() { return randomize; }
  public String getPrefixText() { return prefixText; }
  public String getPrefixColor() { return prefixColor; }
  public String getMessageColor() { return messageColor; }
  @NotNull public List<String> getMessages() { return messages; }
}
