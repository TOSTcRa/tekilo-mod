package com.tekilo.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Пакет для синхронизации воспроизведения анимации игрока
 * Отправляется с сервера на клиенты
 */
public record PlayAnimationPayload(UUID playerUuid, String animationName, int duration) implements CustomPayload {
    public static final CustomPayload.Id<PlayAnimationPayload> ID =
        new CustomPayload.Id<>(Identifier.of("tekilo", "play_animation"));

    public static final PacketCodec<RegistryByteBuf, PlayAnimationPayload> CODEC = PacketCodec.tuple(
        Uuids.PACKET_CODEC, PlayAnimationPayload::playerUuid,
        PacketCodecs.STRING, PlayAnimationPayload::animationName,
        PacketCodecs.VAR_INT, PlayAnimationPayload::duration,
        PlayAnimationPayload::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
