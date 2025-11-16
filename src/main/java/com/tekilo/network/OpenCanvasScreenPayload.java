package com.tekilo.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public record OpenCanvasScreenPayload(BlockPos pos, int[] pixels, int canvasWidth, int canvasHeight, boolean sizeChosen) implements CustomPayload {
    public static final CustomPayload.Id<OpenCanvasScreenPayload> ID = new CustomPayload.Id<>(Identifier.of("tekilo", "open_canvas_screen"));

    public static final PacketCodec<RegistryByteBuf, OpenCanvasScreenPayload> CODEC =
        PacketCodec.of(OpenCanvasScreenPayload::write, OpenCanvasScreenPayload::read);

    private static OpenCanvasScreenPayload read(RegistryByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int canvasWidth = buf.readInt();
        int canvasHeight = buf.readInt();
        boolean sizeChosen = buf.readBoolean();
        int pixelCount = canvasWidth * 16 * canvasHeight * 16;
        int[] pixels = new int[pixelCount];
        for (int i = 0; i < pixelCount; i++) {
            pixels[i] = buf.readInt();
        }
        return new OpenCanvasScreenPayload(pos, pixels, canvasWidth, canvasHeight, sizeChosen);
    }

    private void write(RegistryByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(canvasWidth);
        buf.writeInt(canvasHeight);
        buf.writeBoolean(sizeChosen);
        for (int pixel : pixels) {
            buf.writeInt(pixel);
        }
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
