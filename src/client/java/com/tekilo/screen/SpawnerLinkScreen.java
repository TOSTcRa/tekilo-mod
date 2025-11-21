package com.tekilo.screen;

import com.tekilo.network.SpawnerLinkPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SpawnerLinkScreen extends HandledScreen<SpawnerLinkScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("tekilo", "textures/gui/spawner_link.png");

    public SpawnerLinkScreen(SpawnerLinkScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 114;
        this.backgroundWidth = 176;
    }

    @Override
    protected void init() {
        super.init();
        int centerX = (width - backgroundWidth) / 2;
        int centerY = (height - backgroundHeight) / 2;

        // Title
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.titleY = 10;

        // Buttons for time selection (0, 15, 30, 45, 60 minutes)
        int buttonWidth = 50;
        int buttonHeight = 20;
        int startY = centerY + 35;
        int spacing = 4;
        int totalWidth = (buttonWidth * 5) + (spacing * 4);
        int startX = centerX + (backgroundWidth - totalWidth) / 2;

        // 0 minutes (instant unlock)
        addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.tekilo.spawner_link.0min"),
            button -> sendLinkRequest(0))
            .dimensions(startX, startY, buttonWidth, buttonHeight)
            .build()
        );

        // 15 minutes
        addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.tekilo.spawner_link.15min"),
            button -> sendLinkRequest(15 * 60))
            .dimensions(startX + (buttonWidth + spacing), startY, buttonWidth, buttonHeight)
            .build()
        );

        // 30 minutes
        addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.tekilo.spawner_link.30min"),
            button -> sendLinkRequest(30 * 60))
            .dimensions(startX + (buttonWidth + spacing) * 2, startY, buttonWidth, buttonHeight)
            .build()
        );

        // 45 minutes
        addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.tekilo.spawner_link.45min"),
            button -> sendLinkRequest(45 * 60))
            .dimensions(startX + (buttonWidth + spacing) * 3, startY, buttonWidth, buttonHeight)
            .build()
        );

        // 60 minutes
        addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.tekilo.spawner_link.60min"),
            button -> sendLinkRequest(60 * 60))
            .dimensions(startX + (buttonWidth + spacing) * 4, startY, buttonWidth, buttonHeight)
            .build()
        );

        // Cancel button
        addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.cancel"),
            button -> close())
            .dimensions(centerX + (backgroundWidth - 60) / 2, startY + buttonHeight + spacing + 5, 60, buttonHeight)
            .build()
        );
    }

    private void sendLinkRequest(int unlockDelaySeconds) {
        ClientPlayNetworking.send(new SpawnerLinkPayload(
            handler.getParentPos(),
            handler.getChildPos(),
            unlockDelaySeconds
        ));
        close();
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        int x = (width - backgroundWidth) / 2;
        int y = (height - backgroundHeight) / 2;
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0, 0, backgroundWidth, backgroundHeight, 256, 256);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // Draw instruction text
        int centerX = (width - backgroundWidth) / 2;
        int centerY = (height - backgroundHeight) / 2;
        Text instruction = Text.translatable("gui.tekilo.spawner_link.instruction");
        int textWidth = this.textRenderer.getWidth(instruction);
        context.drawText(this.textRenderer, instruction,
            centerX + (backgroundWidth - textWidth) / 2,
            centerY + 25,
            0x404040, false);
    }
}
