package com.tekilo.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record CanvasUpdatePayload(BlockPos pos, int[] pixels, int canvasWidth, int canvasHeight) implements CustomPayload {
    public static final CustomPayload.Id<CanvasUpdatePayload> ID =
        new CustomPayload.Id<>(Identifier.of("tekilo", "canvas_update"));

    public static final PacketCodec<RegistryByteBuf, CanvasUpdatePayload> CODEC =
        PacketCodec.of(CanvasUpdatePayload::write, CanvasUpdatePayload::read);

    private static CanvasUpdatePayload read(RegistryByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int canvasWidth = buf.readVarInt();
        int canvasHeight = buf.readVarInt();

        // Validate dimensions to prevent integer overflow and DoS
        if (canvasWidth < 1 || canvasWidth > 6 || canvasHeight < 1 || canvasHeight > 6) {
            throw new IllegalArgumentException("Invalid canvas dimensions: " + canvasWidth + "x" + canvasHeight);
        }

        int pixelCount = canvasWidth * 16 * canvasHeight * 16;
        int[] pixels = new int[pixelCount];
        for (int i = 0; i < pixelCount; i++) {
            pixels[i] = buf.readInt();
        }
        return new CanvasUpdatePayload(pos, pixels, canvasWidth, canvasHeight);
    }

    private void write(RegistryByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeVarInt(canvasWidth);
        buf.writeVarInt(canvasHeight);
        for (int i = 0; i < pixels.length; i++) {
            buf.writeInt(pixels[i]);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
