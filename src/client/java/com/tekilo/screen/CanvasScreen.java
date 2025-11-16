package com.tekilo.screen;

import com.tekilo.network.CanvasUpdatePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Environment(EnvType.CLIENT)
public class CanvasScreen extends Screen {
    private static final int MAX_CANVAS_DISPLAY_SIZE = 400; // Максимальный размер отображения холста в пикселях экрана

    // Extended palette with more colors
    private static final int[] PALETTE = {
        // Row 1 - Basic colors
        0xFFFFFF, 0xC0C0C0, 0x808080, 0x404040, 0x000000,
        0xFF0000, 0xFF8000, 0xFFFF00, 0x80FF00, 0x00FF00,
        0x00FF80, 0x00FFFF, 0x0080FF, 0x0000FF, 0x8000FF,
        0xFF00FF,
        // Row 2 - Lighter shades
        0xFFCCCC, 0xFFE0CC, 0xFFFFCC, 0xE0FFCC, 0xCCFFCC,
        0xCCFFE0, 0xCCFFFF, 0xCCE0FF, 0xCCCCFF, 0xE0CCFF,
        0xFFCCFF, 0xFFCCE0, 0x996633, 0x663300, 0x339966,
        0x006633
    };

    private final BlockPos canvasPos;
    private final int[] pixels;
    private final int[] undoBuffer;
    private final int canvasWidth; // В блоках (1-6)
    private final int canvasHeight; // В блоках (1-6)
    private final int pixelWidth; // В пикселях (16-96)
    private final int pixelHeight; // В пикселях (16-96)
    private int pixelSize; // Размер одного пикселя на экране (динамический)
    private int selectedColor = 0x000000;

    // Tools
    private enum Tool { BRUSH, FILL, ERASER }
    private Tool currentTool = Tool.BRUSH;

    // Buttons
    private ButtonWidget brushBtn, fillBtn, eraserBtn;
    private ButtonWidget clearBtn, undoBtn, saveBtn;

    public CanvasScreen(BlockPos pos, int[] pixels, int canvasWidth, int canvasHeight) {
        super(Text.translatable("block.tekilo.canvas"));
        this.canvasPos = pos;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.pixelWidth = canvasWidth * 16;
        this.pixelHeight = canvasHeight * 16;
        this.pixels = pixels.clone();
        this.undoBuffer = pixels.clone();

        // Рассчитываем размер пикселя чтобы холст поместился на экран
        // Учитываем место для UI элементов (палитра + кнопки ~120px снизу, заголовок ~50px сверху)
        int maxHeight = 300; // Оставляем место для UI
        this.pixelSize = Math.max(1, Math.min(MAX_CANVAS_DISPLAY_SIZE / Math.max(pixelWidth, pixelHeight), maxHeight / pixelHeight));
    }

    @Override
    protected void init() {
        super.init();

        int canvasDisplayWidth = pixelWidth * pixelSize;
        int canvasDisplayHeight = pixelHeight * pixelSize;
        int canvasStartX = (this.width - canvasDisplayWidth) / 2;
        int canvasStartY = 50;
        int toolbarX = canvasStartX - 35;
        int toolbarY = canvasStartY;

        // === TOOL BUTTONS (Left side) ===
        this.brushBtn = ButtonWidget.builder(Text.literal("B"), btn -> selectTool(Tool.BRUSH))
            .dimensions(toolbarX, toolbarY, 25, 25).build();
        this.fillBtn = ButtonWidget.builder(Text.literal("F"), btn -> selectTool(Tool.FILL))
            .dimensions(toolbarX, toolbarY + 30, 25, 25).build();
        this.eraserBtn = ButtonWidget.builder(Text.literal("E"), btn -> selectTool(Tool.ERASER))
            .dimensions(toolbarX, toolbarY + 60, 25, 25).build();

        this.addDrawableChild(brushBtn);
        this.addDrawableChild(fillBtn);
        this.addDrawableChild(eraserBtn);

        // === ACTION BUTTONS (Right side) ===
        int actionX = canvasStartX + canvasDisplayWidth + 10;

        this.undoBtn = ButtonWidget.builder(Text.literal("<"), btn -> undo())
            .dimensions(actionX, toolbarY, 25, 25).build();
        this.clearBtn = ButtonWidget.builder(Text.literal("X"), btn -> clearCanvas())
            .dimensions(actionX, toolbarY + 30, 25, 25).build();

        this.addDrawableChild(undoBtn);
        this.addDrawableChild(clearBtn);

        // === SAVE BUTTON (Bottom) ===
        int bottomY = canvasStartY + canvasDisplayHeight + 65;
        // Убеждаемся что кнопка видна на экране
        if (bottomY > this.height - 25) {
            bottomY = this.height - 25;
        }
        this.saveBtn = ButtonWidget.builder(
            Text.translatable("gui.tekilo.canvas.save"),
            button -> saveAndClose()
        ).dimensions(this.width / 2 - 60, bottomY, 120, 20).build();
        this.addDrawableChild(saveBtn);
    }

