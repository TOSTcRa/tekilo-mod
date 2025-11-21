package com.tekilo;

import com.tekilo.network.ItemSpawnerOpenData;
import com.tekilo.network.SpawnerLinkOpenData;
import com.tekilo.screen.ItemSpawnerScreenHandler;
import com.tekilo.screen.SpawnerLinkScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {
    public static final ScreenHandlerType<ItemSpawnerScreenHandler> ITEM_SPAWNER_SCREEN_HANDLER =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("tekilo", "item_spawner"),
            new ExtendedScreenHandlerType<>(
                (syncId, playerInventory, data) -> new ItemSpawnerScreenHandler(syncId, playerInventory, data),
                ItemSpawnerOpenData.CODEC
            )
        );

    public static final ScreenHandlerType<FactionCollectorScreenHandler> FACTION_COLLECTOR_SCREEN_HANDLER =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("tekilo", "faction_collector"),
            new ScreenHandlerType<>(FactionCollectorScreenHandler::new, null)
        );

    public static final ScreenHandlerType<SpawnerLinkScreenHandler> SPAWNER_LINK_SCREEN_HANDLER =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("tekilo", "spawner_link"),
            new ExtendedScreenHandlerType<>(
                (syncId, playerInventory, data) -> new SpawnerLinkScreenHandler(syncId, playerInventory, data),
                SpawnerLinkOpenData.CODEC
            )
        );

    public static void initialize() {
        System.out.println("[TekiloMod] Screen handlers initialized!");
    }
}
