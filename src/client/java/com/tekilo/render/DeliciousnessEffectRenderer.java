package com.tekilo.render;

import com.tekilo.FactionManager;
import com.tekilo.ModStatusEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.Random;

public class DeliciousnessEffectRenderer {
    private static final Identifier[] FACE_TEXTURES = {
        Identifier.of("tekilo", "textures/gui/friends_face_1.png"),
        Identifier.of("tekilo", "textures/gui/friends_face_2.png"),
        Identifier.of("tekilo", "textures/gui/friends_face_3.png"),
        Identifier.of("tekilo", "textures/gui/friends_face_4.png"),
        Identifier.of("tekilo", "textures/gui/friends_face_5.png"),
        Identifier.of("tekilo", "textures/gui/friends_face_6.png"),
    };

    private static final Identifier[] COMMUNIST_POSTERS = {
        Identifier.of("tekilo", "textures/painting/poster_cm_1.png"),
        Identifier.of("tekilo", "textures/painting/poster_cm_2.png"),
        Identifier.of("tekilo", "textures/painting/poster_cm_3.png"),
        Identifier.of("tekilo", "textures/painting/poster_cm_4.png"),
    };

    private static final Identifier[] CAPITALIST_POSTERS = {
        Identifier.of("tekilo", "textures/painting/poster_cp_1.png"),
        Identifier.of("tekilo", "textures/painting/poster_cp_2.png"),
        Identifier.of("tekilo", "textures/painting/poster_cp_3.png"),
        Identifier.of("tekilo", "textures/painting/poster_cp_4.png"),
        Identifier.of("tekilo", "textures/painting/poster_cp_5.png"),
    };

    private static final Identifier[] SOUND_IDS = {
        Identifier.of("tekilo", "face_sound_1"),
        Identifier.of("tekilo", "face_sound_2"),
        Identifier.of("tekilo", "face_sound_3"),
    };

    private static final Random RANDOM = new Random();
    private static long effectStartTime = -1;
    private static Identifier currentFaceTexture = null;
    private static Identifier currentPosterTexture = null;
    private static long lastSoundTime = 0;
    private static final long SOUND_INTERVAL = 5000; // Play sound every 5 seconds

    public static void renderEffect(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            effectStartTime = -1;
            return;
        }

        // Check if player has the deliciousness effect
        if (!client.player.hasStatusEffect(ModStatusEffects.DELICIOUSNESS)) {
            effectStartTime = -1;
            currentFaceTexture = null;
            currentPosterTexture = null;
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Initialize effect if just started
        if (effectStartTime == -1) {
            effectStartTime = currentTime;
            currentFaceTexture = FACE_TEXTURES[RANDOM.nextInt(FACE_TEXTURES.length)];

            // Select poster based on player's faction (show opposite faction)
            FactionManager.Faction playerFaction = FactionManager.getPlayerFaction(client.player.getUuid());
            if (playerFaction == FactionManager.Faction.COMMUNIST) {
                currentPosterTexture = CAPITALIST_POSTERS[RANDOM.nextInt(CAPITALIST_POSTERS.length)];
            } else if (playerFaction == FactionManager.Faction.CAPITALIST) {
                currentPosterTexture = COMMUNIST_POSTERS[RANDOM.nextInt(COMMUNIST_POSTERS.length)];
            }

            lastSoundTime = currentTime;
        }

        // Change face texture randomly every 3-7 seconds
        if (currentTime - effectStartTime > 3000 && RANDOM.nextInt(100) < 2) {
            currentFaceTexture = FACE_TEXTURES[RANDOM.nextInt(FACE_TEXTURES.length)];
        }

        // Play random sounds periodically
        if (currentTime - lastSoundTime > SOUND_INTERVAL) {
            SoundEvent sound = SoundEvent.of(SOUND_IDS[RANDOM.nextInt(SOUND_IDS.length)]);
            client.getSoundManager().play(PositionedSoundInstance.master(sound, 1.0f));
            lastSoundTime = currentTime;
        }

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        // Render rainbow overlay
        renderRainbowOverlay(context, width, height, currentTime);

        // Render face texture (semi-transparent, appears randomly)
        if (currentFaceTexture != null && RANDOM.nextInt(100) < 30) { // 30% chance each frame
            renderFaceTexture(context, currentFaceTexture, width, height);
        }

        // Render poster texture (in corner)
        if (currentPosterTexture != null) {
            renderPosterTexture(context, currentPosterTexture, width, height);
        }
    }

    private static void renderRainbowOverlay(DrawContext context, int width, int height, long currentTime) {
        float hueShift = (currentTime % 10000) / 10000.0f; // Cycle every 10 seconds

        for (int i = 0; i < 7; i++) {
            float hue = (hueShift + i * 0.14f) % 1.0f;
            int color = hsvToRgb(hue, 0.5f, 1.0f);
            int alpha = 0x20; // Low alpha for subtle effect
            int colorWithAlpha = (alpha << 24) | (color & 0xFFFFFF);

            int barHeight = height / 7;
            context.fill(0, i * barHeight, width, (i + 1) * barHeight, colorWithAlpha);
        }
    }

    private static void renderFaceTexture(DrawContext context, Identifier texture, int width, int height) {
        float alpha = 0.3f + (RANDOM.nextFloat() * 0.2f); // 0.3-0.5 alpha, flickering
        int alphaInt = (int) (alpha * 255);
        int color = (alphaInt << 24) | 0xFFFFFF;

        // Random position offset for glitchy effect
        int offsetX = RANDOM.nextInt(20) - 10;
        int offsetY = RANDOM.nextInt(20) - 10;

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            texture,
            offsetX, offsetY,
            0.0F, 0.0F,
            width, height,
            width, height,
            color
        );
    }

    private static void renderPosterTexture(DrawContext context, Identifier texture, int width, int height) {
        int posterWidth = 128;
        int posterHeight = 128;
        int x = width - posterWidth - 10; // Bottom right corner
        int y = height - posterHeight - 10;

        float alpha = 0.6f + (float) Math.sin(System.currentTimeMillis() / 500.0) * 0.2f; // Pulsing
        int alphaInt = (int) (Math.max(0.4f, Math.min(0.8f, alpha)) * 255);
        int color = (alphaInt << 24) | 0xFFFFFF;

        context.drawTexture(
            RenderPipelines.GUI_TEXTURED,
            texture,
            x, y,
            0.0F, 0.0F,
            posterWidth, posterHeight,
            posterWidth, posterHeight,
            color
        );
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        int h = (int) (hue * 6);
        float f = hue * 6 - h;
        float p = value * (1 - saturation);
        float q = value * (1 - f * saturation);
        float t = value * (1 - (1 - f) * saturation);

        float r, g, b;
        switch (h % 6) {
            case 0: r = value; g = t; b = p; break;
            case 1: r = q; g = value; b = p; break;
            case 2: r = p; g = value; b = t; break;
            case 3: r = p; g = q; b = value; break;
            case 4: r = t; g = p; b = value; break;
            case 5: r = value; g = p; b = q; break;
            default: r = g = b = 0; break;
        }

        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}
