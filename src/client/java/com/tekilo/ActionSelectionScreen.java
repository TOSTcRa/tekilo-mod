package com.tekilo;

import com.tekilo.network.HoneyActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.entity.Entity;

public class ActionSelectionScreen extends Screen {
    private final Entity targetEntity;

    public ActionSelectionScreen(Entity targetEntity) {
        super(Text.literal("Выбор действия"));
        this.targetEntity = targetEntity;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int startY = this.height / 2 - 40;

        // Кнопка "Рот"
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Рот"),
                button -> onMouthButtonClick()
        ).dimensions(centerX - buttonWidth / 2, startY, buttonWidth, buttonHeight).build());

        // Кнопка "Зад"
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Зад"),
                button -> onBackButtonClick()
        ).dimensions(centerX - buttonWidth / 2, startY + 30, buttonWidth, buttonHeight).build());

        // Кнопка "Отмена"
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Отмена"),
                button -> onCancelButtonClick()
        ).dimensions(centerX - buttonWidth / 2, startY + 60, buttonWidth, buttonHeight).build());
    }

    private void onMouthButtonClick() {
        // Отправляем пакет на сервер для выполнения действия "Рот"
        if (this.client != null && this.client.player != null) {
            HoneyActionPayload payload = new HoneyActionPayload(targetEntity.getId(), "MOUTH");
            ClientPlayNetworking.send(payload);
        }
        this.close();
    }

    private void onBackButtonClick() {
        // Отправляем пакет на сервер для выполнения действия "Зад"
        if (this.client != null && this.client.player != null) {
            HoneyActionPayload payload = new HoneyActionPayload(targetEntity.getId(), "BACK");
            ClientPlayNetworking.send(payload);
        }
        this.close();
    }

    private void onCancelButtonClick() {
        this.close();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Рисуем заголовок
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                20,
                0xFFFFFF
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
