package com.tekilo;

import com.tekilo.animation.AnimationResourceLoader;
import com.tekilo.render.CanvasBlockEntityRenderer;
import com.tekilo.render.CanvasPaintingTooltipComponent;
import com.tekilo.render.CanvasTextureManager;
import com.tekilo.render.DeliciousnessEffectRenderer;
import com.tekilo.render.ZoneVisualizationManager;
import com.tekilo.screen.FactionCollectorScreen;
import com.tekilo.screen.ItemSpawnerScreen;
import com.tekilo.screen.ItemSpawnerScreenHandler;
import com.tekilo.screen.SpawnerLinkScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.MinecraftClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundEvent;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.client.sound.PositionedSoundInstance;

import java.util.Random;

public class FaceFlashClientMod implements ClientModInitializer {
    private static final Identifier[] FACE_TEXTURES = {
            Identifier.of("tekilo", "textures/gui/friends_face_1.png"),
            Identifier.of("tekilo", "textures/gui/friends_face_2.png"),
            Identifier.of("tekilo", "textures/gui/friends_face_3.png"),
            Identifier.of("tekilo", "textures/gui/friends_face_4.png"),
            Identifier.of("tekilo", "textures/gui/friends_face_5.png"),
    };

    public static final Identifier[] SOUND_IDS = {
            Identifier.of("tekilo", "face_sound_1"),
            Identifier.of("tekilo", "face_sound_2"),
            Identifier.of("tekilo", "face_sound_3"),
    };

    private static final SoundEvent[] SOUND_EVENTS = new SoundEvent[SOUND_IDS.length];
    private static final Random RANDOM = new Random();

    private static long cycleStartTime = -1;
    private static Identifier currentTexture = null;
    private static boolean soundPlayed = false;

    @Override
    public void onInitializeClient() {
        // Регистрация обработчика сетевых пакетов
        com.tekilo.network.ClientNetworkHandler.register();

        // Регистрация обработчика бутылочки меда
        HoneyBottleUseHandler.register();

        // Регистрация ResourceReloadListener для автоматической загрузки анимаций
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES)
            .registerReloadListener(new AnimationResourceLoader());

        // Регистрация команды для тестирования анимаций
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            AnimationCommand.register(dispatcher);
        });

        // Регистрация GUI экранов
        HandledScreens.register(ModScreenHandlers.ITEM_SPAWNER_SCREEN_HANDLER, ItemSpawnerScreen::new);
        HandledScreens.register(ModScreenHandlers.FACTION_COLLECTOR_SCREEN_HANDLER, FactionCollectorScreen::new);
        HandledScreens.register(ModScreenHandlers.SPAWNER_LINK_SCREEN_HANDLER, SpawnerLinkScreen::new);

        // Регистрация BlockEntityRenderer для Canvas
        BlockEntityRendererFactories.register(ModBlockEntities.CANVAS, CanvasBlockEntityRenderer::new);

        // Инициализация менеджера текстур для Canvas
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            CanvasTextureManager.initialize();
        });

        // Очистка текстур и зон при выходе из мира
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            CanvasTextureManager.getInstance().clearAllTextures();
            ZoneVisualizationManager.clear();
        });

        // Регистрация кастомного TooltipComponent для Canvas Painting
        TooltipComponentCallback.EVENT.register(data -> {
            if (data instanceof CanvasPaintingItem.CanvasPaintingTooltipData paintingData) {
                return new CanvasPaintingTooltipComponent(paintingData.pixels());
            }
            return null;
        });

        for (int i = 0; i < SOUND_IDS.length; i++) {
            SOUND_EVENTS[i] = SoundEvent.of(SOUND_IDS[i]);
            Registry.register(Registries.SOUND_EVENT, SOUND_IDS[i], SOUND_EVENTS[i]);
        }

        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.player.getEntityWorld() == null) {
                return;
            }

            // Render deliciousness effect
            DeliciousnessEffectRenderer.renderEffect(drawContext, renderTickCounter);

            if (client.player.getHealth() <= 4.0F && client.player.isAlive()) {
                renderFaceOverlay(drawContext, client);
            } else {
                cycleStartTime = -1;
                soundPlayed = false;
                currentTexture = null;
            }
        });
    }

    private void renderFaceOverlay(DrawContext context, MinecraftClient client) {
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        long worldTime = client.player.getEntityWorld().getTime();
        long timeMs = worldTime * 50;

        long fadeInDuration = 1000;
        long fadeOutDuration = 2000;
        long pauseDuration = 7000;
        long fullCycleDuration = fadeInDuration + fadeOutDuration + pauseDuration;

        if (cycleStartTime == -1) {
            cycleStartTime = timeMs;
            currentTexture = FACE_TEXTURES[RANDOM.nextInt(FACE_TEXTURES.length)];
            soundPlayed = false;
        }

        long timeSinceCycleStart = timeMs - cycleStartTime;

        if (timeSinceCycleStart >= fullCycleDuration) {
            cycleStartTime = timeMs;
            currentTexture = FACE_TEXTURES[RANDOM.nextInt(FACE_TEXTURES.length)];
            soundPlayed = false;
            timeSinceCycleStart = 0;
        }

        float alpha = 0.0f;

        if (timeSinceCycleStart < fadeInDuration) {
            if (!soundPlayed) {
                SoundEvent randomSound = SOUND_EVENTS[RANDOM.nextInt(SOUND_EVENTS.length)];
                client.getSoundManager().play(PositionedSoundInstance.master(randomSound, 1.0f));
                soundPlayed = true;
            }

            float progress = (float) timeSinceCycleStart / fadeInDuration;
            alpha = progress;
        } else if (timeSinceCycleStart < fadeInDuration + fadeOutDuration) {
            long fadeOutTime = timeSinceCycleStart - fadeInDuration;
            float progress = (float) fadeOutTime / fadeOutDuration;
            alpha = 1.0f - progress;
        }

        if (alpha > 0.0f) {
            int alphaInt = (int)(alpha * 255);
            int color = (alphaInt << 24) | 0xFFFFFF;

            context.drawTexture(
                    RenderPipelines.GUI_TEXTURED,
                    currentTexture,
                    0, 0,
                    0.0F, 0.0F,
                    width, height,
                    width, height,
                    color
            );
        }
    }
}