package com.tekilo.network;

import com.tekilo.FactionManager;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record ZoneVisualizationPayload(List<ZoneVisualizationData> zones) implements CustomPayload {
    public static final CustomPayload.Id<ZoneVisualizationPayload> ID =
        new CustomPayload.Id<>(Identifier.of("tekilo", "zone_visualization"));

    public static final PacketCodec<RegistryByteBuf, ZoneVisualizationPayload> CODEC =
        PacketCodec.of(ZoneVisualizationPayload::write, ZoneVisualizationPayload::read);

    private void write(RegistryByteBuf buf) {
        buf.writeInt(zones.size());
        for (ZoneVisualizationData zone : zones) {
            buf.writeBlockPos(zone.center);
            buf.writeInt(zone.radius);
            buf.writeString(zone.ownerFaction.name());
            buf.writeString(zone.capturingFaction.name());
            buf.writeFloat(zone.captureProgress); // 0.0 to 1.0
            buf.writeString(zone.zoneName);
            buf.writeBoolean(zone.enabled);
        }
    }

    private static ZoneVisualizationPayload read(RegistryByteBuf buf) {
        int size = buf.readInt();
        List<ZoneVisualizationData> zones = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            BlockPos center = buf.readBlockPos();
            int radius = buf.readInt();
            FactionManager.Faction owner = FactionManager.Faction.valueOf(buf.readString());
            FactionManager.Faction capturing = FactionManager.Faction.valueOf(buf.readString());
            float progress = buf.readFloat();
            String name = buf.readString();
            boolean enabled = buf.readBoolean();
            zones.add(new ZoneVisualizationData(center, radius, owner, capturing, progress, name, enabled));
        }
        return new ZoneVisualizationPayload(zones);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public record ZoneVisualizationData(
        BlockPos center,
        int radius,
        FactionManager.Faction ownerFaction,
        FactionManager.Faction capturingFaction,
        float captureProgress,
        String zoneName,
        boolean enabled
    ) {}
}
