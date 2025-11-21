package com.tekilo;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {
    public static final BlockEntityType<ItemSpawnerBlockEntity> ITEM_SPAWNER = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        Identifier.of("tekilo", "item_spawner"),
        FabricBlockEntityTypeBuilder.create(ItemSpawnerBlockEntity::new, ModBlocks.ITEM_SPAWNER).build()
    );

    public static final BlockEntityType<CanvasBlockEntity> CANVAS = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        Identifier.of("tekilo", "canvas"),
        FabricBlockEntityTypeBuilder.create(CanvasBlockEntity::new, ModBlocks.CANVAS).build()
    );

    public static final BlockEntityType<CommunistCollectorBlockEntity> COMMUNIST_COLLECTOR = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        Identifier.of("tekilo", "communist_collector"),
        FabricBlockEntityTypeBuilder.create(CommunistCollectorBlockEntity::new, ModBlocks.COMMUNIST_COLLECTOR).build()
    );

    public static final BlockEntityType<CapitalistCollectorBlockEntity> CAPITALIST_COLLECTOR = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        Identifier.of("tekilo", "capitalist_collector"),
        FabricBlockEntityTypeBuilder.create(CapitalistCollectorBlockEntity::new, ModBlocks.CAPITALIST_COLLECTOR).build()
    );

    public static void initialize() {
        System.out.println("[TekiloMod] Block entities initialized!");
    }
}
