package com.tekilo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class FactionCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(FactionCommand::registerCommand);
    }

    private static void registerCommand(
            CommandDispatcher<ServerCommandSource> dispatcher,
            CommandRegistryAccess registryAccess,
            CommandManager.RegistrationEnvironment environment
    ) {
        dispatcher.register(CommandManager.literal("faction")
                .then(CommandManager.argument("type", StringArgumentType.word())
                        .executes(context -> {
                            String factionType = StringArgumentType.getString(context, "type");
                            ServerCommandSource source = context.getSource();

                            if (source.getPlayer() == null) {
                                return 0;
                            }

                            FactionManager.Faction faction;
                            switch (factionType.toLowerCase()) {
                                case "communist":
                                case "коммунист":
                                    faction = FactionManager.Faction.COMMUNIST;
                                    FactionManager.setPlayerFaction(source.getPlayer().getUuid(), faction);
                                    FactionManager.syncToClient(source.getPlayer());
                                    source.sendFeedback(() -> Text.literal("Вы теперь коммунист!"), false);
                                    break;
                                case "capitalist":
                                case "капиталист":
                                    faction = FactionManager.Faction.CAPITALIST;
                                    FactionManager.setPlayerFaction(source.getPlayer().getUuid(), faction);
                                    FactionManager.syncToClient(source.getPlayer());
                                    source.sendFeedback(() -> Text.literal("Вы теперь капиталист!"), false);
                                    break;
                                case "none":
                                case "нет":
                                    faction = FactionManager.Faction.NONE;
                                    FactionManager.setPlayerFaction(source.getPlayer().getUuid(), faction);
                                    FactionManager.syncToClient(source.getPlayer());
                                    source.sendFeedback(() -> Text.literal("Фракция сброшена"), false);
                                    break;
                                default:
                                    source.sendError(Text.literal("Неизвестная фракция! Используй: communist, capitalist или none"));
                                    return 0;
                            }

                            return 1;
                        })
                )
        );
    }
}
