package com.tekilo;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ZoneCaptureManager {
    private static final Map<BlockPos, ZoneData> zones = new ConcurrentHashMap<>();
    private static long lastTickTime = 0;

    public static void registerZone(BlockPos spawnerPos) {
        if (!zones.containsKey(spawnerPos)) {
            zones.put(spawnerPos, new ZoneData(spawnerPos));
        }
    }

    public static void unregisterZone(BlockPos spawnerPos) {
        ZoneData data = zones.remove(spawnerPos);
        if (data != null) {
            data.cleanup();
        }
    }

    public static void tick(ServerWorld world) {
        for (ZoneData zone : zones.values()) {
            zone.tick(world);
        }
    }

    // Ensures tick is called only once per game tick, even if multiple spawners exist
    public static void tickOnce(ServerWorld world) {
        long currentTime = world.getTime();
        if (currentTime != lastTickTime) {
            lastTickTime = currentTime;
            tick(world);
        }
    }

    private static BossBar.Color getBossBarColor(int colorIndex) {
        return switch (colorIndex) {
            case 1 -> BossBar.Color.RED;
            case 2 -> BossBar.Color.YELLOW;
            case 3 -> BossBar.Color.GREEN;
            case 4 -> BossBar.Color.BLUE;
            case 5 -> BossBar.Color.PURPLE;
            case 6 -> BossBar.Color.PINK;
            default -> BossBar.Color.WHITE;
        };
    }

    public static class ZoneData {
        private final BlockPos center;
        private FactionManager.Faction ownerFaction = FactionManager.Faction.NONE;
        private FactionManager.Faction capturingFaction = FactionManager.Faction.NONE;
        private int captureProgress = 0;
        private int requiredCaptureTime = 12000;
        private final ServerBossBar bossBar;
        private final Set<UUID> playersInZone = new HashSet<>();

        public ZoneData(BlockPos center) {
            this.center = center;
            this.bossBar = new ServerBossBar(
                Text.translatable("zone.tekilo.uncaptured"),
                BossBar.Color.WHITE,
                BossBar.Style.PROGRESS
            );
        }

        public void tick(ServerWorld world) {
            // Получаем настройки из BlockEntity
            BlockEntity be = world.getBlockEntity(center);
            if (!(be instanceof ItemSpawnerBlockEntity spawner)) {
                return;
            }

            // Проверяем включена ли зона
            if (!spawner.isZoneEnabled()) {
                // Скрываем bossbar если зона выключена
                bossBar.clearPlayers();
                playersInZone.clear();
                return;
            }

            int zoneRadius = spawner.getZoneRadius();
            int baseCaptureTime = spawner.getBaseCaptureTime();
            int minCaptureTime = spawner.getMinCaptureTime();
            String zoneName = spawner.getZoneName();
            int bossBarColorIndex = spawner.getBossBarColor();
            String captureReward = spawner.getCaptureReward();

            // Обновляем список игроков в зоне
            updatePlayersInZone(world, zoneRadius);

            // Считаем игроков по фракциям
            Map<FactionManager.Faction, Integer> factionCounts = countFactionPlayers(world);

            // Определяем доминирующую фракцию
            FactionManager.Faction dominantFaction = getDominantFaction(factionCounts);

            if (dominantFaction != FactionManager.Faction.NONE && dominantFaction != ownerFaction) {
                // Начинаем/продолжаем захват
                if (capturingFaction != dominantFaction) {
                    capturingFaction = dominantFaction;
                    captureProgress = 0;
                }

                // Рассчитываем время захвата на основе количества игроков
                int playerCount = factionCounts.getOrDefault(dominantFaction, 0);
                requiredCaptureTime = calculateCaptureTime(playerCount, baseCaptureTime, minCaptureTime);

                captureProgress++;

                if (captureProgress >= requiredCaptureTime) {
                    // Зона захвачена!
                    ownerFaction = dominantFaction;
                    capturingFaction = FactionManager.Faction.NONE;
                    captureProgress = 0;

                    // Уведомляем всех игроков в зоне и выдаём награды
                    notifyCapture(world, dominantFaction, zoneName, captureReward);
                }
            } else if (dominantFaction == ownerFaction || dominantFaction == FactionManager.Faction.NONE) {
                // Сбрасываем прогресс захвата
                if (captureProgress > 0) {
                    captureProgress = Math.max(0, captureProgress - 2); // Откат быстрее
                }
                if (captureProgress == 0) {
                    capturingFaction = FactionManager.Faction.NONE;
                }
            }

            updateBossBar(world, zoneName, bossBarColorIndex);
        }

        private void updatePlayersInZone(ServerWorld world, int zoneRadius) {
            Box box = new Box(center).expand(zoneRadius);
            List<ServerPlayerEntity> players = world.getPlayers(player ->
                box.contains(player.getX(), player.getY(), player.getZ())
            );

            Set<UUID> currentPlayers = new HashSet<>();
            for (ServerPlayerEntity player : players) {
                UUID uuid = player.getUuid();
                currentPlayers.add(uuid);

                if (!playersInZone.contains(uuid)) {
                    // Игрок вошел в зону
                    bossBar.addPlayer(player);
                }
            }

            // Удаляем игроков, покинувших зону
            for (UUID uuid : new HashSet<>(playersInZone)) {
                if (!currentPlayers.contains(uuid)) {
                    ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(uuid);
                    if (player != null) {
                        bossBar.removePlayer(player);
                    }
                }
            }

            playersInZone.clear();
            playersInZone.addAll(currentPlayers);
        }

        private Map<FactionManager.Faction, Integer> countFactionPlayers(ServerWorld world) {
            Map<FactionManager.Faction, Integer> counts = new HashMap<>();

            for (UUID playerId : playersInZone) {
                // Для шпионов учитываем их "видимую" фракцию
                FactionManager.Faction faction = FactionManager.getPlayerFaction(playerId);
                if (faction != FactionManager.Faction.NONE) {
                    counts.put(faction, counts.getOrDefault(faction, 0) + 1);
                }
            }

            return counts;
        }

        private FactionManager.Faction getDominantFaction(Map<FactionManager.Faction, Integer> counts) {
            FactionManager.Faction dominant = FactionManager.Faction.NONE;
            int maxCount = 0;

            for (Map.Entry<FactionManager.Faction, Integer> entry : counts.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    dominant = entry.getKey();
                } else if (entry.getValue() == maxCount && maxCount > 0) {
                    // Ничья - никто не захватывает
                    return FactionManager.Faction.NONE;
                }
            }

            return dominant;
        }

        private int calculateCaptureTime(int playerCount, int baseCaptureTime, int minCaptureTime) {
            if (playerCount >= 5) {
                return minCaptureTime;
            } else if (playerCount == 1) {
                return baseCaptureTime;
            } else {
                // Линейная интерполяция
                int reduction = (playerCount - 1) * (baseCaptureTime - minCaptureTime) / 4;
                return baseCaptureTime - reduction;
            }
        }

        private void updateBossBar(ServerWorld world, String zoneName, int bossBarColorIndex) {
            String displayName = zoneName.isEmpty() ? "Zone" : zoneName;

            if (capturingFaction != FactionManager.Faction.NONE) {
                float progress = (float) captureProgress / requiredCaptureTime;
                bossBar.setPercent(progress);

                String factionName = capturingFaction == FactionManager.Faction.COMMUNIST ?
                    "Communists" : "Capitalists";
                int secondsLeft = (requiredCaptureTime - captureProgress) / 20;

                bossBar.setName(Text.literal(displayName + " - ")
                    .append(Text.translatable("zone.tekilo.capturing", factionName, secondsLeft)));

                bossBar.setColor(capturingFaction == FactionManager.Faction.COMMUNIST ?
                    BossBar.Color.RED : BossBar.Color.YELLOW);
            } else if (ownerFaction != FactionManager.Faction.NONE) {
                bossBar.setPercent(1.0f);
                String factionName = ownerFaction == FactionManager.Faction.COMMUNIST ?
                    "Communists" : "Capitalists";
                bossBar.setName(Text.literal(displayName + " - ")
                    .append(Text.translatable("zone.tekilo.owned", factionName)));
                bossBar.setColor(ownerFaction == FactionManager.Faction.COMMUNIST ?
                    BossBar.Color.RED : BossBar.Color.YELLOW);
            } else {
                bossBar.setPercent(0.0f);
                bossBar.setName(Text.literal(displayName + " - ")
                    .append(Text.translatable("zone.tekilo.uncaptured")));
                bossBar.setColor(getBossBarColor(bossBarColorIndex));
            }
        }

        private void notifyCapture(ServerWorld world, FactionManager.Faction faction, String zoneName, String captureReward) {
            String factionName = faction == FactionManager.Faction.COMMUNIST ?
                "Communists" : "Capitalists";
            String displayName = zoneName.isEmpty() ? "Zone" : zoneName;

            for (UUID playerId : playersInZone) {
                ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);
                if (player != null) {
                    // Уведомление о захвате
                    player.sendMessage(
                        Text.literal(displayName + " ")
                            .append(Text.translatable("zone.tekilo.captured", factionName))
                            .formatted(faction == FactionManager.Faction.COMMUNIST ?
                                Formatting.RED : Formatting.YELLOW),
                        false
                    );

                    // Выдаём награду если указана и игрок из захватившей фракции
                    if (!captureReward.isEmpty() && FactionManager.getPlayerFaction(playerId) == faction) {
                        try {
                            Identifier itemId = Identifier.tryParse(captureReward);
                            if (itemId != null) {
                                Item rewardItem = Registries.ITEM.get(itemId);
                                if (rewardItem != null) {
                                    ItemStack reward = new ItemStack(rewardItem, 1);
                                    if (!player.getInventory().insertStack(reward)) {
                                        player.dropItem(reward, false);
                                    }
                                    player.sendMessage(
                                        Text.translatable("zone.tekilo.reward_received")
                                            .formatted(Formatting.GREEN),
                                        true
                                    );
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[TekiloMod] Failed to give capture reward: " + e.getMessage());
                        }
                    }
                }
            }
        }

        public void cleanup() {
            bossBar.clearPlayers();
        }

        public void removePlayer(UUID playerId) {
            playersInZone.remove(playerId);
        }
    }

    // Cleanup when player disconnects - remove from all zones
    public static void cleanupPlayer(UUID playerId) {
        for (ZoneData zone : zones.values()) {
            zone.removePlayer(playerId);
        }
    }
}
