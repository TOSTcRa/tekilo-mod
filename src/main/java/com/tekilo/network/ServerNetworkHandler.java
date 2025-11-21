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
import net.minecraft.util.math.BlockPos;

public class ServerNetworkHandler {
    private static final double MAX_INTERACTION_DISTANCE = 8.0;
    private static final int MAX_STRING_LENGTH = 64;

    private static boolean isWithinDistance(ServerPlayerEntity player, BlockPos pos) {
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= MAX_INTERACTION_DISTANCE * MAX_INTERACTION_DISTANCE;
    }

    private static boolean canEditItemSpawner(ServerPlayerEntity player) {
        return player.hasPermissionLevel(2); // OP level 2+
    }

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

                    // Validate dimensions
                    if (payload.width() < 1 || payload.width() > 6 || payload.height() < 1 || payload.height() > 6) {
                        return;
                    }

                    // Check proximity
                    if (!isWithinDistance(player, payload.pos())) {
                        return;
                    }

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
                        return;
                    }

                    // Validate dimensions (already checked in payload deserialization, but double-check)
                    if (payload.canvasWidth() < 1 || payload.canvasWidth() > 6 || payload.canvasHeight() < 1 || payload.canvasHeight() > 6) {
                        return;
                    }

                    // Check proximity
                    if (!isWithinDistance(player, payload.pos())) {
                        return;
                    }

                    BlockEntity be = world.getBlockEntity(payload.pos());
                    if (be instanceof CanvasBlockEntity canvas) {
                        int expectedSize = payload.canvasWidth() * 16 * payload.canvasHeight() * 16;
                        if (payload.pixels().length != expectedSize) {
                            return;
                        }

                        // Проверяем что размеры совпадают с BlockEntity
                        if (canvas.getCanvasWidth() != payload.canvasWidth() || canvas.getCanvasHeight() != payload.canvasHeight()) {
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

                    // Check operator permission
                    if (!canEditItemSpawner(player)) {
                        player.sendMessage(Text.literal("§cУ вас нет прав для изменения настроек Item Spawner!"), false);
                        return;
                    }

                    ServerWorld world = player.getEntityWorld();
                    if (world == null) return;

                    // Validate payload values
                    if (payload.radius() < 10 || payload.radius() > 500 ||
                        payload.spawnInterval() < 200 || payload.spawnInterval() > 72000 ||
                        payload.itemCount() < 1 || payload.itemCount() > 64) {
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
                    }
                });
            }
        );

        // Обработчик настроек Zone Capture
        ServerPlayNetworking.registerGlobalReceiver(
            ZoneSettingsPayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (player == null) return;

                    // Check operator permission
                    if (!canEditItemSpawner(player)) {
                        player.sendMessage(Text.literal("§cУ вас нет прав для изменения настроек зоны!"), false);
                        return;
                    }

                    ServerWorld world = player.getEntityWorld();
                    if (world == null) return;

                    // Validate string lengths to prevent DoS
                    if (payload.zoneName() != null && payload.zoneName().length() > MAX_STRING_LENGTH) {
                        return;
                    }
                    if (payload.captureReward() != null && payload.captureReward().length() > MAX_STRING_LENGTH) {
                        return;
                    }

                    // Validate payload values
                    if (payload.zoneRadius() < 10 || payload.zoneRadius() > 1000 ||
                        payload.baseCaptureTime() < 1200 || payload.baseCaptureTime() > 72000 ||
                        payload.minCaptureTime() < 600 || payload.minCaptureTime() > payload.baseCaptureTime() ||
                        payload.bossBarColor() < 0 || payload.bossBarColor() > 6) {
                        return;
                    }

                    BlockEntity be = world.getBlockEntity(payload.pos());
                    if (be instanceof ItemSpawnerBlockEntity spawner) {
                        // Обновляем настройки зоны
                        spawner.setZoneRadius(payload.zoneRadius());
                        spawner.setBaseCaptureTime(payload.baseCaptureTime());
                        spawner.setMinCaptureTime(payload.minCaptureTime());
                        spawner.setZoneEnabled(payload.zoneEnabled());
                        spawner.setZoneName(payload.zoneName());
                        spawner.setBossBarColor(payload.bossBarColor());
                        spawner.setCaptureReward(payload.captureReward());
                    }
                });
            }
        );

        // Обработчик связывания спавнеров
        ServerPlayNetworking.registerGlobalReceiver(
            SpawnerLinkPayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (player == null) return;

                    ServerWorld world = player.getEntityWorld();
                    if (world == null) return;

                    // Validate unlock delay (0, 15, 30, 45, 60 minutes in seconds)
                    int delay = payload.unlockDelaySeconds();
                    if (delay != 0 && delay != 15 * 60 && delay != 30 * 60 && delay != 45 * 60 && delay != 60 * 60) {
                        return;
                    }

                    // Check if parent and child are not the same
                    if (payload.parentPos().equals(payload.childPos())) {
                        player.sendMessage(
                            Text.translatable("item.tekilo.spawner_linker.same_spawner")
                            .formatted(net.minecraft.util.Formatting.RED),
                            false
                        );
                        return;
                    }

                    // Check if both spawners exist
                    BlockEntity parentBe = world.getBlockEntity(payload.parentPos());
                    BlockEntity childBe = world.getBlockEntity(payload.childPos());

                    if (!(parentBe instanceof ItemSpawnerBlockEntity) || !(childBe instanceof ItemSpawnerBlockEntity childSpawner)) {
                        player.sendMessage(
                            Text.translatable("item.tekilo.spawner_linker.parent_not_found")
                            .formatted(net.minecraft.util.Formatting.RED),
                            false
                        );
                        return;
                    }

                    // Check distance between spawners (prevent abuse)
                    double distance = Math.sqrt(payload.parentPos().getSquaredDistance(payload.childPos()));
                    if (distance > 1000) {
                        player.sendMessage(
                            Text.literal("§cSpawners are too far apart! Maximum distance: 1000 blocks"),
                            false
                        );
                        return;
                    }

                    // Link the spawners
                    childSpawner.addParentSpawner(payload.parentPos(), delay);

                    // Send success message
                    player.sendMessage(
                        Text.translatable("item.tekilo.spawner_linker.linked",
                            payload.parentPos().getX(), payload.parentPos().getY(), payload.parentPos().getZ(),
                            payload.childPos().getX(), payload.childPos().getY(), payload.childPos().getZ())
                        .formatted(net.minecraft.util.Formatting.GREEN),
                        false
                    );

                    // Spawn particles at both locations
                    for (int i = 0; i < 10; i++) {
                        double x = payload.parentPos().getX() + 0.5 + (world.random.nextDouble() - 0.5);
                        double y = payload.parentPos().getY() + 0.5 + world.random.nextDouble();
                        double z = payload.parentPos().getZ() + 0.5 + (world.random.nextDouble() - 0.5);
                        world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
                    }
                    for (int i = 0; i < 10; i++) {
                        double x = payload.childPos().getX() + 0.5 + (world.random.nextDouble() - 0.5);
                        double y = payload.childPos().getY() + 0.5 + world.random.nextDouble();
                        double z = payload.childPos().getZ() + 0.5 + (world.random.nextDouble() - 0.5);
                        world.spawnParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
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
