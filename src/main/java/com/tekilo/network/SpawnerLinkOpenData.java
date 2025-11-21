package com.tekilo.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

public record SpawnerLinkOpenData(
    BlockPos parentPos,
    BlockPos childPos
) {
    public static final PacketCodec<RegistryByteBuf, SpawnerLinkOpenData> CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC, SpawnerLinkOpenData::parentPos,
        BlockPos.PACKET_CODEC, SpawnerLinkOpenData::childPos,
        SpawnerLinkOpenData::new
    );
}
