package com.tekilo.network;

import com.tekilo.PlayerAnimatorInit;
import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.api.layered.modifier.AbstractFadeModifier;
import dev.kosmx.playerAnim.core.util.Ease;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;

/**
 * Обработчик сетевых пакетов анимаций на клиенте
 */
public class ClientAnimationHandler {
    public static void register() {
        // Обработка пакета воспроизведения анимации
        ClientPlayNetworking.registerGlobalReceiver(
            PlayAnimationPayload.ID,
            (payload, context) -> {
                context.client().execute(() -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    ClientWorld world = client.world;

                    if (world == null) {
                        return;
                    }

                    // Ищем игрока по UUID
                    AbstractClientPlayerEntity player = null;
                    for (var p : world.getPlayers()) {
                        if (p.getUuid().equals(payload.playerUuid()) && p instanceof AbstractClientPlayerEntity) {
                            player = (AbstractClientPlayerEntity) p;
                            break;
                        }
                    }

                    if (player == null) {
                        System.out.println("[TekiloMod] Player not found for animation: " + payload.playerUuid());
                        return;
                    }

                    // Получаем animation layer для игрока
                    ModifierLayer<IAnimation> animationLayer = (ModifierLayer<IAnimation>)
                        PlayerAnimationAccess.getPlayerAssociatedData(player).get(PlayerAnimatorInit.ANIMATION_LAYER_ID);

                    if (animationLayer == null) {
                        System.out.println("[TekiloMod] Animation layer not found for player: " + player.getName().getString());
                        return;
                    }

                    // Загружаем анимацию из реестра
                    Identifier animationId = Identifier.of("tekilo", payload.animationName());
                    var animationContainer = PlayerAnimationRegistry.getAnimation(animationId);

                    if (animationContainer == null) {
                        System.out.println("[TekiloMod] Animation not registered: " + animationId);
                        return;
                    }

                    // Воспроизводим анимацию с плавным переходом
                    animationLayer.replaceAnimationWithFade(
                        AbstractFadeModifier.standardFadeIn(5, Ease.LINEAR),
                        animationContainer.playAnimation()
                    );

                    System.out.println("[TekiloMod] Playing animation " + payload.animationName() + " for player " + player.getName().getString());
                });
            }
        );
    }
}
