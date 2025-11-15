package com.tekilo.mixin.client;

import com.tekilo.animation.AnimationData;
import com.tekilo.animation.AnimationManager;
import com.tekilo.animation.PlayerAnimationState;
import com.tekilo.animation.PlayerEntityRenderStateExt;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для сохранения данных анимации в PlayerEntityRenderState
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {

    /**
     * Перехватываем updateRenderState и сохраняем данные анимации в render state
     * В версии 1.21.2+ рендеринг был переработан, теперь используется updateRenderState
     */
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("RETURN"))
    private void onUpdateRenderState(PlayerLikeEntity player,
                                     PlayerEntityRenderState state,
                                     float tickDelta,
                                     CallbackInfo ci) {
        if (player == null || state == null) {
            return;
        }

        try {
            // AnimationManager expects AbstractClientPlayerEntity, but we have PlayerLikeEntity
            // PlayerLikeEntity is the parent class, so we need to check if it's actually a client player
            if (!(player instanceof net.minecraft.client.network.AbstractClientPlayerEntity)) {
                return;
            }

            net.minecraft.client.network.AbstractClientPlayerEntity clientPlayer =
                (net.minecraft.client.network.AbstractClientPlayerEntity) player;

            PlayerAnimationState animState = AnimationManager.getState(clientPlayer);
            if (animState == null || !animState.isPlaying()) {
                return;
            }

            AnimationData animation = animState.getCurrentAnimation();
            if (animation == null) {
                return;
            }

            float currentTime = animState.getCurrentTime();

            // Сохраняем данные анимации в render state
            ((PlayerEntityRenderStateExt) state).tekilo$setAnimationData(animation, currentTime);
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
}
