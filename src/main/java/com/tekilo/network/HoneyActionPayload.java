package com.tekilo.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record HoneyActionPayload(int entityId, String action) implements CustomPayload {
    public static final CustomPayload.Id<HoneyActionPayload> ID =
        new CustomPayload.Id<>(Identifier.of("tekilo", "honey_action"));

    public static final PacketCodec<RegistryByteBuf, HoneyActionPayload> CODEC =
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, HoneyActionPayload::entityId,
            PacketCodecs.STRING, HoneyActionPayload::action,
            HoneyActionPayload::new
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public enum Action {
        MOUTH,
        BACK
    }

    public Action getAction() {
        return action.equals("MOUTH") ? Action.MOUTH : Action.BACK;
    }
}
