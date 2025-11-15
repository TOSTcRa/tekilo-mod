package com.tekilo.animation;

import java.util.HashMap;
import java.util.Map;

/**
 * Хранит данные анимации из JSON файла
 */
public class AnimationData {
    private final String name;
    private final float animationLength; // в секундах
    private final Map<String, BoneAnimation> bones; // имя кости -> анимация кости

    public AnimationData(String name, float animationLength) {
        this.name = name;
        this.animationLength = animationLength;
        this.bones = new HashMap<>();
    }

    public void addBoneAnimation(String boneName, BoneAnimation animation) {
        bones.put(boneName, animation);
    }

    public BoneAnimation getBoneAnimation(String boneName) {
        return bones.get(boneName);
    }

    public float getAnimationLength() {
        return animationLength;
    }

    public String getName() {
        return name;
    }

    public int getAnimationLengthTicks() {
        return (int) (animationLength * 20); // секунды в тики
    }
}
