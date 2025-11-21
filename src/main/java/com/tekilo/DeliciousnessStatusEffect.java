package com.tekilo;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;

public class DeliciousnessStatusEffect extends StatusEffect {
    public DeliciousnessStatusEffect(StatusEffectCategory category, int color) {
        super(category, color);
    }

    // Effects are handled on client side for visual rendering
    // No server-side logic needed
}
