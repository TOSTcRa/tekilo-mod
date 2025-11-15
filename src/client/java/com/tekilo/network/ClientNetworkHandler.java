package com.tekilo.network;

import com.tekilo.FactionManager;
import com.tekilo.animation.AnimationData;
import com.tekilo.animation.AnimationLoader;
import com.tekilo.animation.AnimationManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class ClientNetworkHandler {
    public static void register() {
        // Регистрация обработчика анимаций
        ClientPlayNetworking.registerGlobalReceiver(
            PlayAnimationPayload.ID,
            (payload, context) -> {
                context.client().execute(() -> {
                    MinecraftClient client = context.client();
                    ClientWorld world = client.world;

                    if (world == null) {
                        return;
                    }

                    // Находим игрока по UUID
                    PlayerEntity targetPlayer = world.getPlayerByUuid(payload.playerId());
                    if (!(targetPlayer instanceof AbstractClientPlayerEntity player)) {
                        return;
                    }

                    // Определяем путь к анимации
                    String animPath = switch (payload.animationName().toLowerCase()) {
                        case "kneel" -> "animations/kneel.animation.json";
                        case "bow" -> "animations/bow.animation.json";
                        case "walk" -> "animations/walk.animation.json";
                        default -> null;
                    };

                    if (animPath == null) {
                        System.err.println("[TekiloMod] Unknown animation: " + payload.animationName());
                        return;
                    }

                    // Загружаем и запускаем анимацию
                    Identifier animId = Identifier.of("tekilo", animPath);
                    AnimationData animation = AnimationLoader.loadAnimation(
                        client.getResourceManager(),
                        animId
                    );

                    if (animation != null) {
                        AnimationManager.playAnimation(player, animation, payload.loop());
                        System.out.println("[TekiloMod] Playing animation '" + payload.animationName()
                            + "' for player " + player.getName().getString());
                    } else {
                        System.err.println("[TekiloMod] Failed to load animation: " + animPath);
                    }
                });
            }
        );

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
    }
}
