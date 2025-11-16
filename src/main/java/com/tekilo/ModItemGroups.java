package com.tekilo;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final String MOD_ID = "tekilo";

    public static final RegistryKey<ItemGroup> TEKILO_GROUP_KEY = RegistryKey.of(
        Registries.ITEM_GROUP.getKey(),
        Identifier.of(MOD_ID, "tekilo_items")
    );

    public static final ItemGroup TEKILO_GROUP = FabricItemGroup.builder()
        .icon(() -> new ItemStack(ModItems.PARTY_CARD))
        .displayName(Text.translatable("itemGroup.tekilo.tekilo_items"))
        .build();

    public static void initialize() {
        // Register the item group
        Registry.register(Registries.ITEM_GROUP, TEKILO_GROUP_KEY, TEKILO_GROUP);

        // Add all items to the group
        ItemGroupEvents.modifyEntriesEvent(TEKILO_GROUP_KEY).register(entries -> {
            // Items
            entries.add(ModItems.PARTY_CARD);
            entries.add(ModItems.TAX_BILL);
            entries.add(ModItems.FAKE_PARTY_CARD);
            entries.add(ModItems.FAKE_TAX_BILL);
            entries.add(ModItems.RABBIT_CLOCK_PAINTING);
            entries.add(ModItems.RABBIT_CLOCK_PAINTING_2);
            entries.add(ModItems.RABBIT_CLOCK_PAINTING_3);
            entries.add(ModItems.MUSIC_DISC_SOUND_1);
            entries.add(ModItems.MUSIC_DISC_SOUND_2);
            entries.add(ModItems.DOLLAR);
            entries.add(ModItems.WEED_CLUMP);
            entries.add(ModItems.CANVAS_PAINTING);

            // Blocks
            entries.add(ModBlocks.COMPRESSED_WEED);
            entries.add(ModBlocks.COMPRESSED_WEED_STAIRS);
            entries.add(ModBlocks.COMPRESSED_WEED_SLAB);
            entries.add(ModBlocks.COMPRESSED_WEED_WALL);
            entries.add(ModBlocks.STALIN_STATUE);
            entries.add(ModBlocks.ITEM_SPAWNER);
            entries.add(ModBlocks.CANVAS);
        });

        System.out.println("[TekiloMod] Item groups initialized!");
    }
}
