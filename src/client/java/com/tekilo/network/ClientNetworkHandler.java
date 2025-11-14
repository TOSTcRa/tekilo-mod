package com.tekilo.network;

import com.tekilo.FactionManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class ClientNetworkHandler {
    public static void register() {
        // Регистрация обработчика синхронизации фракций
        ClientPlayNetworking.registerGlobalReceiver(
            FactionSyncPayload.ID,
            (payload, context) -> {
                context.client().execute(() -> {
                    MinecraftClient client = context.client();
                    if (client.player != null) {
                        FactionManager.setPlayerFaction(
                            client.player.getUuid(),
                            payload.getFaction()
                        );
                    }
                });
            }
        );

        // Регистрация обработчика анимаций
        ClientAnimationHandler.register();
    }
}
