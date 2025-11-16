package com.tekilo;

import com.tekilo.screen.ItemSpawnerScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class ModScreenHandlers {
    public static final ScreenHandlerType<ItemSpawnerScreenHandler> ITEM_SPAWNER_SCREEN_HANDLER =
        Registry.register(
            Registries.SCREEN_HANDLER,
            Identifier.of("tekilo", "item_spawner"),
            new ExtendedScreenHandlerType<>(
                (syncId, playerInventory, data) -> new ItemSpawnerScreenHandler(syncId, playerInventory, data),
                BlockPos.PACKET_CODEC
            )
        );

    public static void initialize() {
        System.out.println("[TekiloMod] Screen handlers initialized!");
    }
}
