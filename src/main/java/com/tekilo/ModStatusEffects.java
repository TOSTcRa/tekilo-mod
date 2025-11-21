package com.tekilo;

import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class ModStatusEffects {
    public static final RegistryEntry<StatusEffect> DELICIOUSNESS = register(
        "deliciousness",
        new DeliciousnessStatusEffect(StatusEffectCategory.NEUTRAL, 0xFF00FF) // Purple/Pink color
    );

    private static RegistryEntry<StatusEffect> register(String id, StatusEffect effect) {
        return Registry.registerReference(Registries.STATUS_EFFECT, Identifier.of("tekilo", id), effect);
    }

    public static void initialize() {
        // Static initialization
    }
}
