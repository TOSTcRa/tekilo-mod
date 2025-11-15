package com.tekilo.animation;

/**
 * Interface mixin для добавления поля анимации в PlayerEntityRenderState
 */
public interface PlayerEntityRenderStateExt {
    void tekilo$setAnimationData(AnimationData animation, float currentTime);
    AnimationData tekilo$getAnimationData();
    float tekilo$getAnimationTime();
}
