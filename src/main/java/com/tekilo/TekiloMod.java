package com.tekilo;

import com.tekilo.network.CanvasSizePayload;
import com.tekilo.network.CanvasUpdatePayload;
import com.tekilo.network.FactionSyncPayload;
import com.tekilo.network.ItemSpawnerSettingsPayload;
import com.tekilo.network.OpenCanvasScreenPayload;
import com.tekilo.network.PlayAnimationPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class TekiloMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ModDataComponents.initialize();
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModItems.initialize();
        ModItemGroups.initialize();
        ModScreenHandlers.initialize();

        // Регистрация сетевых пакетов
        PayloadTypeRegistry.playS2C().register(FactionSyncPayload.ID, FactionSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(com.tekilo.network.HoneyActionPayload.ID, com.tekilo.network.HoneyActionPayload.CODEC);

        // Регистрация пакета анимаций (двунаправленный)
        PayloadTypeRegistry.playC2S().register(PlayAnimationPayload.ID, PlayAnimationPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayAnimationPayload.ID, PlayAnimationPayload.CODEC);

        // Регистрация пакета обновления холста (двунаправленный)
        PayloadTypeRegistry.playC2S().register(CanvasUpdatePayload.ID, CanvasUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CanvasUpdatePayload.ID, CanvasUpdatePayload.CODEC);

        // Регистрация пакета открытия экрана холста (S2C)
        PayloadTypeRegistry.playS2C().register(OpenCanvasScreenPayload.ID, OpenCanvasScreenPayload.CODEC);

        // Регистрация пакета установки размера холста (C2S)
        PayloadTypeRegistry.playC2S().register(CanvasSizePayload.ID, CanvasSizePayload.CODEC);

        // Регистрация пакета настроек Item Spawner (C2S)
        PayloadTypeRegistry.playC2S().register(ItemSpawnerSettingsPayload.ID, ItemSpawnerSettingsPayload.CODEC);

        // Регистрация обработчиков сетевых пакетов
        com.tekilo.network.ServerNetworkHandler.register();

        // Регистрация обработчика кастомных смертей
        FactionDeathHandler.register();

        // Регистрация обработчика friendly fire
        FriendlyFireHandler.register();

        // Регистрация обработчиков событий сервера
        ServerEventHandler.register();

        // Регистрация команд
        FactionCommand.register();
        TellInfoCommand.register();

        System.out.println("[TekiloMod] Mod initialized!");
    }
}
