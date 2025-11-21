package com.tekilo.screen;

import com.tekilo.network.ItemSpawnerSettingsPayload;
import com.tekilo.network.ZoneSettingsPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
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

    // Zone settings values
    private int zoneRadius = 250;
    private int baseCaptureTime = 600; // in seconds (10 min)
    private int minCaptureTime = 300; // in seconds (5 min)
    private boolean zoneEnabled = true;
    private String zoneName = "";
    private int bossBarColor = 0;
    private String captureReward = "";

    // UI state
    private boolean showZonePanel = false; // Toggle between spawner and zone settings

    // Spawner UI Elements
    private ButtonWidget radiusMinusBtn, radiusPlusBtn;
    private ButtonWidget intervalMinusBtn, intervalPlusBtn;
    private ButtonWidget countMinusBtn, countPlusBtn;
    private ButtonWidget modeToggleBtn;
    private ButtonWidget settingsToggleBtn;
    private ButtonWidget enabledToggleBtn;

    // Zone UI Elements
    private ButtonWidget zoneRadiusMinusBtn, zoneRadiusPlusBtn;
    private ButtonWidget baseTimeMinusBtn, baseTimePlusBtn;
    private ButtonWidget minTimeMinusBtn, minTimePlusBtn;
    private ButtonWidget zoneEnabledBtn;
    private ButtonWidget bossBarColorBtn;
    private TextFieldWidget zoneNameField;
    private TextFieldWidget rewardField;

    // Tab buttons
    private ButtonWidget spawnerTabBtn;
    private ButtonWidget zoneTabBtn;

    // Tooltip tracking
    private List<Text> currentTooltip = new ArrayList<>();
    private int tooltipX, tooltipY;

    // Block position for syncing
    private BlockPos blockPos;

    // Debounce timer to prevent packet spam
    private long lastSyncTime = 0;
    private long lastZoneSyncTime = 0;
    private static final long SYNC_COOLDOWN_MS = 100; // 100ms between syncs

    private static final String[] COLOR_NAMES = {"WHITE", "RED", "YELLOW", "GREEN", "BLUE", "PURPLE", "PINK"};

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

        // === TAB BUTTONS (always visible) ===
        this.spawnerTabBtn = ButtonWidget.builder(
            Text.literal("Spawner"),
            btn -> switchToSpawnerTab()
        ).dimensions(panelX, panelY - 18, 45, 16).build();
        this.addDrawableChild(spawnerTabBtn);

        this.zoneTabBtn = ButtonWidget.builder(
            Text.literal("Zone"),
            btn -> switchToZoneTab()
        ).dimensions(panelX + 45, panelY - 18, 45, 16).build();
        this.addDrawableChild(zoneTabBtn);

        // === SPAWNER PANEL CONTROLS ===
        // === RADIUS CONTROLS ===
        this.radiusMinusBtn = ButtonWidget.builder(Text.literal("-"), btn -> adjustRadius(-10))
            .dimensions(panelX + 5, panelY + 14, btnSize, btnSize).build();
        this.radiusPlusBtn = ButtonWidget.builder(Text.literal("+"), btn -> adjustRadius(10))
            .dimensions(panelX + 65, panelY + 14, btnSize, btnSize).build();
        this.addDrawableChild(radiusMinusBtn);
        this.addDrawableChild(radiusPlusBtn);

        // === INTERVAL CONTROLS ===
        this.intervalMinusBtn = ButtonWidget.builder(Text.literal("-"), btn -> adjustInterval(-30))
            .dimensions(panelX + 5, panelY + 52, btnSize, btnSize).build();
        this.intervalPlusBtn = ButtonWidget.builder(Text.literal("+"), btn -> adjustInterval(30))
            .dimensions(panelX + 65, panelY + 52, btnSize, btnSize).build();
        this.addDrawableChild(intervalMinusBtn);
        this.addDrawableChild(intervalPlusBtn);

        // === COUNT CONTROLS ===
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

        // === ZONE PANEL CONTROLS ===
        // Zone Radius
        this.zoneRadiusMinusBtn = ButtonWidget.builder(Text.literal("-"), btn -> adjustZoneRadius(-50))
            .dimensions(panelX + 5, panelY + 14, btnSize, btnSize).build();
        this.zoneRadiusPlusBtn = ButtonWidget.builder(Text.literal("+"), btn -> adjustZoneRadius(50))
            .dimensions(panelX + 65, panelY + 14, btnSize, btnSize).build();
        this.addDrawableChild(zoneRadiusMinusBtn);
        this.addDrawableChild(zoneRadiusPlusBtn);

        // Base Capture Time
        this.baseTimeMinusBtn = ButtonWidget.builder(Text.literal("-"), btn -> adjustBaseCaptureTime(-60))
            .dimensions(panelX + 5, panelY + 52, btnSize, btnSize).build();
        this.baseTimePlusBtn = ButtonWidget.builder(Text.literal("+"), btn -> adjustBaseCaptureTime(60))
            .dimensions(panelX + 65, panelY + 52, btnSize, btnSize).build();
        this.addDrawableChild(baseTimeMinusBtn);
        this.addDrawableChild(baseTimePlusBtn);

        // Min Capture Time
        this.minTimeMinusBtn = ButtonWidget.builder(Text.literal("-"), btn -> adjustMinCaptureTime(-60))
            .dimensions(panelX + 5, panelY + 90, btnSize, btnSize).build();
        this.minTimePlusBtn = ButtonWidget.builder(Text.literal("+"), btn -> adjustMinCaptureTime(60))
            .dimensions(panelX + 65, panelY + 90, btnSize, btnSize).build();
        this.addDrawableChild(minTimeMinusBtn);
        this.addDrawableChild(minTimePlusBtn);

        // Zone Enabled Toggle
        this.zoneEnabledBtn = ButtonWidget.builder(
            Text.literal(zoneEnabled ? "ON" : "OFF"),
            btn -> toggleZoneEnabled()
        ).dimensions(panelX + 5, panelY + 118, 38, 20).build();
        this.addDrawableChild(zoneEnabledBtn);

        // Boss Bar Color
        this.bossBarColorBtn = ButtonWidget.builder(
            Text.literal(COLOR_NAMES[bossBarColor]),
            btn -> cycleBossBarColor()
        ).dimensions(panelX + 47, panelY + 118, 38, 20).build();
        this.addDrawableChild(bossBarColorBtn);

        // Zone Name Field
        this.zoneNameField = new TextFieldWidget(this.textRenderer, panelX + 5, panelY + 143, 80, 12, Text.literal("Name"));
        this.zoneNameField.setMaxLength(20);
        this.zoneNameField.setText(zoneName);
        this.zoneNameField.setChangedListener(this::onZoneNameChanged);
        this.addDrawableChild(zoneNameField);

        // Reward Item Field
        this.rewardField = new TextFieldWidget(this.textRenderer, panelX + 5, panelY + 158, 80, 12, Text.literal("Reward"));
        this.rewardField.setMaxLength(50);
        this.rewardField.setText(captureReward);
        this.rewardField.setChangedListener(this::onRewardChanged);
        this.addDrawableChild(rewardField);

        // Set initial visibility and tab appearance
        updatePanelVisibility();
        updateTabAppearance();
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

    private void switchToSpawnerTab() {
        showZonePanel = false;
        updatePanelVisibility();
        updateTabAppearance();
    }

    private void switchToZoneTab() {
        showZonePanel = true;
        updatePanelVisibility();
        updateTabAppearance();
    }

    private void updateTabAppearance() {
        // Active tab is highlighted, inactive is dimmed
        spawnerTabBtn.active = showZonePanel;
        zoneTabBtn.active = !showZonePanel;
    }

    private void updatePanelVisibility() {
        // Spawner controls visibility
        radiusMinusBtn.visible = !showZonePanel;
        radiusPlusBtn.visible = !showZonePanel;
        intervalMinusBtn.visible = !showZonePanel;
        intervalPlusBtn.visible = !showZonePanel;
        countMinusBtn.visible = !showZonePanel;
        countPlusBtn.visible = !showZonePanel;
        modeToggleBtn.visible = !showZonePanel;
        settingsToggleBtn.visible = !showZonePanel;
        enabledToggleBtn.visible = !showZonePanel;

        // Zone controls visibility
        zoneRadiusMinusBtn.visible = showZonePanel;
        zoneRadiusPlusBtn.visible = showZonePanel;
        baseTimeMinusBtn.visible = showZonePanel;
        baseTimePlusBtn.visible = showZonePanel;
        minTimeMinusBtn.visible = showZonePanel;
        minTimePlusBtn.visible = showZonePanel;
        zoneEnabledBtn.visible = showZonePanel;
        bossBarColorBtn.visible = showZonePanel;
        zoneNameField.visible = showZonePanel;
        rewardField.visible = showZonePanel;
    }

    // Zone settings methods
    private void adjustZoneRadius(int delta) {
        zoneRadius = Math.max(10, Math.min(1000, zoneRadius + delta));
        syncZoneSettings();
    }

    private void adjustBaseCaptureTime(int delta) {
        baseCaptureTime = Math.max(60, Math.min(3600, baseCaptureTime + delta));
        // Ensure base time >= min time
        if (baseCaptureTime < minCaptureTime) {
            minCaptureTime = baseCaptureTime;
        }
        syncZoneSettings();
    }

    private void adjustMinCaptureTime(int delta) {
        minCaptureTime = Math.max(30, Math.min(baseCaptureTime, minCaptureTime + delta));
        syncZoneSettings();
    }

    private void toggleZoneEnabled() {
        zoneEnabled = !zoneEnabled;
        zoneEnabledBtn.setMessage(Text.literal(zoneEnabled ? "ON" : "OFF"));
        syncZoneSettings();
    }

    private void cycleBossBarColor() {
        bossBarColor = (bossBarColor + 1) % COLOR_NAMES.length;
        bossBarColorBtn.setMessage(Text.literal(COLOR_NAMES[bossBarColor]));
        syncZoneSettings();
    }

    private void onZoneNameChanged(String newName) {
        zoneName = newName;
        // Don't sync immediately - will sync on close or when other settings change
    }

    private void onRewardChanged(String newReward) {
        captureReward = newReward;
        // Don't sync immediately - will sync on close or when other settings change
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

    private void syncZoneSettings() {
        if (blockPos != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastZoneSyncTime >= SYNC_COOLDOWN_MS) {
                lastZoneSyncTime = currentTime;
                // Read current values from text fields
                String currentZoneName = zoneNameField != null ? zoneNameField.getText() : zoneName;
                String currentReward = rewardField != null ? rewardField.getText() : captureReward;

                ClientPlayNetworking.send(new ZoneSettingsPayload(
                    blockPos,
                    zoneRadius,
                    baseCaptureTime * 20, // Convert seconds to ticks
                    minCaptureTime * 20,  // Convert seconds to ticks
                    zoneEnabled,
                    currentZoneName,
                    bossBarColor,
                    currentReward
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
        int panelH = 175;

        // Draw tab backgrounds
        // Active tab connects to panel, inactive has gap
        if (!showZonePanel) {
            // Spawner tab is active
            context.fill(panelX, panelY - 18, panelX + 45, panelY, 0xFF373737);
            context.fill(panelX + 45, panelY - 18, panelX + 90, panelY - 1, 0xFF2A2A2A);
        } else {
            // Zone tab is active
            context.fill(panelX, panelY - 18, panelX + 45, panelY - 1, 0xFF2A2A2A);
            context.fill(panelX + 45, panelY - 18, panelX + 90, panelY, 0xFF373737);
        }

        // Panel background with gradient effect
        context.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY + panelH + 2, 0xFF000000);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF373737);

        // Inner panel gradient (darker at bottom)
        context.fill(panelX + 1, panelY + 1, panelX + panelW - 1, panelY + panelH / 3, 0xFF3D3D3D);
        context.fill(panelX + 1, panelY + panelH / 3, panelX + panelW - 1, panelY + 2 * panelH / 3, 0xFF353535);
        context.fill(panelX + 1, panelY + 2 * panelH / 3, panelX + panelW - 1, panelY + panelH - 1, 0xFF2D2D2D);

        // Panel border (3D effect)
        context.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFF5A5A5A); // top highlight
        context.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFF5A5A5A); // left highlight
        context.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF1A1A1A); // bottom shadow
        context.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF1A1A1A); // right shadow

        // Inner highlight line
        context.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + 3, 0xFF4A4A4A);

        // === DRAW SETTING LABELS ===
        if (!showZonePanel) {
            // Spawner panel labels
            context.drawText(this.textRenderer,
                Text.translatable("gui.tekilo.item_spawner.radius_short"),
                panelX + 5, panelY + 2, 0xFFFFFF, false);

            context.drawText(this.textRenderer,
                Text.translatable("gui.tekilo.item_spawner.interval_short"),
                panelX + 5, panelY + 40, 0xFFFFFF, false);

            context.drawText(this.textRenderer,
                Text.translatable("gui.tekilo.item_spawner.count_short"),
                panelX + 5, panelY + 78, 0xFFFFFF, false);
        } else {
            // Zone panel labels
            context.drawText(this.textRenderer,
                Text.literal("Zone Radius"),
                panelX + 5, panelY + 2, 0xFFFFFF, false);

            context.drawText(this.textRenderer,
                Text.literal("Base Time"),
                panelX + 5, panelY + 40, 0xFFFFFF, false);

            context.drawText(this.textRenderer,
                Text.literal("Min Time"),
                panelX + 5, panelY + 78, 0xFFFFFF, false);
        }
    }

    private void drawValueBetweenButtons(DrawContext context, int panelX, int y, String value) {
        // Value centered between - and + buttons (buttons at X+5 to X+25 and X+65 to X+85)
        // Center is at X+45
        int valueWidth = this.textRenderer.getWidth(value);
        int centerX = panelX + 45;
        int textX = centerX - valueWidth / 2;

        // Draw value background (LCD-like display)
        int bgX = panelX + 27;
        int bgY = y - 2;
        int bgW = 36;
        int bgH = 12;
        context.fill(bgX, bgY, bgX + bgW, bgY + bgH, 0xFF1A1A1A);
        context.fill(bgX + 1, bgY + 1, bgX + bgW - 1, bgY + bgH - 1, 0xFF0A0A0A);

        // Use ARGB format (0xAARRGGBB) - alpha channel is required in 1.21+
        context.drawText(this.textRenderer, Text.literal(value), textX, y, 0xFF55FF55, true);
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
            // Load spawner settings
            currentRadius = handler.getRadius();
            currentInterval = handler.getSpawnInterval() / 20;
            currentCount = handler.getItemCount();
            spawnInChests = handler.isSpawnInChests();
            useGlobalSettings = handler.isUseGlobalSettings();
            enabled = handler.isEnabled();

            // Load zone settings
            zoneRadius = handler.getZoneRadius();
            baseCaptureTime = handler.getBaseCaptureTime() / 20; // ticks to seconds
            minCaptureTime = handler.getMinCaptureTime() / 20;   // ticks to seconds
            zoneEnabled = handler.isZoneEnabled();
            bossBarColor = handler.getBossBarColor();
            // Load string data from handler (sent via ExtendedScreenHandlerFactory)
            zoneName = handler.getZoneName();
            captureReward = handler.getCaptureReward();

            // Update text fields with loaded values
            if (zoneNameField != null) {
                zoneNameField.setText(zoneName);
            }
            if (rewardField != null) {
                rewardField.setText(captureReward);
            }

            // Update spawner button text
            if (modeToggleBtn != null) {
                modeToggleBtn.setMessage(Text.literal(spawnInChests ? "Chests" : "Ground"));
            }
            if (settingsToggleBtn != null) {
                settingsToggleBtn.setMessage(Text.literal(useGlobalSettings ? "Glb" : "Item"));
            }
            if (enabledToggleBtn != null) {
                enabledToggleBtn.setMessage(Text.literal(enabled ? "ON" : "OFF"));
            }

            // Update zone button text
            if (zoneEnabledBtn != null) {
                zoneEnabledBtn.setMessage(Text.literal(zoneEnabled ? "ON" : "OFF"));
            }
            if (bossBarColorBtn != null) {
                bossBarColorBtn.setMessage(Text.literal(COLOR_NAMES[bossBarColor]));
            }

            // Update tab appearance
            updateTabAppearance();

            initializationDelay = -1; // Mark as initialized
        }

        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);

        // Draw values between buttons (after widgets are rendered)
        int panelX = this.x + this.backgroundWidth + 5;
        int panelY = this.y + 5;

        if (!showZonePanel) {
            // Spawner panel values
            drawValueBetweenButtons(context, panelX, panelY + 19, String.valueOf(currentRadius) + "m");
            drawValueBetweenButtons(context, panelX, panelY + 57, formatTime(currentInterval));
            drawValueBetweenButtons(context, panelX, panelY + 95, String.valueOf(currentCount));
        } else {
            // Zone panel values
            drawValueBetweenButtons(context, panelX, panelY + 19, String.valueOf(zoneRadius) + "m");
            drawValueBetweenButtons(context, panelX, panelY + 57, formatTime(baseCaptureTime));
            drawValueBetweenButtons(context, panelX, panelY + 95, formatTime(minCaptureTime));
        }

        currentTooltip.clear();

        if (!showZonePanel) {
            // Spawner panel tooltips
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
        } else {
            // Zone panel tooltips
            if (isMouseOver(mouseX, mouseY, panelX + 5, panelY + 118, 38, 20)) {
                currentTooltip.add(Text.literal("Zone Enabled"));
                currentTooltip.add(Text.literal(zoneEnabled ? "Zone capture is active" : "Zone capture is disabled").styled(s -> s.withColor(0xAAAAAA)));
            }

            if (isMouseOver(mouseX, mouseY, panelX + 47, panelY + 118, 38, 20)) {
                currentTooltip.add(Text.literal("Boss Bar Color"));
                currentTooltip.add(Text.literal("Color of uncaptured zone bar").styled(s -> s.withColor(0xAAAAAA)));
            }

            if (isMouseOver(mouseX, mouseY, panelX, panelY + 2, 90, 35)) {
                currentTooltip.add(Text.literal("Zone Radius"));
                currentTooltip.add(Text.literal("Capture zone size in blocks").styled(s -> s.withColor(0xAAAAAA)));
            }

            if (isMouseOver(mouseX, mouseY, panelX, panelY + 40, 90, 35)) {
                currentTooltip.add(Text.literal("Base Capture Time"));
                currentTooltip.add(Text.literal("Time to capture with 1 player").styled(s -> s.withColor(0xAAAAAA)));
            }

            if (isMouseOver(mouseX, mouseY, panelX, panelY + 78, 90, 35)) {
                currentTooltip.add(Text.literal("Min Capture Time"));
                currentTooltip.add(Text.literal("Minimum time with 5+ players").styled(s -> s.withColor(0xAAAAAA)));
            }

            if (isMouseOver(mouseX, mouseY, panelX + 5, panelY + 143, 80, 12)) {
                currentTooltip.add(Text.literal("Zone Name"));
                currentTooltip.add(Text.literal("Display name in boss bar").styled(s -> s.withColor(0xAAAAAA)));
            }

            if (isMouseOver(mouseX, mouseY, panelX + 5, panelY + 158, 80, 12)) {
                currentTooltip.add(Text.literal("Capture Reward"));
                currentTooltip.add(Text.literal("Item ID (e.g. minecraft:diamond)").styled(s -> s.withColor(0xAAAAAA)));
            }
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

    @Override
    public void close() {
        // Sync zone settings one final time before closing (to save text field values)
        if (blockPos != null && initializationDelay == -1) {
            // Read current values from text fields
            String currentZoneName = zoneNameField != null ? zoneNameField.getText() : zoneName;
            String currentReward = rewardField != null ? rewardField.getText() : captureReward;

            // Force sync without cooldown
            ClientPlayNetworking.send(new ZoneSettingsPayload(
                blockPos,
                zoneRadius,
                baseCaptureTime * 20,
                minCaptureTime * 20,
                zoneEnabled,
                currentZoneName,
                bossBarColor,
                currentReward
            ));
        }
        super.close();
    }
}
