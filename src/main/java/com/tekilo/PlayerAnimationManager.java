package com.tekilo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер анимаций для всех игроков
 */
public class PlayerAnimationManager {
    private static final Map<UUID, AnimationState> playerAnimations = new ConcurrentHashMap<>();

    /**
     * Запустить анимацию для игрока
     */
    public static void startAnimation(UUID playerUuid, String animationName, int durationTicks) {
        AnimationState state = playerAnimations.computeIfAbsent(playerUuid, k -> new AnimationState());
        state.startAnimation(animationName, durationTicks);
    }

    /**
     * Остановить анимацию игрока
     */
    public static void stopAnimation(UUID playerUuid) {
        AnimationState state = playerAnimations.get(playerUuid);
        if (state != null) {
            state.stopAnimation();
        }
    }

    /**
     * Получить текущее состояние анимации игрока
     */
    public static AnimationState getAnimationState(UUID playerUuid) {
        return playerAnimations.computeIfAbsent(playerUuid, k -> new AnimationState());
    }

    /**
     * Проверить, воспроизводится ли анимация
     */
    public static boolean isAnimating(UUID playerUuid) {
        AnimationState state = playerAnimations.get(playerUuid);
        return state != null && state.isAnimating();
    }

    /**
     * Получить название текущей анимации
     */
    public static String getCurrentAnimation(UUID playerUuid) {
        AnimationState state = playerAnimations.get(playerUuid);
        return state != null ? state.getCurrentAnimation() : null;
    }

    /**
     * Очистка старых состояний
     */
    public static void cleanup() {
        playerAnimations.entrySet().removeIf(entry -> !entry.getValue().isAnimating());
    }
}
