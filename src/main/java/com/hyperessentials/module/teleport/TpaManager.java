package com.hyperessentials.module.teleport;

import com.hyperessentials.Permissions;
import com.hyperessentials.config.modules.TeleportConfig;
import com.hyperessentials.data.PlayerTeleportData;
import com.hyperessentials.data.TeleportRequest;
import com.hyperessentials.integration.PermissionManager;
import com.hyperessentials.storage.PlayerDataStorage;
import com.hyperessentials.util.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages TPA (teleport ask) requests between players.
 */
public class TpaManager {

    private final PlayerDataStorage storage;
    private final TeleportConfig config;
    private final Map<UUID, PlayerTeleportData> playerCache;
    private final Map<UUID, List<TeleportRequest>> incomingRequests; // target -> list of requests
    private final Map<UUID, TeleportRequest> outgoingRequests; // requester -> their pending request

    public TpaManager(@NotNull PlayerDataStorage storage, @NotNull TeleportConfig config) {
        this.storage = storage;
        this.config = config;
        this.playerCache = new ConcurrentHashMap<>();
        this.incomingRequests = new ConcurrentHashMap<>();
        this.outgoingRequests = new ConcurrentHashMap<>();
    }

    public CompletableFuture<PlayerTeleportData> loadPlayer(@NotNull UUID uuid, @NotNull String username) {
        return storage.loadPlayerData(uuid).thenApply(opt -> {
            PlayerTeleportData data = opt.orElseGet(() -> new PlayerTeleportData(uuid, username));
            data.setUsername(username);
            playerCache.put(uuid, data);
            Logger.debug("Loaded teleport data for %s", username);
            return data;
        });
    }

    public CompletableFuture<Void> savePlayer(@NotNull UUID uuid) {
        PlayerTeleportData data = playerCache.get(uuid);
        if (data == null) {
            return CompletableFuture.completedFuture(null);
        }
        return storage.savePlayerData(data);
    }

    public CompletableFuture<Void> unloadPlayer(@NotNull UUID uuid) {
        cancelOutgoingRequest(uuid);
        incomingRequests.remove(uuid);

        for (List<TeleportRequest> requests : incomingRequests.values()) {
            requests.removeIf(req -> req.requester().equals(uuid));
        }

        return savePlayer(uuid).thenRun(() -> {
            playerCache.remove(uuid);
            Logger.debug("Unloaded player %s from TPA cache", uuid);
        });
    }

    @Nullable
    public PlayerTeleportData getPlayerData(@NotNull UUID uuid) {
        return playerCache.get(uuid);
    }

    @NotNull
    public PlayerTeleportData getOrCreatePlayerData(@NotNull UUID uuid, @NotNull String username) {
        return playerCache.computeIfAbsent(uuid, k -> new PlayerTeleportData(uuid, username));
    }

    // ========== TPA Toggle ==========

    public boolean isAcceptingRequests(@NotNull UUID uuid) {
        PlayerTeleportData data = playerCache.get(uuid);
        return data == null || data.isTpToggle();
    }

    public boolean toggleTpToggle(@NotNull UUID uuid) {
        PlayerTeleportData data = playerCache.get(uuid);
        if (data == null) {
            return true;
        }
        boolean newState = data.toggleTpToggle();
        savePlayer(uuid);
        return newState;
    }

    // ========== TPA Requests ==========

    @Nullable
    public TeleportRequest createRequest(@NotNull UUID requesterUuid, @NotNull UUID targetUuid,
                                         @NotNull TeleportRequest.Type type) {
        if (!isAcceptingRequests(targetUuid)) {
            if (!PermissionManager.get().hasPermission(requesterUuid, Permissions.BYPASS_TOGGLE)) {
                return null;
            }
        }

        cancelOutgoingRequest(requesterUuid);

        List<TeleportRequest> targetIncoming = incomingRequests.computeIfAbsent(targetUuid, k -> new ArrayList<>());
        cleanupExpiredRequests(targetIncoming);

        if (targetIncoming.size() >= config.getMaxPendingTpa()) {
            return null;
        }

        PlayerTeleportData requesterData = playerCache.get(requesterUuid);
        if (requesterData != null) {
            long lastRequest = requesterData.getLastTpaRequest();
            long cooldownMs = config.getTpaCooldown() * 1000L;
            if (System.currentTimeMillis() - lastRequest < cooldownMs) {
                return null;
            }
        }

        TeleportRequest request = TeleportRequest.create(requesterUuid, targetUuid, type, config.getTpaTimeout());

        targetIncoming.add(request);
        outgoingRequests.put(requesterUuid, request);

        if (requesterData != null) {
            requesterData.setLastTpaRequest(System.currentTimeMillis());
            savePlayer(requesterUuid);
        }

        Logger.debug("TPA request created: %s -> %s (%s)", requesterUuid, targetUuid, type);
        return request;
    }

