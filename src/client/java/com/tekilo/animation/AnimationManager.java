package com.tekilo.animation;

import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер анимаций для всех игроков
 */
public class AnimationManager {
    private static final Map<UUID, PlayerAnimationState> PLAYER_ANIMATIONS = new ConcurrentHashMap<>();

    /**
     * Воспроизвести анимацию для игрока
     */
    public static void playAnimation(AbstractClientPlayerEntity player, AnimationData animation, boolean loop) {
        UUID uuid = player.getUuid();
        PlayerAnimationState state = PLAYER_ANIMATIONS.computeIfAbsent(uuid, k -> new PlayerAnimationState());
        state.play(animation, loop);
    }

    /**
     * Остановить анимацию игрока
     */
    public static void stopAnimation(AbstractClientPlayerEntity player) {
        UUID uuid = player.getUuid();
        PlayerAnimationState state = PLAYER_ANIMATIONS.get(uuid);
        if (state != null) {
            state.stop();
        }
    }

    /**
     * Получить состояние анимации игрока
     */
    public static PlayerAnimationState getState(AbstractClientPlayerEntity player) {
        return PLAYER_ANIMATIONS.computeIfAbsent(player.getUuid(), k -> new PlayerAnimationState());
    }

    /**
     * Очистить все анимации
     */
    public static void clear() {
        PLAYER_ANIMATIONS.clear();
    }

    /**
     * Удалить анимацию игрока (при отключении)
     */
    public static void removePlayer(UUID playerId) {
        PlayerAnimationState state = PLAYER_ANIMATIONS.remove(playerId);
        if (state != null) {
            state.stop();
        }
    }

    /**
     * Очистить завершенные анимации (предотвращение утечки памяти)
     */
    public static void cleanupStoppedAnimations() {
        PLAYER_ANIMATIONS.entrySet().removeIf(entry -> !entry.getValue().isPlaying());
    }
}
