package com.tekilo;

/**
 * Состояние анимации для игрока
 */
public class AnimationState {
    private String animationName; // Название текущей анимации ("kneel", "bend", или null)
    private long startTime; // Время начала анимации
    private int duration; // Длительность в тиках

    public AnimationState() {
        this.animationName = null;
        this.startTime = 0;
        this.duration = 0;
    }

    public void startAnimation(String name, int durationTicks) {
        this.animationName = name;
        this.startTime = System.currentTimeMillis();
        this.duration = durationTicks;
    }

    public void stopAnimation() {
        this.animationName = null;
        this.startTime = 0;
        this.duration = 0;
    }

    public boolean isAnimating() {
        if (animationName == null) return false;

        long elapsed = System.currentTimeMillis() - startTime;
        long maxDuration = duration * 50L; // тики в миллисекунды

        if (elapsed >= maxDuration) {
            stopAnimation();
            return false;
        }

        return true;
    }

    public String getCurrentAnimation() {
        return isAnimating() ? animationName : null;
    }

    public float getAnimationProgress() {
        if (!isAnimating()) return 0f;

        long elapsed = System.currentTimeMillis() - startTime;
        long maxDuration = duration * 50L;

        return Math.min(1f, (float) elapsed / maxDuration);
    }
}
