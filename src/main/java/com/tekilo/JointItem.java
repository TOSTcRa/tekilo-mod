package com.tekilo;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class JointItem extends Item {
    public JointItem(Settings settings) {
        super(settings.food(
            new net.minecraft.component.type.FoodComponent.Builder()
                .nutrition(1)
                .saturationModifier(0.1f)
                .alwaysEdible()
                .build()
        ));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (user instanceof PlayerEntity player) {
            player.incrementStat(Stats.USED.getOrCreateStat(this));

            if (!world.isClient()) {
                // Apply Deliciousness effect (30 seconds = 600 ticks)
                user.addStatusEffect(new StatusEffectInstance(
                    ModStatusEffects.DELICIOUSNESS,
                    600, // 30 seconds
                    0, // Amplifier 0
                    false, // Not ambient
                    true, // Show particles
                    true // Show icon
                ));

                // Apply Nausea effect (30 seconds)
                user.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.NAUSEA,
                    600,
                    0,
                    false,
                    true,
                    true
                ));

                if (player instanceof ServerPlayerEntity serverPlayer) {
                    Criteria.CONSUME_ITEM.trigger(serverPlayer, stack);
                }
            }

            if (!player.isInCreativeMode()) {
                stack.decrement(1);
            }
        }

        return stack;
    }
}
