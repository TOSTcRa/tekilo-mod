package com.tekilo;

import com.tekilo.network.FactionSyncPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FactionManager {
    public enum Faction {
        COMMUNIST,
        CAPITALIST,
        NONE
    }

    private static final Map<UUID, Faction> playerFactions = new HashMap<>();

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
}
