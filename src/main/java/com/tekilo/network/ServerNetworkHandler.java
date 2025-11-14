package com.tekilo.network;

import com.tekilo.FactionManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class ServerNetworkHandler {
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(
            HoneyActionPayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();

                    // Проверяем, что игрок коммунист
                    if (!FactionManager.isCommunist(player.getUuid())) {
                        player.sendMessage(Text.literal("§cТолько коммунисты могут это делать!"), false);
                        return;
                    }

                    // Находим целевую сущность во всех мирах
                    Entity target = null;
                    for (ServerWorld world : context.server().getWorlds()) {
                        target = world.getEntityById(payload.entityId());
                        if (target != null) {
                            performAction(player, target, payload.getAction(), world);
                            break;
                        }
                    }
                });
            }
        );
    }

    private static void performAction(ServerPlayerEntity player, Entity target, HoneyActionPayload.Action action, ServerWorld world) {
        // Анимация махания рукой для всех игроков
        player.swingHand(Hand.MAIN_HAND, true);

        // Звуковые эффекты и партиклы в зависимости от действия
        switch (action) {
            case MOUTH -> {
                // Эффект для "Рот"
                world.playSound(
                    null,
                    target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_GENERIC_DRINK,
                    SoundCategory.PLAYERS,
                    1.0f, 1.0f
                );

                // Партиклы меда вокруг головы сущности
                world.spawnParticles(
                    ParticleTypes.FALLING_HONEY,
                    target.getX(),
                    target.getY() + target.getHeight() * 0.8,
                    target.getZ(),
                    20, // количество
                    0.3, 0.3, 0.3, // разброс
                    0.0 // скорость
                );

                // Если цель - игрок, запускаем анимацию кормления
                if (target instanceof ServerPlayerEntity targetPlayer) {
                    com.tekilo.AnimationManager.startCustomAnimation(world, targetPlayer, "mouth_feed", 60); // 3 секунды (60 тиков)
                }

                player.sendMessage(Text.literal("§eВы накормили сущность мёдом!"), true);
            }
            case BACK -> {
                // Эффект для "Зад"
                world.playSound(
                    null,
                    target.getX(), target.getY(), target.getZ(),
                    SoundEvents.BLOCK_HONEY_BLOCK_SLIDE,
                    SoundCategory.PLAYERS,
                    1.0f, 0.8f
                );

                // Партиклы меда сзади сущности
                world.spawnParticles(
                    ParticleTypes.DRIPPING_HONEY,
                    target.getX(),
                    target.getY() + target.getHeight() * 0.3,
                    target.getZ(),
                    15, // количество
                    0.2, 0.2, 0.2, // разброс
                    0.0 // скорость
                );

                // Если цель - игрок, запускаем анимацию нагибания
                if (target instanceof ServerPlayerEntity targetPlayer) {
                    com.tekilo.AnimationManager.startBendAnimation(world, targetPlayer, 60); // 3 секунды (60 тиков)
                }

                player.sendMessage(Text.literal("§eВы смазали сущность мёдом сзади!"), true);
            }
        }

        // Дополнительные эффекты
        // Анимация "повреждения" для целевой сущности (красная вспышка)
        world.sendEntityStatus(target, (byte) 2);

        // Звук удовлетворения
        world.playSound(
            null,
            player.getX(), player.getY(), player.getZ(),
            SoundEvents.ENTITY_PLAYER_BURP,
            SoundCategory.PLAYERS,
            0.5f, 1.2f
        );
    }
}
