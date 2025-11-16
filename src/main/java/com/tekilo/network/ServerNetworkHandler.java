package com.tekilo.network;

import com.tekilo.CanvasBlockEntity;
import com.tekilo.CanvasPaintingItem;
import com.tekilo.FactionManager;
import com.tekilo.ItemSpawnerBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

public class ServerNetworkHandler {
    public static void register() {
        // Регистрация обработчика анимаций
        ServerPlayNetworking.registerGlobalReceiver(
            PlayAnimationPayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity sender = context.player();

                    // Создаем пакет для отправки всем остальным игрокам
                    PlayAnimationPayload broadcastPayload = new PlayAnimationPayload(
                        sender.getUuid(),
                        payload.animationName(),
                        payload.loop()
                    );

                    // Отправляем анимацию всем игрокам кроме отправителя
                    for (ServerPlayerEntity player : context.server().getPlayerManager().getPlayerList()) {
                        if (player != sender) {
                            ServerPlayNetworking.send(player, broadcastPayload);
                        }
                    }

                    System.out.println("[TekiloMod] Broadcasting animation '" + payload.animationName()
                        + "' from player " + sender.getName().getString() + " to all clients");
                });
            }
        );

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

        // Обработчик установки размера холста
        ServerPlayNetworking.registerGlobalReceiver(
            CanvasSizePayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (player == null) return;

                    ServerWorld world = player.getEntityWorld();
                    if (world == null) return;

                    BlockEntity be = world.getBlockEntity(payload.pos());
                    if (be instanceof CanvasBlockEntity canvas) {
                        if (!canvas.isSizeChosen()) {
                            canvas.setCanvasSize(payload.width(), payload.height());

                            // Отправляем обновленный payload с размерами обратно клиенту
                            OpenCanvasScreenPayload openPayload = new OpenCanvasScreenPayload(
                                payload.pos(),
                                canvas.getPixels(),
                                canvas.getCanvasWidth(),
                                canvas.getCanvasHeight(),
                                canvas.isSizeChosen()
                            );
                            ServerPlayNetworking.send(player, openPayload);

                            System.out.println("[TekiloMod] Canvas size set to " + payload.width() + "x" + payload.height() + " at " + payload.pos());
                        }
                    }
                });
            }
        );

        // Обработчик обновления холста
        ServerPlayNetworking.registerGlobalReceiver(
            CanvasUpdatePayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (player == null) return;

                    ServerWorld world = player.getEntityWorld();
                    if (world == null) return;

                    // Validate pixel array
                    if (payload.pixels() == null || payload.pixels().length == 0) {
                        System.err.println("[TekiloMod] Invalid canvas payload from " + player.getName().getString());
                        return;
                    }

                    BlockEntity be = world.getBlockEntity(payload.pos());
                    if (be instanceof CanvasBlockEntity canvas) {
                        int expectedSize = payload.canvasWidth() * 16 * payload.canvasHeight() * 16;
                        if (payload.pixels().length != expectedSize) {
                            System.err.println("[TekiloMod] Invalid canvas pixel count: " + payload.pixels().length + ", expected " + expectedSize);
                            return;
                        }

                        // Проверяем что размеры совпадают с BlockEntity
                        if (canvas.getCanvasWidth() != payload.canvasWidth() || canvas.getCanvasHeight() != payload.canvasHeight()) {
                            System.err.println("[TekiloMod] Canvas size mismatch: payload=" + payload.canvasWidth() + "x" + payload.canvasHeight() + ", entity=" + canvas.getCanvasWidth() + "x" + canvas.getCanvasHeight());
                            return;
                        }

                        // Обновляем пиксели на сервере
                        canvas.setPixels(payload.pixels());

                        // Рассылаем обновление всем игрокам
                        for (ServerPlayerEntity p : context.server().getPlayerManager().getPlayerList()) {
                            if (p != player) {
                                ServerPlayNetworking.send(p, payload);
                            }
                        }

                        // Уведомляем о визуальном обновлении
                        world.updateListeners(payload.pos(), canvas.getCachedState(), canvas.getCachedState(), 3);

                        System.out.println("[TekiloMod] Canvas updated at " + payload.pos() + " by " + player.getName().getString() + " size=" + payload.canvasWidth() + "x" + payload.canvasHeight());

                        // Создаём и выдаём игроку Canvas Painting с нарисованным изображением
                        ItemStack paintingStack = CanvasPaintingItem.createWithPixels(payload.pixels(), payload.canvasWidth(), payload.canvasHeight());

                        // Пытаемся добавить в инвентарь
                        if (!player.getInventory().insertStack(paintingStack)) {
                            // Если инвентарь полон, бросаем на землю
                            ItemEntity itemEntity = new ItemEntity(
                                world,
                                player.getX(),
                                player.getY() + 0.5,
                                player.getZ(),
                                paintingStack
                            );
                            world.spawnEntity(itemEntity);
                        }

                        // Уведомляем игрока
                        player.sendMessage(Text.translatable("message.tekilo.canvas.painting_created"), true);

                        System.out.println("[TekiloMod] Canvas painting created for " + player.getName().getString() + " size=" + payload.canvasWidth() + "x" + payload.canvasHeight());
                    }
                });
            }
        );

        // Обработчик настроек Item Spawner
        ServerPlayNetworking.registerGlobalReceiver(
            ItemSpawnerSettingsPayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (player == null) return;

                    ServerWorld world = player.getEntityWorld();
                    if (world == null) return;

                    // Validate payload values
                    if (payload.radius() < 10 || payload.radius() > 500 ||
                        payload.spawnInterval() < 200 || payload.spawnInterval() > 72000 ||
                        payload.itemCount() < 1 || payload.itemCount() > 64) {
                        System.err.println("[TekiloMod] Invalid Item Spawner settings from " + player.getName().getString());
                        return;
                    }

                    BlockEntity be = world.getBlockEntity(payload.pos());
                    if (be instanceof ItemSpawnerBlockEntity spawner) {
                        // Обновляем настройки
                        spawner.setRadius(payload.radius());
                        spawner.setSpawnInterval(payload.spawnInterval());
                        spawner.setItemCount(payload.itemCount());
                        spawner.setSpawnInChests(payload.spawnInChests());
                        spawner.setUseGlobalSettings(payload.useGlobalSettings());
                        spawner.setEnabled(payload.enabled());

                        System.out.println("[TekiloMod] Item Spawner settings updated at " + payload.pos()
                            + " by " + player.getName().getString()
                            + " - radius=" + payload.radius()
                            + ", interval=" + payload.spawnInterval()
                            + ", count=" + payload.itemCount()
                            + ", enabled=" + payload.enabled());
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

                player.sendMessage(Text.literal("§eВы накормили сущность мёдом!"), true);

                // Запускаем анимацию kneel для цели (если это игрок)
                if (target instanceof ServerPlayerEntity targetPlayer) {
                    PlayAnimationPayload animPayload = new PlayAnimationPayload(
                        targetPlayer.getUuid(),
                        "kneel",
                        false
                    );
                    // Отправляем всем игрокам
                    for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(p, animPayload);
                    }
                }
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

                player.sendMessage(Text.literal("§eВы смазали сущность мёдом сзади!"), true);

                // Запускаем анимацию walk для игрока, который кликнул
                PlayAnimationPayload playerAnimPayload = new PlayAnimationPayload(
                    player.getUuid(),
                    "walk",
                    false
                );
                for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
                    ServerPlayNetworking.send(p, playerAnimPayload);
                }

                // Запускаем анимацию bow для цели (если это игрок)
                if (target instanceof ServerPlayerEntity targetPlayer) {
                    PlayAnimationPayload targetAnimPayload = new PlayAnimationPayload(
                        targetPlayer.getUuid(),
                        "bow",
                        false
                    );
                    for (ServerPlayerEntity p : world.getServer().getPlayerManager().getPlayerList()) {
                        ServerPlayNetworking.send(p, targetAnimPayload);
                    }
                }
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
