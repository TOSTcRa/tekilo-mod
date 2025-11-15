package com.tekilo.mixin.client;

import com.tekilo.animation.AnimationData;
import com.tekilo.animation.PlayerEntityRenderStateExt;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Mixin для добавления данных анимации в PlayerEntityRenderState
 */
@Mixin(PlayerEntityRenderState.class)
public class PlayerEntityRenderStateMixin implements PlayerEntityRenderStateExt {

    @Unique
    private AnimationData tekilo$animationData = null;

    @Unique
    private float tekilo$animationTime = 0.0f;

    @Override
    public void tekilo$setAnimationData(AnimationData animation, float currentTime) {
        this.tekilo$animationData = animation;
        this.tekilo$animationTime = currentTime;
    }

    @Override
    public AnimationData tekilo$getAnimationData() {
        return this.tekilo$animationData;
    }

    @Override
    public float tekilo$getAnimationTime() {
        return this.tekilo$animationTime;
    }
}
