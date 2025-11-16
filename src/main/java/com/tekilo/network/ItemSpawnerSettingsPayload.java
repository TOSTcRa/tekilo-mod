package com.tekilo.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ItemSpawnerSettingsPayload(
    BlockPos pos,
    int radius,
    int spawnInterval,
    int itemCount,
    boolean spawnInChests,
    boolean useGlobalSettings,
    boolean enabled
) implements CustomPayload {
    public static final Id<ItemSpawnerSettingsPayload> ID = new Id<>(Identifier.of("tekilo", "item_spawner_settings"));

    public static final PacketCodec<RegistryByteBuf, ItemSpawnerSettingsPayload> CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC, ItemSpawnerSettingsPayload::pos,
        PacketCodecs.INTEGER, ItemSpawnerSettingsPayload::radius,
        PacketCodecs.INTEGER, ItemSpawnerSettingsPayload::spawnInterval,
        PacketCodecs.INTEGER, ItemSpawnerSettingsPayload::itemCount,
        PacketCodecs.BOOLEAN, ItemSpawnerSettingsPayload::spawnInChests,
        PacketCodecs.BOOLEAN, ItemSpawnerSettingsPayload::useGlobalSettings,
        PacketCodecs.BOOLEAN, ItemSpawnerSettingsPayload::enabled,
        ItemSpawnerSettingsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
