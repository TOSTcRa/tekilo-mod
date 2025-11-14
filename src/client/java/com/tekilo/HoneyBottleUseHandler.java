package com.tekilo;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class HoneyBottleUseHandler {

    public static void register() {
        UseEntityCallback.EVENT.register(HoneyBottleUseHandler::onUseEntity);
    }

    private static ActionResult onUseEntity(
            PlayerEntity player,
            World world,
            Hand hand,
            Entity entity,
            @Nullable EntityHitResult hitResult
    ) {
        // Проверяем, что игрок держит бутылочку меда
        if (!player.getStackInHand(hand).isOf(Items.HONEY_BOTTLE)) {
            return ActionResult.PASS;
        }

        // Проверяем, что игрок является коммунистом
        if (!FactionManager.isCommunist(player.getUuid())) {
            return ActionResult.PASS;
        }

        if (world.isClient()) {
            MinecraftClient client = MinecraftClient.getInstance();
            client.execute(() -> {
                client.setScreen(new ActionSelectionScreen(entity));
            });
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}
