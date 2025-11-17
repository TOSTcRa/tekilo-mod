package com.tekilo;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerEventHandler {
    private static ScheduledExecutorService autoSaveExecutor;

    public static void register() {
        // Синхронизация фракции при входе игрока
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            FactionManager.syncToClient(handler.getPlayer());

            // Добавляем игрока в команду на основе его фракции и типа
            FactionManager.Faction faction = FactionManager.getPlayerFaction(handler.getPlayer().getUuid());
            boolean isSpy = FactionManager.isSpy(handler.getPlayer().getUuid());
            TeamManager.addPlayerToTeam(server, handler.getPlayer(), faction, isSpy);
        });

        // Очистка при отключении игрока
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            java.util.UUID playerId = handler.getPlayer().getUuid();
            MaskCommand.cleanupPlayer(playerId);
            FactionDeathHandler.cleanupPlayer(playerId);
            ZoneCaptureManager.cleanupPlayer(playerId);
        });

        // Загрузка фракций и инициализация команд при старте сервера
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Инициализируем команды
            TeamManager.initializeTeams(server);
            System.out.println("[TekiloMod] Teams initialized");

            // Загружаем фракции
            FactionPersistence.load(server);
            System.out.println("[TekiloMod] Factions loaded from file");
        });

        // Сохранение фракций при остановке сервера
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // Shutdown auto-save executor
            if (autoSaveExecutor != null && !autoSaveExecutor.isShutdown()) {
                autoSaveExecutor.shutdown();
                try {
                    if (!autoSaveExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        autoSaveExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    autoSaveExecutor.shutdownNow();
                }
                autoSaveExecutor = null;
            }

            FactionPersistence.save(server);
            SpyLeakNotifier.shutdown();
            MaskCommand.shutdown();
            System.out.println("[TekiloMod] Factions saved to file and all executors shutdown");
        });

        // Периодическое сохранение фракций (каждые 5 минут)
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Prevent duplicate executor creation
            if (autoSaveExecutor != null && !autoSaveExecutor.isShutdown()) {
                return;
            }

            autoSaveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TekiloMod-AutoSave");
                t.setDaemon(true);
                return t;
            });

            autoSaveExecutor.scheduleAtFixedRate(() -> {
                if (!server.isStopped()) {
                    server.execute(() -> {
                        FactionPersistence.save(server);
                        System.out.println("[TekiloMod] Factions auto-saved");
                    });
                }
            }, 5, 5, TimeUnit.MINUTES);
        });
    }
}
