package com.tekilo;

import com.tekilo.network.FactionSyncPayload;
import com.tekilo.network.PlayAnimationPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class TekiloMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ModItems.initialize();

        // Регистрация сетевых пакетов
        PayloadTypeRegistry.playS2C().register(FactionSyncPayload.ID, FactionSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(com.tekilo.network.HoneyActionPayload.ID, com.tekilo.network.HoneyActionPayload.CODEC);

        // Регистрация пакета анимаций (двунаправленный)
        PayloadTypeRegistry.playC2S().register(PlayAnimationPayload.ID, PlayAnimationPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PlayAnimationPayload.ID, PlayAnimationPayload.CODEC);

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
