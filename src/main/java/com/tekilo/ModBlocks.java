package com.tekilo;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModBlocks {
    public static final String MOD_ID = "tekilo";

    // Сжатая марихуана
    public static final Block COMPRESSED_WEED = register(
        "compressed_weed",
        Block::new,
        AbstractBlock.Settings.create()
            .strength(1.5f, 6.0f)
            .sounds(BlockSoundGroup.GRASS)
            .requiresTool(),
        true
    );

    public static final Block COMPRESSED_WEED_STAIRS = register(
        "compressed_weed_stairs",
        settings -> new StairsBlock(COMPRESSED_WEED.getDefaultState(), settings),
        AbstractBlock.Settings.copy(COMPRESSED_WEED),
        true
    );

    public static final Block COMPRESSED_WEED_SLAB = register(
        "compressed_weed_slab",
        SlabBlock::new,
        AbstractBlock.Settings.copy(COMPRESSED_WEED),
        true
    );

    public static final Block COMPRESSED_WEED_WALL = register(
        "compressed_weed_wall",
        WallBlock::new,
        AbstractBlock.Settings.copy(COMPRESSED_WEED),
        true
    );

    public static final Block STALIN_STATUE = register(
        "stalin_statue",
        StalinStatueBlock::new,
        AbstractBlock.Settings.create()
            .strength(2.0f, 6.0f)
            .sounds(BlockSoundGroup.STONE)
            .nonOpaque(),
        true
    );

    public static final Block ITEM_SPAWNER = register(
        "item_spawner",
        ItemSpawnerBlock::new,
        AbstractBlock.Settings.create()
            .strength(-1.0f, 3600000.0f) // Неразрушимый как бедрок
            .sounds(BlockSoundGroup.METAL)
            .dropsNothing(),
        true
    );

    public static final Block CANVAS = register(
        "canvas",
        CanvasBlock::new,
        AbstractBlock.Settings.create()
            .strength(0.5f)
            .sounds(BlockSoundGroup.WOOL)
            .nonOpaque(),
        true
    );

    public static final Block COMMUNIST_COLLECTOR = register(
        "communist_collector",
        CommunistCollectorBlock::new,
        AbstractBlock.Settings.create()
            .strength(2.5f)
            .sounds(BlockSoundGroup.WOOD)
            .requiresTool(),
        true
    );

    public static final Block CAPITALIST_COLLECTOR = register(
        "capitalist_collector",
        CapitalistCollectorBlock::new,
        AbstractBlock.Settings.create()
            .strength(2.5f)
            .sounds(BlockSoundGroup.WOOD)
            .requiresTool(),
        true
    );

    private static Block register(String name, Function<AbstractBlock.Settings, Block> blockFactory,
                                  AbstractBlock.Settings settings, boolean shouldRegisterItem) {
        RegistryKey<Block> blockKey = keyOfBlock(name);
        Block block = blockFactory.apply(settings.registryKey(blockKey));

        if (shouldRegisterItem) {
            RegistryKey<Item> itemKey = keyOfItem(name);
            BlockItem blockItem = new BlockItem(block, new Item.Settings()
                .registryKey(itemKey).useBlockPrefixedTranslationKey());
            Registry.register(Registries.ITEM, itemKey, blockItem);
        }

        return Registry.register(Registries.BLOCK, blockKey, block);
    }

    private static RegistryKey<Block> keyOfBlock(String name) {
        return RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, name));
    }

    private static RegistryKey<Item> keyOfItem(String name) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, name));
    }

    public static void initialize() {
        // Blocks are now registered in ModItemGroups
        System.out.println("[TekiloMod] Blocks initialized!");
    }
}
