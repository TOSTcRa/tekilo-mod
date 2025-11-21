package com.tekilo.mixin.client;

import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static final Identifier[] FACE_TEXTURES = {
        Identifier.of("tekilo", "textures/entity/face_1.png"),
        Identifier.of("tekilo", "textures/entity/face_2.png"),
        Identifier.of("tekilo", "textures/entity/face_3.png"),
        Identifier.of("tekilo", "textures/entity/face_4.png"),
        Identifier.of("tekilo", "textures/entity/face_5.png"),
        Identifier.of("tekilo", "textures/entity/face_6.png"),
        Identifier.of("tekilo", "textures/entity/face_7.png"),
        Identifier.of("tekilo", "textures/entity/face_8.png")
    };

    @Inject(method = "render",
            at = @At("TAIL"))
    private void renderFace(LivingEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        // НЕ рендерим лица для игроков (у них высота 1.8)
        if (Math.abs(state.height - 1.8f) < 0.01f) {
            return;
        }

        // Простое решение: используем высоту как seed
        // Все мобы одного типа получат одно лицо, но это стабильно
        int seed = Float.floatToIntBits(state.height);
        Random random = new Random(seed);
        Identifier faceTexture = FACE_TEXTURES[random.nextInt(FACE_TEXTURES.length)];

        matrices.push();

        // Позиция головы зависит от типа моба
        // Высокие мобы (зомби, скелеты): голова выше (~85%)
        // Низкие/животные (корова, свинья): голова выше (~80%)
        float headHeightRatio = state.height > 1.5f ? 0.85f : 0.80f;
        float headY = state.height * headHeightRatio;
        matrices.translate(0, headY, 0);

        // Сначала поворачиваем лицо вместе с телом
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - state.bodyYaw));

        // Смещаем лицо вперед так, чтобы оно прилегало к морде
        // Для высоких мобов (зомби, скелеты) - 0.37
        // Для животных (корова, свинья) - 0.92
        float forwardOffset = state.height > 1.5f ? 0.37f : 0.92f;
        matrices.translate(0, 0, -forwardOffset);

        // Размер лица пропорционален размеру моба
        float faceSize = state.height * 0.22f;
        matrices.scale(faceSize, faceSize, 0.01f);

        // Используем MAX_LIGHT_COORDINATE для яркого света
        int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;

        // Submit render command to queue
        RenderLayer renderLayer = RenderLayer.getEntityCutoutNoCull(faceTexture);
        queue.submitCustom(matrices, renderLayer, (entry, consumer) -> {
            Matrix4f matrix = entry.getPositionMatrix();

            // Рисуем квад перед головой
            consumer.vertex(matrix, -1, 1, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .texture(0, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0, 0, 1);

            consumer.vertex(matrix, 1, 1, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .texture(1, 0)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0, 0, 1);

            consumer.vertex(matrix, 1, -1, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .texture(1, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0, 0, 1);

            consumer.vertex(matrix, -1, -1, 0)
                .color(1.0f, 1.0f, 1.0f, 1.0f)
                .texture(0, 1)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0, 0, 1);
        });

        matrices.pop();
    }
}
