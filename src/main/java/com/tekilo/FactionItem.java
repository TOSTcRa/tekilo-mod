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


public class FactionItem extends Item {
    public FactionItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);

        if (!world.isClient() && user instanceof ServerPlayerEntity serverPlayer) {
            FactionManager.Faction faction;
            String translationKey;

            if (this == ModItems.PARTY_CARD) {
                faction = FactionManager.Faction.COMMUNIST;
                translationKey = "item.tekilo.party_card.use";
            } else if (this == ModItems.TAX_BILL) {
                faction = FactionManager.Faction.CAPITALIST;
                translationKey = "item.tekilo.tax_bill.use";
            } else {
                return ActionResult.PASS;
            }

            FactionManager.setPlayerFaction(user.getUuid(), faction);

            FactionManager.syncToClient(serverPlayer);

            MinecraftServer server = world.getServer();
            if (server != null) {
                // Добавляем игрока в команду
                TeamManager.addPlayerToTeam(server, serverPlayer, faction);

                FactionPersistence.save(server);
            }

            user.sendMessage(Text.translatable(translationKey), false);

            if (!user.getAbilities().creativeMode) {
                itemStack.decrement(1);
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
