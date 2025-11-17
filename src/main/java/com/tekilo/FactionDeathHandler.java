package com.tekilo;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FactionDeathHandler {

    private static final Map<UUID, CustomDeathMessageState> playersWithCustomDeath = new ConcurrentHashMap<>();

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(FactionDeathHandler::afterDeath);
        ServerMessageEvents.ALLOW_GAME_MESSAGE.register(FactionDeathHandler::onGameMessage);
    }

    private static void afterDeath(LivingEntity entity, DamageSource damageSource) {
        if (!(entity instanceof ServerPlayerEntity player)) {
            return;
        }

        System.out.println("[FactionDeathHandler] Player died: " + player.getName().getString());

        FactionManager.Faction faction = FactionManager.getPlayerFaction(player.getUuid());
        System.out.println("[FactionDeathHandler] Faction: " + faction);

        if (faction == FactionManager.Faction.NONE) {
            System.out.println("[FactionDeathHandler] No faction, skipping");
            return;
        }

        RegistryKey<DamageType> customDeathType;

        // Определяем контекст смерти
        if (damageSource.isOf(DamageTypes.STARVE)) {
            // Смерть от голода
            customDeathType = FactionDeathMessages.getStarvationDeath(faction);
        } else if (damageSource.getAttacker() instanceof ServerPlayerEntity attacker) {
            // Смерть от игрока
            FactionManager.Faction attackerFaction = FactionManager.getPlayerFaction(attacker.getUuid());
            if (attackerFaction == faction && attackerFaction != FactionManager.Faction.NONE) {
                // Убит своим
                customDeathType = FactionDeathMessages.getFriendlyFireDeath(faction);
            } else if (attackerFaction != FactionManager.Faction.NONE && faction != FactionManager.Faction.NONE) {
                // Убит врагом
                customDeathType = FactionDeathMessages.getEnemyKillDeath(faction);
            } else if (faction == FactionManager.Faction.CAPITALIST) {
                customDeathType = FactionDeathMessages.getRandomCapitalistDeath();
            } else {
                customDeathType = FactionDeathMessages.getRandomCommunistDeath();
            }
        } else if (faction == FactionManager.Faction.CAPITALIST) {
            customDeathType = FactionDeathMessages.getRandomCapitalistDeath();
        } else {
            customDeathType = FactionDeathMessages.getRandomCommunistDeath();
        }

        System.out.println("[FactionDeathHandler] Custom death type: " + customDeathType.getValue());

        Registry<DamageType> damageTypeRegistry = player.getEntityWorld().getServer()
                .getRegistryManager()
                .getOrThrow(RegistryKeys.DAMAGE_TYPE);

        DamageType damageType = damageTypeRegistry.get(customDeathType.getValue());
        System.out.println("[FactionDeathHandler] Found damage type: " + (damageType != null));

        if (damageType != null) {
            RegistryEntry<DamageType> damageTypeEntry = damageTypeRegistry.getEntry(damageType);
            DamageSource customDamageSource = new DamageSource(damageTypeEntry);

            // Отправляем кастомное сообщение о смерти всем игрокам
            Text deathMessage = customDamageSource.getDeathMessage(player);
            System.out.println("[FactionDeathHandler] Death message: " + deathMessage.getString());
            playersWithCustomDeath.put(
                    player.getUuid(),
                    new CustomDeathMessageState(player.getName().getString(), deathMessage.getString())
            );
            player.getEntityWorld().getServer().getPlayerManager().broadcast(deathMessage, false);
        }
    }

    private static boolean onGameMessage(net.minecraft.server.MinecraftServer server, Text message, boolean overlay) {
        // Пытаемся найти сообщения о смерти игроков с кастомной смертью и блокируем их
        String messageText = message.getString();

        System.out.println("[FactionDeathHandler] Game message: " + messageText);

        Iterator<Map.Entry<UUID, CustomDeathMessageState>> iterator = playersWithCustomDeath.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, CustomDeathMessageState> entry = iterator.next();
            CustomDeathMessageState state = entry.getValue();

            if (!messageText.contains(state.playerName())) {
                continue;
            }

            if (!state.customMessageDelivered()) {
                if (messageText.equals(state.customMessage())) {
                    state.markCustomDelivered();
                }
                // Разрешаем кастомное сообщение
                return true;
            }

            System.out.println("[FactionDeathHandler] Blocking standard death message");
            iterator.remove();
            return false; // Блокируем стандартное сообщение о смерти
        }

        return true; // Разрешаем все остальные сообщения
    }

    private static final class CustomDeathMessageState {
        private final String playerName;
        private final String customMessage;
        private boolean customMessageDelivered;

        private CustomDeathMessageState(String playerName, String customMessage) {
            this.playerName = playerName;
            this.customMessage = customMessage;
        }

        private String playerName() {
            return playerName;
        }

        private String customMessage() {
            return customMessage;
        }

        private boolean customMessageDelivered() {
            return customMessageDelivered;
        }

        private void markCustomDelivered() {
            this.customMessageDelivered = true;
        }
    }

    // Cleanup method for player disconnect
    public static void cleanupPlayer(UUID playerId) {
        playersWithCustomDeath.remove(playerId);
    }
}
