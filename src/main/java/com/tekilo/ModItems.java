package com.tekilo;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.function.Function;

public class ModItems {
	public static final Item PARTY_CARD = register("party_card", FactionItem::new, new
		Item.Settings().maxCount(1));

	public static final Item TAX_BILL = register("tax_bill", FactionItem::new, new
		Item.Settings().maxCount(1));

	public static final Item FAKE_PARTY_CARD = register("fake_party_card", SpyItem::new, new
		Item.Settings().maxCount(1));

	public static final Item FAKE_TAX_BILL = register("fake_tax_bill", SpyItem::new, new
		Item.Settings().maxCount(1));

	public static final Item RABBIT_CLOCK_PAINTING = register(
		"rabbit_clock_painting",
		settings -> new RabbitClockPaintingItem(settings, "rabbit_with_clock"),
		new Item.Settings().maxCount(16)
	);

	public static final Item RABBIT_CLOCK_PAINTING_2 = register(
		"rabbit_clock_painting_2",
		settings -> new RabbitClockPaintingItem(settings, "rabbit_with_clock_2"),
		new Item.Settings().maxCount(16)
	);

	public static final Item RABBIT_CLOCK_PAINTING_3 = register(
		"rabbit_clock_painting_3",
		settings -> new RabbitClockPaintingItem(settings, "rabbit_with_clock_3"),
		new Item.Settings().maxCount(16)
	);

	public static final Item MUSIC_DISC_SOUND_1 = register(
		"music_disc_sound_1",
		settings -> new Item(settings
			.maxCount(1)
			.jukeboxPlayable(RegistryKey.of(RegistryKeys.JUKEBOX_SONG, Identifier.of("tekilo",
				"sound_1")))
			.rarity(Rarity.RARE)
		),
		new Item.Settings()
	);

	public static final Item MUSIC_DISC_SOUND_2 = register(
		"music_disc_sound_2",
		settings -> new Item(settings
			.maxCount(1)
			.jukeboxPlayable(RegistryKey.of(RegistryKeys.JUKEBOX_SONG, Identifier.of("tekilo",
				"sound_2")))
			.rarity(Rarity.RARE)
		),
		new Item.Settings()
	);

	public static final Item DOLLAR = register(
		"dollar",
		Item::new,
		new Item.Settings().maxCount(64)
	);

	public static final Item WEED_CLUMP = register(
		"weed_clump",
		Item::new,
		new Item.Settings().maxCount(64)
	);

	public static final Item JOINT = register(
		"joint",
		JointItem::new,
		new Item.Settings().maxCount(16).rarity(Rarity.UNCOMMON)
	);

	public static final Item SPAWNER_LINKER = register(
		"spawner_linker",
		SpawnerLinkerItem::new,
		new Item.Settings().maxCount(1).rarity(Rarity.RARE)
	);

	public static final Item CANVAS_PAINTING = register(
		"canvas_painting",
		CanvasPaintingItem::new,
		new Item.Settings().maxCount(16)
	);

	private static <T extends Item> T register(String path, Function<Item.Settings, T> factory,
											   Item.Settings settings) {
		final Identifier identifier = Identifier.of("tekilo", path);
		final RegistryKey<Item> registryKey = RegistryKey.of(RegistryKeys.ITEM, identifier);

		settings.registryKey(registryKey);

		return Registry.register(Registries.ITEM, registryKey, factory.apply(settings));
	}

	public static void initialize() {
		// Items are now registered in ModItemGroups
	}
}
