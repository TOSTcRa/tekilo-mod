package com.tekilo.screen;

import com.tekilo.network.ItemSpawnerSettingsPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class ItemSpawnerScreen extends HandledScreen<ItemSpawnerScreenHandler> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/generic_54.png");
    private static final Identifier WIDGETS = Identifier.ofVanilla("textures/gui/sprites/widget/button.png");

    // Settings values (local copy, synced via network packet)
    private int currentRadius;
    private int currentInterval; // in seconds
    private int currentCount;
    private boolean spawnInChests;
    private boolean useGlobalSettings;
    private boolean enabled;
    private int initializationDelay = 5; // Wait 5 frames for PropertyDelegate sync

    // UI Elements
    private ButtonWidget radiusMinusBtn, radiusPlusBtn;
    private ButtonWidget intervalMinusBtn, intervalPlusBtn;
    private ButtonWidget countMinusBtn, countPlusBtn;
    private ButtonWidget modeToggleBtn;
    private ButtonWidget settingsToggleBtn;
    private ButtonWidget enabledToggleBtn;

    // Tooltip tracking
    private List<Text> currentTooltip = new ArrayList<>();
    private int tooltipX, tooltipY;

    // Block position for syncing
    private BlockPos blockPos;

    // Debounce timer to prevent packet spam
    private long lastSyncTime = 0;
    private static final long SYNC_COOLDOWN_MS = 100; // 100ms between syncs

    public ItemSpawnerScreen(ItemSpawnerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 168;
        this.backgroundWidth = 176;
        this.playerInventoryTitleY = 72;
        this.blockPos = handler.getBlockPos();

        // Initialize with defaults, will be updated from PropertyDelegate
        this.currentRadius = 50;
        this.currentInterval = 300; // 5 minutes in seconds
        this.currentCount = 1;
        this.spawnInChests = false;
        this.useGlobalSettings = true;
        this.enabled = true;
    }

    @Override
    protected void init() {
        super.init();

        int panelX = this.x + this.backgroundWidth + 5;
        int panelY = this.y + 5;
        int btnSize = 20;

        // === RADIUS CONTROLS ===
        // Label at Y+2, buttons at Y+14
        this.radiusMinusBtn = ButtonWidget.builder(Text.literal("-"), btn -> adjustRadius(-10))
            .dimensions(panelX + 5, panelY + 14, btnSize, btnSize).build();
        this.radiusPlusBtn = ButtonWidget.builder(Text.literal("+"), btn -> adjustRadius(10))
            .dimensions(panelX + 65, panelY + 14, btnSize, btnSize).build();
        this.addDrawableChild(radiusMinusBtn);
        this.addDrawableChild(radiusPlusBtn);

        // === INTERVAL CONTROLS ===
        // Label at Y+40, buttons at Y+52
        this.intervalMinusBtn = ButtonWidget.builder(Text.literal("-"), btn -> adjustInterval(-30))
            .dimensions(panelX + 5, panelY + 52, btnSize, btnSize).build();
        this.intervalPlusBtn = ButtonWidget.builder(Text.literal("+"), btn -> adjustInterval(30))
            .dimensions(panelX + 65, panelY + 52, btnSize, btnSize).build();
        this.addDrawableChild(intervalMinusBtn);
        this.addDrawableChild(intervalPlusBtn);

        // === COUNT CONTROLS ===
        // Label at Y+78, buttons at Y+90
        this.countMinusBtn = ButtonWidget.builder(Text.literal("-"), btn -> adjustCount(-1))
            .dimensions(panelX + 5, panelY + 90, btnSize, btnSize).build();
        this.countPlusBtn = ButtonWidget.builder(Text.literal("+"), btn -> adjustCount(1))
            .dimensions(panelX + 65, panelY + 90, btnSize, btnSize).build();
        this.addDrawableChild(countMinusBtn);
        this.addDrawableChild(countPlusBtn);

        // === MODE TOGGLE (Chest/Ground) ===
        this.modeToggleBtn = ButtonWidget.builder(
            Text.literal(spawnInChests ? "Chests" : "Ground"),
            btn -> toggleMode()
        ).dimensions(panelX + 5, panelY + 118, 38, 20).build();
        this.addDrawableChild(modeToggleBtn);

        // === SETTINGS TOGGLE (Global/Per-item) ===
        this.settingsToggleBtn = ButtonWidget.builder(
            Text.literal(useGlobalSettings ? "Glb" : "Item"),
            btn -> toggleSettings()
        ).dimensions(panelX + 47, panelY + 118, 38, 20).build();
        this.addDrawableChild(settingsToggleBtn);

        // === ENABLED TOGGLE (ON/OFF) ===
        this.enabledToggleBtn = ButtonWidget.builder(
            Text.literal(enabled ? "ON" : "OFF"),
            btn -> toggleEnabled()
        ).dimensions(panelX + 5, panelY + 143, 80, 20).build();
        this.addDrawableChild(enabledToggleBtn);
    }

    private void adjustRadius(int delta) {
        currentRadius = Math.max(10, Math.min(500, currentRadius + delta));
        syncSettings();
    }

    private void adjustInterval(int delta) {
        currentInterval = Math.max(10, Math.min(3600, currentInterval + delta));
        syncSettings();
    }

    private void adjustCount(int delta) {
        currentCount = Math.max(1, Math.min(64, currentCount + delta));
        syncSettings();
    }

    private void toggleMode() {
        spawnInChests = !spawnInChests;
        modeToggleBtn.setMessage(Text.literal(spawnInChests ? "Chests" : "Ground"));
        syncSettings();
    }

    private void toggleSettings() {
        useGlobalSettings = !useGlobalSettings;
        settingsToggleBtn.setMessage(Text.literal(useGlobalSettings ? "Glb" : "Item"));
        syncSettings();
    }

    private void toggleEnabled() {
        enabled = !enabled;
        enabledToggleBtn.setMessage(Text.literal(enabled ? "ON" : "OFF"));
        syncSettings();
    }

    private void syncSettings() {
        if (blockPos != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSyncTime >= SYNC_COOLDOWN_MS) {
                lastSyncTime = currentTime;
                ClientPlayNetworking.send(new ItemSpawnerSettingsPayload(
                    blockPos,
                    currentRadius,
                    currentInterval * 20, // Convert seconds to ticks
                    currentCount,
                    spawnInChests,
                    useGlobalSettings,
                    enabled
                ));
            }
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Draw main inventory background
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.x, this.y, 0, 0, backgroundWidth, 71, 256, 256);
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, this.x, this.y + 71, 0, 126, backgroundWidth, 96, 256, 256);

        // Draw settings panel background
        int panelX = this.x + this.backgroundWidth + 5;
        int panelY = this.y + 5;
        int panelW = 90;
        int panelH = 165;

        // Panel background (dark gray)
        context.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0xFF000000);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF373737);

        // Panel border (lighter)
        context.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF5A5A5A);
        context.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF5A5A5A);
        context.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF5A5A5A);
        context.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF5A5A5A);

        // === DRAW SETTING LABELS ===

        // Radius section - label at Y+2
        context.drawText(this.textRenderer,
            Text.translatable("gui.tekilo.item_spawner.radius_short"),
            panelX + 5, panelY + 2, 0xFFFFFF, false);

        // Interval section - label at Y+40
        context.drawText(this.textRenderer,
            Text.translatable("gui.tekilo.item_spawner.interval_short"),
            panelX + 5, panelY + 40, 0xFFFFFF, false);

        // Count section - label at Y+78
        context.drawText(this.textRenderer,
            Text.translatable("gui.tekilo.item_spawner.count_short"),
            panelX + 5, panelY + 78, 0xFFFFFF, false);
    }

    private void drawValueBetweenButtons(DrawContext context, int panelX, int y, String value) {
        // Value centered between - and + buttons (buttons at X+5 to X+25 and X+65 to X+85)
        // Center is at X+45
        int valueWidth = this.textRenderer.getWidth(value);
        // Use ARGB format (0xAARRGGBB) - alpha channel is required in 1.21+
        context.drawText(this.textRenderer, Text.literal(value), panelX + 45 - valueWidth / 2, y, 0xFF55FF55, true);
    }

    private String formatTime(int seconds) {
        if (seconds >= 60) {
            int min = seconds / 60;
            int sec = seconds % 60;
            return sec > 0 ? min + "m" + sec + "s" : min + "m";
        }
        return seconds + "s";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Wait a few frames for PropertyDelegate to sync, then read values
        if (initializationDelay > 0) {
            initializationDelay--;
        } else if (initializationDelay == 0) {
            currentRadius = handler.getRadius();
            currentInterval = handler.getSpawnInterval() / 20;
            currentCount = handler.getItemCount();
            spawnInChests = handler.isSpawnInChests();
            useGlobalSettings = handler.isUseGlobalSettings();
            enabled = handler.isEnabled();

            // Update button text
            if (modeToggleBtn != null) {
                modeToggleBtn.setMessage(Text.literal(spawnInChests ? "Chests" : "Ground"));
            }
            if (settingsToggleBtn != null) {
                settingsToggleBtn.setMessage(Text.literal(useGlobalSettings ? "Glb" : "Item"));
            }
            if (enabledToggleBtn != null) {
                enabledToggleBtn.setMessage(Text.literal(enabled ? "ON" : "OFF"));
            }
            initializationDelay = -1; // Mark as initialized
        }

        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);

        // Draw values between buttons (after widgets are rendered)
        int panelX = this.x + this.backgroundWidth + 5;
        int panelY = this.y + 5;

        drawValueBetweenButtons(context, panelX, panelY + 19, String.valueOf(currentRadius) + "m");
        drawValueBetweenButtons(context, panelX, panelY + 57, formatTime(currentInterval));
        drawValueBetweenButtons(context, panelX, panelY + 95, String.valueOf(currentCount));

        currentTooltip.clear();

        // Check hover over mode button
        if (isMouseOver(mouseX, mouseY, panelX + 5, panelY + 118, 38, 20)) {
            currentTooltip.add(Text.translatable("gui.tekilo.item_spawner.mode.tooltip"));
            currentTooltip.add(Text.translatable(spawnInChests ?
                "gui.tekilo.item_spawner.mode.chests.desc" :
                "gui.tekilo.item_spawner.mode.ground.desc").copy().styled(s -> s.withColor(0xAAAAAA)));
        }

        // Check hover over settings button
        if (isMouseOver(mouseX, mouseY, panelX + 47, panelY + 118, 38, 20)) {
            currentTooltip.add(Text.translatable("gui.tekilo.item_spawner.settings.tooltip"));
            currentTooltip.add(Text.translatable(useGlobalSettings ?
                "gui.tekilo.item_spawner.settings.global.desc" :
                "gui.tekilo.item_spawner.settings.peritem.desc").copy().styled(s -> s.withColor(0xAAAAAA)));
        }

        // Check hover over enabled button
        if (isMouseOver(mouseX, mouseY, panelX + 5, panelY + 143, 80, 20)) {
            currentTooltip.add(Text.translatable("gui.tekilo.item_spawner.enabled.tooltip"));
            currentTooltip.add(Text.translatable(enabled ?
                "gui.tekilo.item_spawner.enabled.on.desc" :
                "gui.tekilo.item_spawner.enabled.off.desc").copy().styled(s -> s.withColor(0xAAAAAA)));
        }

        // Check hover over radius area
        if (isMouseOver(mouseX, mouseY, panelX, panelY + 2, 90, 35)) {
            currentTooltip.add(Text.translatable("gui.tekilo.item_spawner.radius.tooltip"));
            currentTooltip.add(Text.translatable("gui.tekilo.item_spawner.radius.desc").copy().styled(s -> s.withColor(0xAAAAAA)));
        }

        // Check hover over interval area
        if (isMouseOver(mouseX, mouseY, panelX, panelY + 40, 90, 35)) {
            currentTooltip.add(Text.translatable("gui.tekilo.item_spawner.interval.tooltip"));
            currentTooltip.add(Text.translatable("gui.tekilo.item_spawner.interval.desc").copy().styled(s -> s.withColor(0xAAAAAA)));
        }

        // Check hover over count area
        if (isMouseOver(mouseX, mouseY, panelX, panelY + 78, 90, 35)) {
            currentTooltip.add(Text.translatable("gui.tekilo.item_spawner.count.tooltip"));
            currentTooltip.add(Text.translatable("gui.tekilo.item_spawner.count.desc").copy().styled(s -> s.withColor(0xAAAAAA)));
        }

        if (!currentTooltip.isEmpty()) {
            context.drawTooltip(this.textRenderer, currentTooltip, mouseX, mouseY);
        }
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);

        // Draw hint text above inventory
        Text hint = Text.translatable("gui.tekilo.item_spawner.hint");
        int hintWidth = this.textRenderer.getWidth(hint);
        context.drawText(this.textRenderer, hint, (this.backgroundWidth - hintWidth) / 2, 60, 0x666666, false);
    }
}
