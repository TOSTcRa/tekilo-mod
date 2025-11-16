package com.tekilo;

import com.tekilo.network.FactionSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FactionManager {
    public enum Faction {
        COMMUNIST,
        CAPITALIST,
        NONE
    }

    public enum PlayerType {
        NORMAL,
        SPY
    }

    // Use ConcurrentHashMap for thread-safe access from multiple threads
    private static final Map<UUID, Faction> playerFactions = new ConcurrentHashMap<>();
    private static final Map<UUID, PlayerType> playerTypes = new ConcurrentHashMap<>();

    public static void setPlayerFaction(UUID playerId, Faction faction) {
        playerFactions.put(playerId, faction);
    }

    public static Faction getPlayerFaction(UUID playerId) {
        return playerFactions.getOrDefault(playerId, Faction.NONE);
    }

    public static boolean isCommunist(UUID playerId) {
        return getPlayerFaction(playerId) == Faction.COMMUNIST;
    }

    public static boolean isCapitalist(UUID playerId) {
        return getPlayerFaction(playerId) == Faction.CAPITALIST;
    }

    public static Map<UUID, Faction> getAllFactions() {
        return new HashMap<>(playerFactions);
    }

    public static void syncToClient(ServerPlayerEntity player) {
        Faction faction = getPlayerFaction(player.getUuid());
        FactionSyncPayload payload = new FactionSyncPayload(faction.name());
        ServerPlayNetworking.send(player, payload);
    }

    // Методы для работы со шпионами
    public static void setPlayerType(UUID playerId, PlayerType type) {
        playerTypes.put(playerId, type);
    }

    public static PlayerType getPlayerType(UUID playerId) {
        return playerTypes.getOrDefault(playerId, PlayerType.NORMAL);
    }

    public static boolean isSpy(UUID playerId) {
        return getPlayerType(playerId) == PlayerType.SPY;
    }

    public static void setPlayerAsSpy(UUID playerId, Faction faction) {
        setPlayerFaction(playerId, faction);
        setPlayerType(playerId, PlayerType.SPY);
    }

    public static Map<UUID, PlayerType> getAllPlayerTypes() {
        return new HashMap<>(playerTypes);
    }
}
