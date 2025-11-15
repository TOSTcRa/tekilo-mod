package com.tekilo;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class ServerEventHandler {
    public static void register() {
        // Синхронизация фракции при входе игрока
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            FactionManager.syncToClient(handler.getPlayer());

            // Добавляем игрока в команду на основе его фракции
            FactionManager.Faction faction = FactionManager.getPlayerFaction(handler.getPlayer().getUuid());
            TeamManager.addPlayerToTeam(server, handler.getPlayer(), faction);
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
            FactionPersistence.save(server);
            System.out.println("[TekiloMod] Factions saved to file");
        });

        // Периодическое сохранение фракций (каждые 5 минут)
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            new Thread(() -> {
                while (!server.isStopped()) {
                    try {
                        Thread.sleep(300000); // 5 минут
                        if (!server.isStopped()) {
                            FactionPersistence.save(server);
                            System.out.println("[TekiloMod] Factions auto-saved");
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }).start();
        });
    }
}
