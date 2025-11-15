package com.tekilo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class SpyItem extends Item {
	public SpyItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult use(World world, PlayerEntity user, Hand hand) {
		ItemStack itemStack = user.getStackInHand(hand);

		if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
			FactionManager.Faction faction;
			String messageKey;

			if (this == ModItems.FAKE_PARTY_CARD) {
				faction = FactionManager.Faction.COMMUNIST;
				messageKey = "item.tekilo.fake_party_card.use";
			} else if (this == ModItems.FAKE_TAX_BILL) {
				faction = FactionManager.Faction.CAPITALIST;
				messageKey = "item.tekilo.fake_tax_bill.use";
			} else {
				return ActionResult.PASS;
			}

			FactionManager.setPlayerAsSpy(user.getUuid(), faction);
			FactionManager.syncToClient(serverPlayer);

			MinecraftServer server = world.getServer();
			if (server != null) {
				TeamManager.addPlayerToTeam(server, serverPlayer, faction, true);
				FactionPersistence.save(server);
			}

			user.sendMessage(Text.translatable(messageKey), false);

			if (!user.getAbilities().creativeMode) {
				itemStack.decrement(1);
			}

			return ActionResult.SUCCESS;
		}

		return ActionResult.PASS;
	}
}
