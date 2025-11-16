package com.tekilo;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.*;

public class ZoneCaptureManager {
    private static final Map<BlockPos, ZoneData> zones = new HashMap<>();
    private static final int ZONE_RADIUS = 250;
    private static final int BASE_CAPTURE_TIME = 12000; // 10 minutes in ticks
    private static final int MIN_CAPTURE_TIME = 6000; // 5 minutes in ticks
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

    public static class ZoneData {
        private final BlockPos center;
        private FactionManager.Faction ownerFaction = FactionManager.Faction.NONE;
        private FactionManager.Faction capturingFaction = FactionManager.Faction.NONE;
        private int captureProgress = 0;
        private int requiredCaptureTime = BASE_CAPTURE_TIME;
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
            // Обновляем список игроков в зоне
            updatePlayersInZone(world);

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
                requiredCaptureTime = calculateCaptureTime(playerCount);

                captureProgress++;

                if (captureProgress >= requiredCaptureTime) {
                    // Зона захвачена!
                    ownerFaction = dominantFaction;
                    capturingFaction = FactionManager.Faction.NONE;
                    captureProgress = 0;

                    // Уведомляем всех игроков в зоне
                    notifyCapture(world, dominantFaction);
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

            updateBossBar(world);
        }

        private void updatePlayersInZone(ServerWorld world) {
            Box box = new Box(center).expand(ZONE_RADIUS);
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
            Iterator<UUID> iterator = playersInZone.iterator();
            while (iterator.hasNext()) {
                UUID uuid = iterator.next();
                if (!currentPlayers.contains(uuid)) {
                    iterator.remove();
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

        private int calculateCaptureTime(int playerCount) {
            if (playerCount >= 5) {
                return MIN_CAPTURE_TIME;
            } else if (playerCount == 1) {
                return BASE_CAPTURE_TIME;
            } else {
                // Линейная интерполяция
                int reduction = (playerCount - 1) * (BASE_CAPTURE_TIME - MIN_CAPTURE_TIME) / 4;
                return BASE_CAPTURE_TIME - reduction;
            }
        }

        private void updateBossBar(ServerWorld world) {
            if (capturingFaction != FactionManager.Faction.NONE) {
                float progress = (float) captureProgress / requiredCaptureTime;
                bossBar.setPercent(progress);

                String factionName = capturingFaction == FactionManager.Faction.COMMUNIST ?
                    "Communists" : "Capitalists";
                int secondsLeft = (requiredCaptureTime - captureProgress) / 20;

                bossBar.setName(Text.translatable("zone.tekilo.capturing",
                    factionName, secondsLeft));

                bossBar.setColor(capturingFaction == FactionManager.Faction.COMMUNIST ?
                    BossBar.Color.RED : BossBar.Color.YELLOW);
            } else if (ownerFaction != FactionManager.Faction.NONE) {
                bossBar.setPercent(1.0f);
                String factionName = ownerFaction == FactionManager.Faction.COMMUNIST ?
                    "Communists" : "Capitalists";
                bossBar.setName(Text.translatable("zone.tekilo.owned", factionName));
                bossBar.setColor(ownerFaction == FactionManager.Faction.COMMUNIST ?
                    BossBar.Color.RED : BossBar.Color.YELLOW);
            } else {
                bossBar.setPercent(0.0f);
                bossBar.setName(Text.translatable("zone.tekilo.uncaptured"));
                bossBar.setColor(BossBar.Color.WHITE);
            }
        }

        private void notifyCapture(ServerWorld world, FactionManager.Faction faction) {
            String factionName = faction == FactionManager.Faction.COMMUNIST ?
                "Communists" : "Capitalists";

            for (UUID playerId : playersInZone) {
                ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(playerId);
                if (player != null) {
                    player.sendMessage(
                        Text.translatable("zone.tekilo.captured", factionName)
                            .formatted(faction == FactionManager.Faction.COMMUNIST ?
                                Formatting.RED : Formatting.YELLOW),
                        false
                    );
                }
            }
        }

        public void cleanup() {
            bossBar.clearPlayers();
        }
    }
}