    @NotNull
    public List<TeleportRequest> getIncomingRequests(@NotNull UUID uuid) {
        List<TeleportRequest> requests = incomingRequests.get(uuid);
        if (requests == null) {
            return new ArrayList<>();
        }
        cleanupExpiredRequests(requests);
        return new ArrayList<>(requests);
    }

    @Nullable
    public TeleportRequest getIncomingRequest(@NotNull UUID targetUuid, @NotNull UUID requesterUuid) {
        List<TeleportRequest> requests = incomingRequests.get(targetUuid);
        if (requests == null) {
            return null;
        }
        cleanupExpiredRequests(requests);
        for (TeleportRequest req : requests) {
            if (req.requester().equals(requesterUuid) && !req.isExpired()) {
                return req;
            }
        }
        return null;
    }

    @Nullable
    public TeleportRequest getMostRecentIncomingRequest(@NotNull UUID uuid) {
        List<TeleportRequest> requests = incomingRequests.get(uuid);
        if (requests == null || requests.isEmpty()) {
            return null;
        }
        cleanupExpiredRequests(requests);
        if (requests.isEmpty()) {
            return null;
        }
        return requests.get(requests.size() - 1);
    }

    @Nullable
    public TeleportRequest getOutgoingRequest(@NotNull UUID uuid) {
        TeleportRequest request = outgoingRequests.get(uuid);
        if (request != null && request.isExpired()) {
            outgoingRequests.remove(uuid);
            return null;
        }
        return request;
    }

    public void acceptRequest(@NotNull TeleportRequest request) {
        removeRequest(request);
        Logger.debug("TPA request accepted: %s -> %s", request.requester(), request.target());
    }

    public void denyRequest(@NotNull TeleportRequest request) {
        removeRequest(request);
        Logger.debug("TPA request denied: %s -> %s", request.requester(), request.target());
    }

    @Nullable
    public TeleportRequest cancelOutgoingRequest(@NotNull UUID uuid) {
        TeleportRequest request = outgoingRequests.remove(uuid);
        if (request != null) {
            List<TeleportRequest> targetIncoming = incomingRequests.get(request.target());
            if (targetIncoming != null) {
                targetIncoming.remove(request);
            }
            Logger.debug("TPA request cancelled: %s -> %s", request.requester(), request.target());
        }
        return request;
    }

    private void removeRequest(@NotNull TeleportRequest request) {
        outgoingRequests.remove(request.requester());
        List<TeleportRequest> targetIncoming = incomingRequests.get(request.target());
        if (targetIncoming != null) {
            targetIncoming.remove(request);
        }
    }

    private void cleanupExpiredRequests(@NotNull List<TeleportRequest> requests) {
        Iterator<TeleportRequest> iter = requests.iterator();
        while (iter.hasNext()) {
            TeleportRequest req = iter.next();
            if (req.isExpired()) {
                iter.remove();
                outgoingRequests.remove(req.requester());
            }
        }
    }

    public long getRemainingTpaCooldown(@NotNull UUID uuid) {
        PlayerTeleportData data = playerCache.get(uuid);
        if (data == null) {
            return 0;
        }
        long lastRequest = data.getLastTpaRequest();
        if (lastRequest == 0) {
            return 0;
        }
        long cooldownMs = config.getTpaCooldown() * 1000L;
        long elapsed = System.currentTimeMillis() - lastRequest;
        return Math.max(0, cooldownMs - elapsed);
    }

    public boolean hasPendingIncoming(@NotNull UUID uuid) {
        List<TeleportRequest> requests = incomingRequests.get(uuid);
        if (requests == null || requests.isEmpty()) {
            return false;
        }
        cleanupExpiredRequests(requests);
        return !requests.isEmpty();
    }

    public CompletableFuture<Void> saveAll() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (UUID uuid : playerCache.keySet()) {
            futures.add(savePlayer(uuid));
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }
}
