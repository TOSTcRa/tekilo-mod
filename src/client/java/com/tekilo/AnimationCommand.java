package com.tekilo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.tekilo.animation.AnimationData;
import com.tekilo.animation.AnimationLoader;
import com.tekilo.animation.AnimationManager;
import com.tekilo.network.PlayAnimationPayload;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Клиентская команда для тестирования анимаций
 */
public class AnimationCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(
            ClientCommandManager.literal("animate")
                .then(ClientCommandManager.argument("animation", StringArgumentType.string())
                    .executes(AnimationCommand::playAnimation))
                .executes(AnimationCommand::showUsage)
        );
    }

    private static int showUsage(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();
        if (client.player != null) {
            client.player.sendMessage(Text.literal("§eИспользование: /animate <kneel|bow|walk>"), false);
        }
        return 0;
    }

    private static int playAnimation(CommandContext<FabricClientCommandSource> context) {
        MinecraftClient client = context.getSource().getClient();

        if (client.player == null) {
            return 0;
        }

        try {
            String animName = StringArgumentType.getString(context, "animation");

            // Определяем путь к файлу анимации
            String animPath = switch (animName.toLowerCase()) {
                case "kneel" -> "animations/kneel.animation.json";
                case "bow" -> "animations/bow.animation.json";
                case "walk" -> "animations/walk.animation.json";
                default -> {
                    client.player.sendMessage(Text.literal("§cНеизвестная анимация: " + animName), false);
                    client.player.sendMessage(Text.literal("§eДоступные: kneel, bow, walk"), false);
                    yield null;
                }
            };

            if (animPath == null) {
                return 0;
            }

            // Загружаем анимацию
            Identifier animId = Identifier.of("tekilo", animPath);
            AnimationData animation = AnimationLoader.loadAnimation(
                client.getResourceManager(),
                animId
            );

            if (animation == null) {
                client.player.sendMessage(Text.literal("§cНе удалось загрузить анимацию: " + animName), false);
                System.err.println("[TekiloMod] Failed to load animation: " + animId);
                return 0;
            }

            System.out.println("[TekiloMod] Animation loaded successfully: " + animation.getName());
            System.out.println("[TekiloMod] Animation length: " + animation.getAnimationLength() + " seconds");

            // Запускаем анимацию для локального игрока
            if (client.player instanceof AbstractClientPlayerEntity player) {
                AnimationManager.playAnimation(player, animation, false);
                client.player.sendMessage(Text.literal("§aАнимация '" + animName + "' запущена! Длина: " + animation.getAnimationLength() + "с"), false);
                System.out.println("[TekiloMod] Playing animation '" + animName + "' for player: " + player.getName().getString());

                // Отправляем пакет на сервер для синхронизации с другими игроками
                if (ClientPlayNetworking.canSend(PlayAnimationPayload.ID)) {
                    PlayAnimationPayload payload = new PlayAnimationPayload(
                        player.getUuid(),
                        animName,
                        false
                    );
                    ClientPlayNetworking.send(payload);
                    System.out.println("[TekiloMod] Sent animation packet to server");
                }
            }

            return 1;
        } catch (Exception e) {
            client.player.sendMessage(Text.literal("§cОшибка: " + e.getMessage()), false);
            e.printStackTrace();
            return 0;
        }
    }
}
