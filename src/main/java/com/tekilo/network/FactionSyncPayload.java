package com.tekilo.network;

import com.tekilo.FactionManager;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record FactionSyncPayload(String factionName) implements CustomPayload {
    public static final CustomPayload.Id<FactionSyncPayload> ID =
        new CustomPayload.Id<>(Identifier.of("tekilo", "faction_sync"));

    public static final PacketCodec<RegistryByteBuf, FactionSyncPayload> CODEC =
        PacketCodec.tuple(
            PacketCodecs.STRING, FactionSyncPayload::factionName,
            FactionSyncPayload::new
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public FactionManager.Faction getFaction() {
        try {
            return FactionManager.Faction.valueOf(factionName);
        } catch (IllegalArgumentException e) {
            return FactionManager.Faction.NONE;
        }
    }
}
