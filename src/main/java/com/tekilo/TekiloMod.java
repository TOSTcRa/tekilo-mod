package com.tekilo;

import com.tekilo.network.FactionSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class TekiloMod implements ModInitializer {

    @Override
    public void onInitialize() {
        ModItems.initialize();

        // Регистрация сетевых пакетов
        PayloadTypeRegistry.playS2C().register(FactionSyncPayload.ID, FactionSyncPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(com.tekilo.network.HoneyActionPayload.ID, com.tekilo.network.HoneyActionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(com.tekilo.network.PlayAnimationPayload.ID, com.tekilo.network.PlayAnimationPayload.CODEC);

        // Регистрация обработчиков сетевых пакетов
        com.tekilo.network.ServerNetworkHandler.register();

        // Регистрация обработчика кастомных смертей
        FactionDeathHandler.register();

        // Регистрация обработчика friendly fire
        FriendlyFireHandler.register();

        // Регистрация обработчиков событий сервера
        ServerEventHandler.register();

        System.out.println("[TekiloMod] Mod initialized!");
    }
}
