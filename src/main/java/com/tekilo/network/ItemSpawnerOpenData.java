package com.tekilo.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.math.BlockPos;

public record ItemSpawnerOpenData(
    BlockPos pos,
    String zoneName,
    String captureReward
) {
    public static final PacketCodec<RegistryByteBuf, ItemSpawnerOpenData> CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC, ItemSpawnerOpenData::pos,
        PacketCodecs.STRING, ItemSpawnerOpenData::zoneName,
        PacketCodecs.STRING, ItemSpawnerOpenData::captureReward,
        ItemSpawnerOpenData::new
    );
}
