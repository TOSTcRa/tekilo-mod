package com.tekilo.render;

import com.tekilo.network.ZoneVisualizationPayload.ZoneVisualizationData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ZoneVisualizationManager {
    private static final List<ZoneVisualizationData> zones = Collections.synchronizedList(new ArrayList<>());
    private static final Object LOCK = new Object();

    public static void updateZones(List<ZoneVisualizationData> newZones) {
        synchronized (LOCK) {
            zones.clear();
            if (newZones != null && !newZones.isEmpty()) {
                zones.addAll(newZones);
            }
        }
    }

    public static List<ZoneVisualizationData> getZones() {
        synchronized (LOCK) {
            // Return a copy to avoid concurrent modification
            return new ArrayList<>(zones);
        }
    }

    public static void clear() {
        synchronized (LOCK) {
            zones.clear();
        }
    }
}