    private void selectTool(Tool tool) {
        this.currentTool = tool;
    }

    private void saveUndo() {
        if (pixels.length == undoBuffer.length) {
            System.arraycopy(pixels, 0, undoBuffer, 0, pixels.length);
        }
    }

    private void undo() {
        if (pixels.length == undoBuffer.length) {
            System.arraycopy(undoBuffer, 0, pixels, 0, pixels.length);
        }
    }

    private void clearCanvas() {
        saveUndo();
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0xFFFFFF;
        }
    }

    private void floodFill(int startX, int startY, int targetColor, int replacementColor) {
        if (targetColor == replacementColor) return;
        if (startX < 0 || startX >= pixelWidth || startY < 0 || startY >= pixelHeight) return;
        if (pixels[startY * pixelWidth + startX] != targetColor) return;

        // Use iterative BFS instead of recursion to avoid StackOverflowError
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int x = pos[0];
            int y = pos[1];

            if (x < 0 || x >= pixelWidth || y < 0 || y >= pixelHeight) continue;
            if (pixels[y * pixelWidth + x] != targetColor) continue;

            pixels[y * pixelWidth + x] = replacementColor;

            queue.add(new int[]{x + 1, y});
            queue.add(new int[]{x - 1, y});
            queue.add(new int[]{x, y + 1});
            queue.add(new int[]{x, y - 1});
        }
    }

    private void saveAndClose() {
        CanvasUpdatePayload payload = new CanvasUpdatePayload(canvasPos, pixels, canvasWidth, canvasHeight);
        ClientPlayNetworking.send(payload);
        if (this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        int canvasDisplayWidth = pixelWidth * pixelSize;
        int canvasDisplayHeight = pixelHeight * pixelSize;
        int canvasStartX = (this.width - canvasDisplayWidth) / 2;
        int canvasStartY = 50;

        // === TITLE ===
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);

        // === INSTRUCTIONS ===
        Text instructions = Text.translatable("gui.tekilo.canvas.instructions");
        context.drawCenteredTextWithShadow(this.textRenderer, instructions, this.width / 2, 25, 0xAAAAAA);

        // === SIZE INFO ===
        Text sizeInfo = Text.literal(canvasWidth + "x" + canvasHeight + " blocks (" + pixelWidth + "x" + pixelHeight + " px)");
        context.drawCenteredTextWithShadow(this.textRenderer, sizeInfo, this.width / 2, 38, 0x55FF55);

        // === TOOL LABELS ===
        int toolbarX = canvasStartX - 35;
        context.drawText(this.textRenderer, Text.translatable("gui.tekilo.canvas.tools"), toolbarX - 5, canvasStartY - 12, 0xFFFFFF, false);

        // Highlight current tool
        int toolHighlightY = canvasStartY + (currentTool == Tool.BRUSH ? 0 : currentTool == Tool.FILL ? 30 : 60);
        context.fill(toolbarX - 2, toolHighlightY - 2, toolbarX + 27, toolHighlightY + 27, 0xFF55FF55);

        // === CANVAS BACKGROUND ===
        context.fill(canvasStartX - 3, canvasStartY - 3,
            canvasStartX + canvasDisplayWidth + 3, canvasStartY + canvasDisplayHeight + 3, 0xFF000000);

        // === DRAW CANVAS using RUN-LENGTH ENCODING (OPTIMIZED) ===
        // Instead of drawing each pixel separately, we group consecutive pixels with the same color
        // This reduces draw calls from pixelWidth*pixelHeight to much fewer
        for (int py = 0; py < pixelHeight; py++) {
            int px = 0;
            while (px < pixelWidth) {
                int color = pixels[py * pixelWidth + px];
                int runStart = px;

                // Find the end of this run (consecutive pixels with same color)
                while (px < pixelWidth && pixels[py * pixelWidth + px] == color) {
                    px++;
                }

                // Draw the entire run as a single rectangle
                int posX = canvasStartX + runStart * pixelSize;
                int posY = canvasStartY + py * pixelSize;
                int runWidth = (px - runStart) * pixelSize;

                context.fill(posX, posY, posX + runWidth, posY + pixelSize, 0xFF000000 | color);
            }
        }

        // Canvas white border
        drawBorderRect(context, canvasStartX - 1, canvasStartY - 1,
            canvasDisplayWidth + 2, canvasDisplayHeight + 2, 0xFFFFFFFF);

        // === PALETTE ===
        int paletteStartX = canvasStartX;
        int paletteStartY = canvasStartY + canvasDisplayHeight + 10;

        // Palette background
        context.fill(paletteStartX - 3, paletteStartY - 3,
            paletteStartX + 16 * 14 + 3, paletteStartY + 2 * 14 + 3, 0xFF202020);

        // Draw palette colors (2 rows of 16)
        for (int i = 0; i < PALETTE.length; i++) {
            int col = i % 16;
            int row = i / 16;
            int posX = paletteStartX + col * 14;
            int posY = paletteStartY + row * 14;

            context.fill(posX, posY, posX + 13, posY + 13, 0xFF000000 | PALETTE[i]);

            // Highlight selected
            if (PALETTE[i] == selectedColor) {
                drawBorderRect(context, posX - 1, posY - 1, 15, 15, 0xFFFF5555);
            }
        }

        // === CURRENT COLOR DISPLAY ===
        int colorDisplayX = paletteStartX + 16 * 14 + 10;
        context.drawText(this.textRenderer, Text.translatable("gui.tekilo.canvas.current_color"),
            colorDisplayX, paletteStartY - 3, 0xFFFFFF, false);

        // Large color preview
        context.fill(colorDisplayX, paletteStartY + 8, colorDisplayX + 30, paletteStartY + 38, 0xFF000000);
        context.fill(colorDisplayX + 1, paletteStartY + 9, colorDisplayX + 29, paletteStartY + 37, 0xFF000000 | selectedColor);

        // Color hex code
        String hexCode = String.format("#%06X", selectedColor);
        context.drawText(this.textRenderer, Text.literal(hexCode), colorDisplayX, paletteStartY + 42, 0xAAAAAA, false);

        // === TOOLTIPS ===
        List<Text> tooltip = new ArrayList<>();

        if (isMouseOver(mouseX, mouseY, brushBtn)) {
            tooltip.add(Text.translatable("gui.tekilo.canvas.tool.brush"));
            tooltip.add(Text.translatable("gui.tekilo.canvas.tool.brush.desc").copy().styled(s -> s.withColor(0xAAAAAA)));
        } else if (isMouseOver(mouseX, mouseY, fillBtn)) {
            tooltip.add(Text.translatable("gui.tekilo.canvas.tool.fill"));
            tooltip.add(Text.translatable("gui.tekilo.canvas.tool.fill.desc").copy().styled(s -> s.withColor(0xAAAAAA)));
        } else if (isMouseOver(mouseX, mouseY, eraserBtn)) {
            tooltip.add(Text.translatable("gui.tekilo.canvas.tool.eraser"));
            tooltip.add(Text.translatable("gui.tekilo.canvas.tool.eraser.desc").copy().styled(s -> s.withColor(0xAAAAAA)));
        } else if (isMouseOver(mouseX, mouseY, undoBtn)) {
            tooltip.add(Text.translatable("gui.tekilo.canvas.undo"));
        } else if (isMouseOver(mouseX, mouseY, clearBtn)) {
            tooltip.add(Text.translatable("gui.tekilo.canvas.clear"));
        }

        // Pixel coordinate tooltip when hovering canvas
        if (mouseX >= canvasStartX && mouseX < canvasStartX + canvasDisplayWidth &&
            mouseY >= canvasStartY && mouseY < canvasStartY + canvasDisplayHeight) {
            int px = (mouseX - canvasStartX) / pixelSize;
            int py = (mouseY - canvasStartY) / pixelSize;
            tooltip.add(Text.literal("X: " + px + ", Y: " + py));
        }

        if (!tooltip.isEmpty()) {
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    private void drawBorderRect(DrawContext context, int x, int y, int w, int h, int color) {
        context.fill(x, y, x + w, y + 1, color);
        context.fill(x, y + h - 1, x + w, y + h, color);
        context.fill(x, y, x + 1, y + h, color);
        context.fill(x + w - 1, y, x + w, y + h, color);
    }

    private boolean isMouseOver(int mouseX, int mouseY, ButtonWidget button) {
        return mouseX >= button.getX() && mouseX < button.getX() + button.getWidth() &&
               mouseY >= button.getY() && mouseY < button.getY() + button.getHeight();
    }

    @Override
    public boolean mouseClicked(Click click, boolean bl) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();

        int canvasDisplayWidth = pixelWidth * pixelSize;
        int canvasDisplayHeight = pixelHeight * pixelSize;
        int canvasStartX = (this.width - canvasDisplayWidth) / 2;
        int canvasStartY = 50;

        // Canvas interaction
        if (mouseX >= canvasStartX && mouseX < canvasStartX + canvasDisplayWidth &&
            mouseY >= canvasStartY && mouseY < canvasStartY + canvasDisplayHeight) {

            int px = (int) ((mouseX - canvasStartX) / pixelSize);
            int py = (int) ((mouseY - canvasStartY) / pixelSize);

            if (px >= 0 && px < pixelWidth && py >= 0 && py < pixelHeight) {
                if (button == 0) { // Left click
                    saveUndo();
                    switch (currentTool) {
                        case BRUSH -> pixels[py * pixelWidth + px] = selectedColor;
                        case FILL -> floodFill(px, py, pixels[py * pixelWidth + px], selectedColor);
                        case ERASER -> pixels[py * pixelWidth + px] = 0xFFFFFF;
                    }
                } else if (button == 1) { // Right click - pick color
                    selectedColor = pixels[py * pixelWidth + px];
                }
                return true;
            }
        }

        // Palette interaction
        int paletteStartX = canvasStartX;
        int paletteStartY = canvasStartY + canvasDisplayHeight + 10;

        for (int i = 0; i < PALETTE.length; i++) {
            int col = i % 16;
            int row = i / 16;
            int posX = paletteStartX + col * 14;
            int posY = paletteStartY + row * 14;

            if (mouseX >= posX && mouseX < posX + 13 && mouseY >= posY && mouseY < posY + 13) {
                selectedColor = PALETTE[i];
                return true;
            }
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (click.button() == 0 && (currentTool == Tool.BRUSH || currentTool == Tool.ERASER)) {
            double mouseX = click.x();
            double mouseY = click.y();

            int canvasDisplayWidth = pixelWidth * pixelSize;
            int canvasDisplayHeight = pixelHeight * pixelSize;
            int canvasStartX = (this.width - canvasDisplayWidth) / 2;
            int canvasStartY = 50;

            if (mouseX >= canvasStartX && mouseX < canvasStartX + canvasDisplayWidth &&
                mouseY >= canvasStartY && mouseY < canvasStartY + canvasDisplayHeight) {

                int px = (int) ((mouseX - canvasStartX) / pixelSize);
                int py = (int) ((mouseY - canvasStartY) / pixelSize);

                if (px >= 0 && px < pixelWidth && py >= 0 && py < pixelHeight) {
                    if (currentTool == Tool.BRUSH) {
                        pixels[py * pixelWidth + px] = selectedColor;
                    } else {
                        pixels[py * pixelWidth + px] = 0xFFFFFF;
                    }
                    return true;
                }
            }
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
