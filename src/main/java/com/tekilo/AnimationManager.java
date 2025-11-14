package com.tekilo;

import com.tekilo.network.PlayAnimationPayload;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Менеджер анимаций для игроков (серверная сторона)
 */
public class AnimationManager {

    // Запускаем анимацию нагибания (для "Зад")
    public static void startBendAnimation(ServerWorld world, ServerPlayerEntity player, int durationTicks) {
        // Отправляем пакет всем игрокам поблизости
        broadcastAnimation(world, player, "bend", durationTicks);
    }

    // Запускаем анимацию вставания на колени (для "Рот")
    public static void startKneelAnimation(ServerWorld world, ServerPlayerEntity player, int durationTicks) {
        // Отправляем пакет всем игрокам поблизости
        broadcastAnimation(world, player, "kneel", durationTicks);
    }

    // Запускаем кастомную анимацию по имени
    public static void startCustomAnimation(ServerWorld world, ServerPlayerEntity player, String animationName, int durationTicks) {
        // Отправляем пакет всем игрокам поблизости
        broadcastAnimation(world, player, animationName, durationTicks);
    }

    /**
     * Отправить пакет воспроизведения анимации всем игрокам поблизости
     */
    private static void broadcastAnimation(ServerWorld world, ServerPlayerEntity targetPlayer, String animationName, int duration) {
        PlayAnimationPayload payload = new PlayAnimationPayload(
            targetPlayer.getUuid(),
            animationName,
            duration
        );

        // Отправляем всем игрокам, которые могут видеть целевого игрока
        for (ServerPlayerEntity viewer : PlayerLookup.tracking(targetPlayer)) {
            ServerPlayNetworking.send(viewer, payload);
        }

        // Также отправляем самому игроку
        ServerPlayNetworking.send(targetPlayer, payload);
    }

    // Обновление анимаций (вызывается каждый тик)
    // Player Animator управляет анимациями автоматически на клиенте
    public static void tick() {
        // Больше не требуется, анимации управляются Player Animator API
    }
}
