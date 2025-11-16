package com.tekilo;

import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.Random;

public class FactionDeathMessages {
    private static final Random RANDOM = new Random();

    public static final RegistryKey<DamageType> SOLD_OUT = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "sold_out")
    );

    public static final RegistryKey<DamageType> SOLD_SOUL = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "sold_soul")
    );

    public static final RegistryKey<DamageType> GAVE_TO_PARTY = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "gave_to_party")
    );

    public static final RegistryKey<DamageType> KISSED_VOVCHIK = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "kissed_vovchik")
    );

    public static final RegistryKey<DamageType> KISSED_LENIN = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "kissed_lenin")
    );

    public static final RegistryKey<DamageType> KISSED_STALIN = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "kissed_stalin")
    );

    public static final RegistryKey<DamageType> WENT_TO_SVO = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "went_to_svo")
    );

    // Новые контекстные смерти
    public static final RegistryKey<DamageType> HOLODOMOR = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "holodomor")
    );

    public static final RegistryKey<DamageType> BANKRUPT = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "bankrupt")
    );

    public static final RegistryKey<DamageType> REPRESSED = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "repressed")
    );

    public static final RegistryKey<DamageType> FIRED = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "fired")
    );

    public static final RegistryKey<DamageType> IMPERIALISM_VICTIM = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "imperialism_victim")
    );

    public static final RegistryKey<DamageType> REVOLUTION_VICTIM = RegistryKey.of(
            RegistryKeys.DAMAGE_TYPE,
            Identifier.of("tekilo", "revolution_victim")
    );

    public static final RegistryKey<DamageType>[] CAPITALIST_DEATHS = new RegistryKey[]{
            SOLD_OUT,
            SOLD_SOUL
    };

    public static final RegistryKey<DamageType>[] COMMUNIST_DEATHS = new RegistryKey[]{
            GAVE_TO_PARTY,
            KISSED_STALIN,
            KISSED_LENIN,
            KISSED_VOVCHIK,
            WENT_TO_SVO,
    };

    public static RegistryKey<DamageType> getRandomCapitalistDeath() {
        return CAPITALIST_DEATHS[RANDOM.nextInt(CAPITALIST_DEATHS.length)];
    }

    public static RegistryKey<DamageType> getRandomCommunistDeath() {
        return COMMUNIST_DEATHS[RANDOM.nextInt(COMMUNIST_DEATHS.length)];
    }

    public static RegistryKey<DamageType> getStarvationDeath(FactionManager.Faction faction) {
        if (faction == FactionManager.Faction.COMMUNIST) {
            return HOLODOMOR;
        } else {
            return BANKRUPT;
        }
    }

    public static RegistryKey<DamageType> getFriendlyFireDeath(FactionManager.Faction faction) {
        if (faction == FactionManager.Faction.COMMUNIST) {
            return REPRESSED;
        } else {
            return FIRED;
        }
    }

    public static RegistryKey<DamageType> getEnemyKillDeath(FactionManager.Faction victimFaction) {
        if (victimFaction == FactionManager.Faction.COMMUNIST) {
            return IMPERIALISM_VICTIM;
        } else {
            return REVOLUTION_VICTIM;
        }
    }
}