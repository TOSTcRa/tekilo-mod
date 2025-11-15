package com.tekilo.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.UUID;

/**
 * Пакет для синхронизации анимаций между клиентами
 */
public record PlayAnimationPayload(UUID playerId, String animationName, boolean loop) implements CustomPayload {
    public static final CustomPayload.Id<PlayAnimationPayload> ID =
        new CustomPayload.Id<>(Identifier.of("tekilo", "play_animation"));

    public static final PacketCodec<RegistryByteBuf, PlayAnimationPayload> CODEC =
        PacketCodec.tuple(
            Uuids.PACKET_CODEC, PlayAnimationPayload::playerId,
            PacketCodecs.STRING, PlayAnimationPayload::animationName,
            PacketCodecs.BOOLEAN, PlayAnimationPayload::loop,
            PlayAnimationPayload::new
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
