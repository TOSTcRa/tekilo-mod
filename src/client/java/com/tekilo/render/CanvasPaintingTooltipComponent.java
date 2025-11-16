package com.tekilo.render;

import com.tekilo.ModDataComponents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;

@Environment(EnvType.CLIENT)
public class CanvasPaintingTooltipComponent implements TooltipComponent {
    private static final int PREVIEW_SIZE = 48; // 48x48 pixel preview
    private static final int PIXEL_SIZE = 3; // Each canvas pixel is 3x3 in preview
    private final int[] pixels;

    public CanvasPaintingTooltipComponent(int[] pixels) {
        this.pixels = pixels;
    }

    @Override
    public int getHeight(TextRenderer textRenderer) {
        return PREVIEW_SIZE + 4; // Preview size + padding
    }

    @Override
    public int getWidth(TextRenderer textRenderer) {
        return PREVIEW_SIZE + 4; // Preview size + padding
    }

    @Override
    public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context) {
        if (pixels == null || pixels.length != 256) return;

        // Draw border
        context.fill(x, y, x + PREVIEW_SIZE + 4, y + PREVIEW_SIZE + 4, 0xFF000000);
        context.fill(x + 1, y + 1, x + PREVIEW_SIZE + 3, y + PREVIEW_SIZE + 3, 0xFF373737);

        // Draw pixels
        for (int py = 0; py < 16; py++) {
            for (int px = 0; px < 16; px++) {
                int color = pixels[py * 16 + px];
                int drawX = x + 2 + px * PIXEL_SIZE;
                int drawY = y + 2 + py * PIXEL_SIZE;

                // Draw pixel with alpha channel
                context.fill(drawX, drawY, drawX + PIXEL_SIZE, drawY + PIXEL_SIZE, 0xFF000000 | color);
            }
        }
    }

    // Data class for tooltip
    public record Data(int[] pixels) implements TooltipData {
    }
}
