package com.hyperessentials.module.kits.storage;

import com.google.gson.*;
import com.hyperessentials.module.kits.data.Kit;
import com.hyperessentials.module.kits.data.KitItem;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and saves kit definitions from data/kits.json.
 */
public class KitStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path filePath;
    private final Map<String, Kit> kits = new ConcurrentHashMap<>();

    public KitStorage(@NotNull Path dataDir) {
        this.filePath = dataDir.resolve("data").resolve("kits.json");
    }

    public void load() {
        kits.clear();

        if (!Files.exists(filePath)) {
            Logger.info("[KitStorage] No kits data file found, starting fresh");
            return;
        }

        try {
            String json = Files.readString(filePath);
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (root.has("kits") && root.get("kits").isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray("kits")) {
                    Kit kit = deserializeKit(el.getAsJsonObject());
                    if (kit != null) {
                        kits.put(kit.name(), kit);
                    }
                }
            }

            Logger.info("[KitStorage] Loaded %d kit(s)", kits.size());
        } catch (Exception e) {
            Logger.severe("[KitStorage] Failed to load kits: %s", e.getMessage());
        }
    }

    public void save() {
        try {
            Files.createDirectories(filePath.getParent());

            JsonObject root = new JsonObject();
            JsonArray kitsArray = new JsonArray();

            for (Kit kit : kits.values()) {
                kitsArray.add(serializeKit(kit));
            }
            root.add("kits", kitsArray);

            Files.writeString(filePath, GSON.toJson(root));
            Logger.debug("[KitStorage] Saved %d kit(s)", kits.size());
        } catch (IOException e) {
            Logger.severe("[KitStorage] Failed to save kits: %s", e.getMessage());
        }
    }

    @NotNull
    public Map<String, Kit> getKits() {
        return Collections.unmodifiableMap(kits);
    }

    @Nullable
    public Kit getKit(@NotNull String name) {
        return kits.get(name.toLowerCase());
    }

    public void addKit(@NotNull Kit kit) {
        kits.put(kit.name(), kit);
        save();
    }

    public boolean removeKit(@NotNull String name) {
        Kit removed = kits.remove(name.toLowerCase());
        if (removed != null) {
            save();
            return true;
        }
        return false;
    }

    private JsonObject serializeKit(@NotNull Kit kit) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", kit.name());
        obj.addProperty("displayName", kit.displayName());
        obj.addProperty("cooldownSeconds", kit.cooldownSeconds());
        obj.addProperty("oneTime", kit.oneTime());
        if (kit.permission() != null) {
            obj.addProperty("permission", kit.permission());
        }

        JsonArray items = new JsonArray();
        for (KitItem item : kit.items()) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("itemId", item.itemId());
            itemObj.addProperty("quantity", item.quantity());
            itemObj.addProperty("slot", item.slot());
            items.add(itemObj);
        }
        obj.add("items", items);

        return obj;
    }

    @Nullable
    private Kit deserializeKit(@NotNull JsonObject obj) {
        try {
            String name = obj.get("name").getAsString();
            String displayName = obj.has("displayName") ? obj.get("displayName").getAsString() : name;
            int cooldown = obj.has("cooldownSeconds") ? obj.get("cooldownSeconds").getAsInt() : 0;
            boolean oneTime = obj.has("oneTime") && obj.get("oneTime").getAsBoolean();
            String permission = obj.has("permission") ? obj.get("permission").getAsString() : null;

            List<KitItem> items = new ArrayList<>();
            if (obj.has("items") && obj.get("items").isJsonArray()) {
                for (JsonElement el : obj.getAsJsonArray("items")) {
                    JsonObject itemObj = el.getAsJsonObject();
                    items.add(new KitItem(
                        itemObj.get("itemId").getAsString(),
                        itemObj.has("quantity") ? itemObj.get("quantity").getAsInt() : 1,
                        itemObj.has("slot") ? itemObj.get("slot").getAsInt() : -1
                    ));
                }
            }

            return new Kit(name.toLowerCase(), displayName, items, cooldown, oneTime, permission);
        } catch (Exception e) {
            Logger.warn("[KitStorage] Failed to parse kit: %s", e.getMessage());
            return null;
        }
    }
}
