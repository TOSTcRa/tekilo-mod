package com.tekilo.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record SpawnerLinkPayload(
    BlockPos parentPos,
    BlockPos childPos,
    int unlockDelaySeconds
) implements CustomPayload {
    public static final Id<SpawnerLinkPayload> ID = new Id<>(Identifier.of("tekilo", "spawner_link"));

    public static final PacketCodec<RegistryByteBuf, SpawnerLinkPayload> CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC, SpawnerLinkPayload::parentPos,
        BlockPos.PACKET_CODEC, SpawnerLinkPayload::childPos,
        PacketCodecs.INTEGER, SpawnerLinkPayload::unlockDelaySeconds,
        SpawnerLinkPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
