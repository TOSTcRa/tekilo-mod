package com.tekilo.screen;

import com.tekilo.network.CanvasSizePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class CanvasSizeScreen extends Screen {
    private final BlockPos canvasPos;
    private int selectedWidth = 1;
    private int selectedHeight = 1;

    public CanvasSizeScreen(BlockPos pos) {
        super(Text.translatable("gui.tekilo.canvas.choose_size"));
        this.canvasPos = pos;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 80;

        // Размеры от 1x1 до 6x6
        String[] sizes = {"1x1", "2x2", "3x3", "4x4", "5x5", "6x6"};
        int btnWidth = 60;
        int btnHeight = 20;
        int gap = 10;

        // Первый ряд: 1x1, 2x2, 3x3
        for (int i = 0; i < 3; i++) {
            int size = i + 1;
            int x = centerX - (3 * btnWidth + 2 * gap) / 2 + i * (btnWidth + gap);
            this.addDrawableChild(ButtonWidget.builder(Text.literal(sizes[i]), btn -> selectSize(size, size))
                .dimensions(x, startY, btnWidth, btnHeight).build());
        }

        // Второй ряд: 4x4, 5x5, 6x6
        for (int i = 0; i < 3; i++) {
            int size = i + 4;
            int x = centerX - (3 * btnWidth + 2 * gap) / 2 + i * (btnWidth + gap);
            this.addDrawableChild(ButtonWidget.builder(Text.literal(sizes[i + 3]), btn -> selectSize(size, size))
                .dimensions(x, startY + btnHeight + gap, btnWidth, btnHeight).build());
        }

        // Кнопка подтверждения
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.tekilo.canvas.confirm"), btn -> confirm())
            .dimensions(centerX - 50, startY + 120, 100, 20).build());

        // Кнопка отмены
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), btn -> close())
            .dimensions(centerX - 50, startY + 145, 100, 20).build());
    }

    private void selectSize(int width, int height) {
        this.selectedWidth = width;
        this.selectedHeight = height;
    }

    private void confirm() {
        // Отправляем размер на сервер
        CanvasSizePayload payload = new CanvasSizePayload(canvasPos, selectedWidth, selectedHeight);
        ClientPlayNetworking.send(payload);
        close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Заголовок
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 30, 0xFFFFFF);

        // Инструкция
        Text instruction = Text.translatable("gui.tekilo.canvas.size_instruction");
        context.drawCenteredTextWithShadow(this.textRenderer, instruction, this.width / 2, 50, 0xAAAAAA);

        // Выбранный размер
        Text selected = Text.translatable("gui.tekilo.canvas.selected_size", selectedWidth, selectedHeight, selectedWidth * 16, selectedHeight * 16);
        context.drawCenteredTextWithShadow(this.textRenderer, selected, this.width / 2, 165, 0x55FF55);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
