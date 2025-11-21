package com.tekilo.render;

import com.tekilo.FactionManager;
import com.tekilo.network.ZoneVisualizationPayload.ZoneVisualizationData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class ZoneVisualizationRenderer {

    public static void renderHud(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        List<ZoneVisualizationData> zones = ZoneVisualizationManager.getZones();
        if (zones.isEmpty()) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        Vec3d cameraPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());

        // Find closest zone to player
        ZoneVisualizationData closestZone = null;
        double closestDistance = Double.MAX_VALUE;

        for (ZoneVisualizationData zone : zones) {
            if (!zone.enabled()) {
                continue;
            }

            BlockPos center = zone.center();
            double distance = cameraPos.squaredDistanceTo(center.getX() + 0.5, cameraPos.y, center.getZ() + 0.5);

            // Only show if within visual range (500 blocks)
            if (distance < 250000 && distance < closestDistance) {
                closestDistance = distance;
                closestZone = zone;
            }
        }

        if (closestZone != null) {
            renderZoneInfo(context, textRenderer, closestZone);
        }
    }

    private static void renderZoneInfo(DrawContext context, TextRenderer textRenderer, ZoneVisualizationData zone) {
        int width = context.getScaledWindowWidth();
        int y = 35; // Start position from top (below boss bar)

        String zoneName = zone.zoneName().isEmpty() ? "Zone" : zone.zoneName();
        String ownerText = getOwnerText(zone);
        String progressText = getProgressText(zone);

        // Zone name
        int zoneNameColor = getZoneNameColor(zone);
        drawCenteredText(context, textRenderer, zoneName, width / 2, y, zoneNameColor);
        y += 12;

        // Owner status
        int ownerColor = getOwnerColor(zone.ownerFaction());
        drawCenteredText(context, textRenderer, ownerText, width / 2, y, ownerColor);
        y += 12;

        // Capture progress if capturing
        if (zone.capturingFaction() != FactionManager.Faction.NONE && zone.captureProgress() > 0) {
            drawCenteredText(context, textRenderer, progressText, width / 2, y, 0xFFFF00);

            // Progress bar
            y += 12;
            int barWidth = 200;
            int barHeight = 10;
            int barX = (width - barWidth) / 2;

            // Background
            context.fill(barX - 1, y - 1, barX + barWidth + 1, y + barHeight + 1, 0xFF000000);
            context.fill(barX, y, barX + barWidth, y + barHeight, 0xFF333333);

            // Progress fill
            int fillColor = getFactionColor(zone.capturingFaction());
            int fillWidth = (int) (barWidth * zone.captureProgress());
            context.fill(barX, y, barX + fillWidth, y + barHeight, fillColor);
        }
    }

    private static void drawCenteredText(DrawContext context, TextRenderer textRenderer, String text, int centerX, int y, int color) {
        int textWidth = textRenderer.getWidth(text);
        int x = centerX - textWidth / 2;

        // Background
        context.fill(x - 2, y - 2, x + textWidth + 2, y + 10, 0x80000000);

        // Text
        context.drawText(textRenderer, text, x, y, color, false);
    }

    private static String getOwnerText(ZoneVisualizationData zone) {
        if (zone.ownerFaction() == FactionManager.Faction.NONE) {
            return "Uncaptured";
        }
        return switch (zone.ownerFaction()) {
            case COMMUNIST -> "Controlled by Communists";
            case CAPITALIST -> "Controlled by Capitalists";
            default -> "Unknown";
        };
    }

    private static String getProgressText(ZoneVisualizationData zone) {
        int percent = (int) (zone.captureProgress() * 100);
        String capturingFaction = zone.capturingFaction() == FactionManager.Faction.COMMUNIST ? "Communists" : "Capitalists";
        return capturingFaction + " capturing: " + percent + "%";
    }

    private static int getZoneNameColor(ZoneVisualizationData zone) {
        // Blend color based on capture progress
        int baseColor = zone.ownerFaction() == FactionManager.Faction.NONE ? 0xFFFFFF : getFactionColor(zone.ownerFaction());

        if (zone.capturingFaction() != FactionManager.Faction.NONE && zone.captureProgress() > 0) {
            int targetColor = getFactionColor(zone.capturingFaction());
            return blendColors(baseColor, targetColor, zone.captureProgress());
        }

        return baseColor;
    }

    private static int getOwnerColor(FactionManager.Faction faction) {
        return switch (faction) {
            case COMMUNIST -> 0xFF5555; // Light red
            case CAPITALIST -> 0xFFFF55; // Light yellow
            case NONE -> 0xAAAAAA; // Gray
        };
    }

    private static int getFactionColor(FactionManager.Faction faction) {
        return switch (faction) {
            case COMMUNIST -> 0xFFFF0000; // Red
            case CAPITALIST -> 0xFFFFFF00; // Yellow
            case NONE -> 0xFFFFFFFF; // White
        };
    }

    private static int blendColors(int color1, int color2, float ratio) {
        ratio = Math.max(0, Math.min(1, ratio));

        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (0xFF << 24) | (r << 16) | (g << 8) | b;
    }
}
