package com.tekilo;

import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Optional;

public class RabbitClockPaintingItem extends Item {

	private final String variantName;

	public RabbitClockPaintingItem(Settings settings, String variantName) {
		super(settings);
		this.variantName = variantName;
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos blockPos = context.getBlockPos();
		Direction direction = context.getSide();
		PlayerEntity player = context.getPlayer();

		if (!direction.getAxis().isHorizontal()) {
			return ActionResult.FAIL;
		}

		BlockPos paintingPos = blockPos.offset(direction);

		Identifier variantId = Identifier.of("tekilo", variantName);

		Optional<RegistryEntry.Reference<PaintingVariant>> optionalVariant =
			world.getRegistryManager()
				.getOrThrow(RegistryKeys.PAINTING_VARIANT)
				.getEntry(variantId);

		if (optionalVariant.isEmpty()) {
			return ActionResult.FAIL;
		}

		RegistryEntry<PaintingVariant> variant = optionalVariant.get();

		PaintingEntity painting = new PaintingEntity(world, paintingPos, direction, variant);

		if (!painting.canStayAttached()) {
			return ActionResult.FAIL;
		}

		if (!world.isClient()) {
			world.spawnEntity(painting);

			if (player != null && !player.isCreative()) {
				context.getStack().decrement(1);
			}
		}

		return ActionResult.SUCCESS;
	}
}
