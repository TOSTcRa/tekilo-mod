package com.tekilo;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class FriendlyFireHandler {

    public static void register() {
        AttackEntityCallback.EVENT.register(FriendlyFireHandler::onAttackEntity);
    }

    private static ActionResult onAttackEntity(
            PlayerEntity player,
            World world,
            Hand hand,
            Entity entity,
            @Nullable EntityHitResult hitResult
    ) {
        // Проверяем только на серверной стороне
        if (world.isClient()) {
            return ActionResult.PASS;
        }

        // Проверяем, что атакуемый - тоже игрок
        if (!(entity instanceof ServerPlayerEntity targetPlayer)) {
            return ActionResult.PASS;
        }

        // Проверяем, что атакующий - серверный игрок
        if (!(player instanceof ServerPlayerEntity attackerPlayer)) {
            return ActionResult.PASS;
        }

        // Получаем фракции обоих игроков
        FactionManager.Faction attackerFaction = FactionManager.getPlayerFaction(attackerPlayer.getUuid());
        FactionManager.Faction targetFaction = FactionManager.getPlayerFaction(targetPlayer.getUuid());

        // Если оба игрока из одной фракции (и фракция не NONE), запрещаем урон
        if (attackerFaction != FactionManager.Faction.NONE &&
            attackerFaction == targetFaction) {

            attackerPlayer.sendMessage(
                Text.literal("§cВы не можете атаковать союзника!"),
                true // actionBar - сообщение над хотбаром
            );

            return ActionResult.FAIL; // Отменяем атаку
        }

        return ActionResult.PASS;
    }
}
