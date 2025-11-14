package com.tekilo;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {
    public static final Item PARTY_CARD = register("party_card", FactionItem::new, new Item.Settings().maxCount(1));

    public static final Item TAX_BILL = register("tax_bill", FactionItem::new, new Item.Settings().maxCount(1));

    private static <T extends Item> T register(String path, Function<Item.Settings, T> factory, Item.Settings settings) {
        final Identifier identifier = Identifier.of("tekilo", path);
        final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, identifier);

        settings.registryKey(registryKey);

        return  Registry.register(Registries.ITEM, registryKey, factory.apply(settings));
    }

    public static void initialize() {

    }
}