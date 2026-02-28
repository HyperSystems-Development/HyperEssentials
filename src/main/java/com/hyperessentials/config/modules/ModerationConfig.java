package com.hyperessentials.config.modules;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import java.nio.file.Path;
import com.hyperessentials.config.ModuleConfig;

public class ModerationConfig extends ModuleConfig {

    private String defaultBanReason = "You have been banned from this server.";
    private String defaultMuteReason = "You have been muted.";
    private String defaultKickReason = "You have been kicked from this server.";
    private String mutedChatMessage = "You are muted. Your message was not sent.";
    private String freezeMessage = "You have been frozen by a moderator.";
    private int freezeCheckIntervalMs = 100;
    private boolean broadcastBans = true;
    private boolean broadcastKicks = true;
    private boolean broadcastMutes = false;
    private int maxHistoryPerPlayer = 50;

    public ModerationConfig(@NotNull Path filePath) { super(filePath); }

    @Override @NotNull public String getModuleName() { return "moderation"; }
    @Override protected boolean getDefaultEnabled() { return false; }

    @Override
    protected void createDefaults() {
        defaultBanReason = "You have been banned from this server.";
        defaultMuteReason = "You have been muted.";
        defaultKickReason = "You have been kicked from this server.";
        mutedChatMessage = "You are muted. Your message was not sent.";
        freezeMessage = "You have been frozen by a moderator.";
        freezeCheckIntervalMs = 100;
        broadcastBans = true;
        broadcastKicks = true;
        broadcastMutes = false;
        maxHistoryPerPlayer = 50;
    }

    @Override
    protected void loadModuleSettings(@NotNull JsonObject root) {
        defaultBanReason = getString(root, "defaultBanReason", "You have been banned from this server.");
        defaultMuteReason = getString(root, "defaultMuteReason", "You have been muted.");
        defaultKickReason = getString(root, "defaultKickReason", "You have been kicked from this server.");
        mutedChatMessage = getString(root, "mutedChatMessage", "You are muted. Your message was not sent.");
        freezeMessage = getString(root, "freezeMessage", "You have been frozen by a moderator.");
        freezeCheckIntervalMs = getInt(root, "freezeCheckIntervalMs", 100);
        broadcastBans = getBool(root, "broadcastBans", true);
        broadcastKicks = getBool(root, "broadcastKicks", true);
        broadcastMutes = getBool(root, "broadcastMutes", false);
        maxHistoryPerPlayer = getInt(root, "maxHistoryPerPlayer", 50);
    }

    @Override
    protected void writeModuleSettings(@NotNull JsonObject root) {
        root.addProperty("defaultBanReason", defaultBanReason);
        root.addProperty("defaultMuteReason", defaultMuteReason);
        root.addProperty("defaultKickReason", defaultKickReason);
        root.addProperty("mutedChatMessage", mutedChatMessage);
        root.addProperty("freezeMessage", freezeMessage);
        root.addProperty("freezeCheckIntervalMs", freezeCheckIntervalMs);
        root.addProperty("broadcastBans", broadcastBans);
        root.addProperty("broadcastKicks", broadcastKicks);
        root.addProperty("broadcastMutes", broadcastMutes);
        root.addProperty("maxHistoryPerPlayer", maxHistoryPerPlayer);
    }

    public String getDefaultBanReason() { return defaultBanReason; }
    public String getDefaultMuteReason() { return defaultMuteReason; }
    public String getDefaultKickReason() { return defaultKickReason; }
    public String getMutedChatMessage() { return mutedChatMessage; }
    public String getFreezeMessage() { return freezeMessage; }
    public int getFreezeCheckIntervalMs() { return freezeCheckIntervalMs; }
    public boolean isBroadcastBans() { return broadcastBans; }
    public boolean isBroadcastKicks() { return broadcastKicks; }
    public boolean isBroadcastMutes() { return broadcastMutes; }
    public int getMaxHistoryPerPlayer() { return maxHistoryPerPlayer; }
}
