package com.tekilo.animation;

import net.minecraft.client.network.AbstractClientPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Менеджер анимаций для всех игроков
 */
public class AnimationManager {
    private static final Map<UUID, PlayerAnimationState> PLAYER_ANIMATIONS = new HashMap<>();

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
}
