package com.tekilo.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record ZoneSettingsPayload(
    BlockPos pos,
    int zoneRadius,
    int baseCaptureTime,
    int minCaptureTime,
    boolean zoneEnabled,
    String zoneName,
    int bossBarColor,
    String captureReward
) implements CustomPayload {
    public static final Id<ZoneSettingsPayload> ID = new Id<>(Identifier.of("tekilo", "zone_settings"));

    public static final PacketCodec<RegistryByteBuf, ZoneSettingsPayload> CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC, ZoneSettingsPayload::pos,
        PacketCodecs.INTEGER, ZoneSettingsPayload::zoneRadius,
        PacketCodecs.INTEGER, ZoneSettingsPayload::baseCaptureTime,
        PacketCodecs.INTEGER, ZoneSettingsPayload::minCaptureTime,
        PacketCodecs.BOOLEAN, ZoneSettingsPayload::zoneEnabled,
        PacketCodecs.STRING, ZoneSettingsPayload::zoneName,
        PacketCodecs.INTEGER, ZoneSettingsPayload::bossBarColor,
        PacketCodecs.STRING, ZoneSettingsPayload::captureReward,
        ZoneSettingsPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
