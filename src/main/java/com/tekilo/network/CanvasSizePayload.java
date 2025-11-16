package com.tekilo.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record CanvasSizePayload(BlockPos pos, int width, int height) implements CustomPayload {
    public static final CustomPayload.Id<CanvasSizePayload> ID = new CustomPayload.Id<>(Identifier.of("tekilo", "canvas_size"));

    public static final PacketCodec<RegistryByteBuf, CanvasSizePayload> CODEC = PacketCodec.tuple(
        BlockPos.PACKET_CODEC, CanvasSizePayload::pos,
        PacketCodecs.INTEGER, CanvasSizePayload::width,
        PacketCodecs.INTEGER, CanvasSizePayload::height,
        CanvasSizePayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
