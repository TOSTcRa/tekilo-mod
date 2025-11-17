package com.tekilo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MaskCommand {
    private static final Map<UUID, Long> maskEndTime = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastUsed = new ConcurrentHashMap<>();
    private static final Map<UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private static final long MASK_DURATION_MS = 300000; // 5 минут
    private static final long COOLDOWN_MS = 600000; // 10 минут кулдаун
    private static ScheduledExecutorService maskScheduler;

    public static void register() {
        CommandRegistrationCallback.EVENT.register(MaskCommand::registerCommand);
    }

    private static void registerCommand(
        CommandDispatcher<ServerCommandSource> dispatcher,
        CommandRegistryAccess registryAccess,
        CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(CommandManager.literal("spyMask")
            .executes(MaskCommand::execute)
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
            UUID playerId = player.getUuid();
            MinecraftServer server = context.getSource().getServer();

            // Проверяем, является ли игрок шпионом
            if (!FactionManager.isSpy(playerId)) {
                player.sendMessage(Text.translatable("command.tekilo.mask.not_spy"), false);
                return 0;
            }

            FactionManager.Faction faction = FactionManager.getPlayerFaction(playerId);
            if (faction == FactionManager.Faction.NONE) {
                player.sendMessage(Text.translatable("command.tekilo.mask.no_faction"), false);
                return 0;
            }

            long currentTime = System.currentTimeMillis();

            // Проверяем, активна ли маска
            if (maskEndTime.containsKey(playerId)) {
                long remaining = maskEndTime.get(playerId) - currentTime;
                if (remaining > 0) {
                    long secondsLeft = remaining / 1000;
                    player.sendMessage(Text.translatable("command.tekilo.mask.already_active", secondsLeft), false);
                    return 0;
                }
            }

            // Проверяем кулдаун
            if (lastUsed.containsKey(playerId)) {
                long timeSinceLastUse = currentTime - lastUsed.get(playerId);
                if (timeSinceLastUse < COOLDOWN_MS) {
                    long secondsLeft = (COOLDOWN_MS - timeSinceLastUse) / 1000;
                    player.sendMessage(Text.translatable("command.tekilo.mask.cooldown", secondsLeft), false);
                    return 0;
                }
            }

            // Активируем маску - меняем тип игрока на NORMAL (становится "настоящим" членом фракции)
            FactionManager.setPlayerType(playerId, FactionManager.PlayerType.NORMAL);

            // Перемещаем в обычную команду фракции (не шпионскую)
            TeamManager.addPlayerToTeam(server, player, faction, false);

            FactionManager.syncToClient(player);

            // Устанавливаем время окончания маски
            maskEndTime.put(playerId, currentTime + MASK_DURATION_MS);
            lastUsed.put(playerId, currentTime);

            // Планируем снятие маски через 5 минут
            scheduleMaskRemoval(server, playerId, faction);

            String factionName = faction == FactionManager.Faction.COMMUNIST
                ? Text.translatable("faction.tekilo.communist").getString()
                : Text.translatable("faction.tekilo.capitalist").getString();

            player.sendMessage(Text.translatable("command.tekilo.mask.activated", factionName), false);

            FactionPersistence.save(server);
            return 1;
        } catch (Exception e) {
            context.getSource().sendError(Text.literal("Ошибка: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static void scheduleMaskRemoval(MinecraftServer server, UUID playerId, FactionManager.Faction faction) {
        // Initialize scheduler if needed
        if (maskScheduler == null || maskScheduler.isShutdown()) {
            maskScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TekiloMod-MaskScheduler");
                t.setDaemon(true);
                return t;
            });
        }

        // Cancel any existing task for this player
        ScheduledFuture<?> existingTask = scheduledTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel(false);
        }

        // Schedule new task
        ScheduledFuture<?> task = maskScheduler.schedule(() -> {
            // Проверяем, что маска всё ещё должна быть снята
            if (maskEndTime.containsKey(playerId)) {
                long endTime = maskEndTime.get(playerId);
                if (System.currentTimeMillis() >= endTime) {
                    // Выполняем на главном потоке сервера
                    server.execute(() -> {
                        ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                        if (player != null) {
                            // Возвращаем статус шпиона
                            FactionManager.setPlayerType(playerId, FactionManager.PlayerType.SPY);
                            TeamManager.addPlayerToTeam(server, player, faction, true);
                            FactionManager.syncToClient(player);

                            player.sendMessage(Text.translatable("command.tekilo.mask.deactivated"), false);
                            FactionPersistence.save(server);
                        }
                        maskEndTime.remove(playerId);
                        scheduledTasks.remove(playerId);
                    });
                }
            }
        }, MASK_DURATION_MS, TimeUnit.MILLISECONDS);

        scheduledTasks.put(playerId, task);
    }

    // Метод для проверки, активна ли маска
    public static boolean isMaskActive(UUID playerId) {
        if (maskEndTime.containsKey(playerId)) {
            return System.currentTimeMillis() < maskEndTime.get(playerId);
        }
        return false;
    }

    // Cleanup method for player disconnect
    public static void cleanupPlayer(UUID playerId) {
        maskEndTime.remove(playerId);
        lastUsed.remove(playerId);
        ScheduledFuture<?> task = scheduledTasks.remove(playerId);
        if (task != null) {
            task.cancel(false);
        }
    }

    // Shutdown method for server stop
    public static void shutdown() {
        if (maskScheduler != null && !maskScheduler.isShutdown()) {
            maskScheduler.shutdown();
            try {
                if (!maskScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    maskScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                maskScheduler.shutdownNow();
            }
            maskScheduler = null;
        }
        scheduledTasks.clear();
        maskEndTime.clear();
        lastUsed.clear();
    }
}
