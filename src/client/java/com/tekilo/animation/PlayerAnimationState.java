package com.tekilo.animation;

/**
 * Состояние анимации для одного игрока
 */
public class PlayerAnimationState {
    private AnimationData currentAnimation;
    private long startTime; // Время начала анимации в миллисекундах
    private boolean isPlaying;
    private boolean loop;

    public PlayerAnimationState() {
        this.isPlaying = false;
        this.loop = false;
    }

    /**
     * Начать воспроизведение анимации
     */
    public void play(AnimationData animation, boolean loop) {
        this.currentAnimation = animation;
        this.startTime = System.currentTimeMillis();
        this.isPlaying = true;
        this.loop = loop;
    }

    /**
     * Остановить анимацию
     */
    public void stop() {
        this.isPlaying = false;
        this.currentAnimation = null;
    }

    /**
     * Получить текущее время анимации в секундах
     */
    public float getCurrentTime() {
        if (!isPlaying || currentAnimation == null) {
            return 0;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        float time = elapsed / 1000.0f;

        // Если анимация закончилась
        if (time >= currentAnimation.getAnimationLength()) {
            if (loop) {
                // Зациклить анимацию
                startTime = System.currentTimeMillis();
                return 0;
            } else {
                // Остановить
                stop();
                return 0;
            }
        }

        return time;
    }

    /**
     * Получить текущую анимацию
     */
    public AnimationData getCurrentAnimation() {
        return currentAnimation;
    }

    /**
     * Проверить, воспроизводится ли анимация
     */
    public boolean isPlaying() {
        return isPlaying && currentAnimation != null;
    }

    /**
     * Получить прогресс анимации (0.0 - 1.0)
     */
    public float getProgress() {
        if (!isPlaying || currentAnimation == null) {
            return 0;
        }

        float currentTime = getCurrentTime();
        return currentTime / currentAnimation.getAnimationLength();
    }
}
