package com.tekilo;

import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.List;

/**
 * Manager for finding and depositing items into faction collector chests
 */
public class FactionCollectorManager {

    /**
     * Tries to deposit items into faction collector chests
     * @return true if items were deposited, false otherwise
     */
    public static boolean depositItems(ServerWorld world, FactionManager.Faction faction, ItemStack stack, int count) {
        if (faction == FactionManager.Faction.NONE) {
            return false;
        }

        // Search entire world for collector chests
        // In production, this should be optimized with a registry
        List<Inventory> collectors = findCollectorChests(world, faction);

        if (collectors.isEmpty()) {
            return false;
        }

        // Try to deposit items
        int remaining = count;
        ItemStack depositStack = stack.copy();

        for (Inventory collector : collectors) {
            if (remaining <= 0) {
                break;
            }

            // Try to add to this collector
            for (int slot = 0; slot < collector.size(); slot++) {
                if (remaining <= 0) {
                    break;
                }

                ItemStack slotStack = collector.getStack(slot);

                if (slotStack.isEmpty()) {
                    // Empty slot - fill it
                    int toDeposit = Math.min(remaining, depositStack.getMaxCount());
                    ItemStack newStack = depositStack.copy();
                    newStack.setCount(toDeposit);
                    collector.setStack(slot, newStack);
                    remaining -= toDeposit;
                } else if (ItemStack.areItemsAndComponentsEqual(slotStack, depositStack)) {
                    // Matching item - try to stack
                    int canAdd = Math.min(remaining, slotStack.getMaxCount() - slotStack.getCount());
                    if (canAdd > 0) {
                        slotStack.increment(canAdd);
                        remaining -= canAdd;
                    }
                }
            }

            collector.markDirty();
        }

        return remaining < count; // Return true if any items were deposited
    }

    private static List<Inventory> findCollectorChests(ServerWorld world, FactionManager.Faction faction) {
        List<Inventory> collectors = new java.util.ArrayList<>();

        // Search all loaded chunks for collector block entities
        // Iterate through players to get loaded chunks around them
        var players = world.getPlayers();
        java.util.Set<net.minecraft.util.math.ChunkPos> searchedChunks = new java.util.HashSet<>();

        for (var player : players) {
            // Get player's chunk position
            int playerChunkX = player.getChunkPos().x;
            int playerChunkZ = player.getChunkPos().z;

            // Search in a radius around each player (view distance)
            int searchRadius = 8; // Reasonable search radius (8 chunks = 128 blocks)
            for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    int chunkX = playerChunkX + dx;
                    int chunkZ = playerChunkZ + dz;
                    var chunkPos = new net.minecraft.util.math.ChunkPos(chunkX, chunkZ);

                    // Skip if already searched this chunk
                    if (!searchedChunks.add(chunkPos)) {
                        continue;
                    }

                    try {
                        if (world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) {
                            var chunk = world.getChunk(chunkX, chunkZ);
                            if (chunk == null) {
                                continue;
                            }

                            var blockEntities = chunk.getBlockEntities();
                            if (blockEntities == null) {
                                continue;
                            }

                            for (var blockEntity : blockEntities.values()) {
                                if (blockEntity instanceof FactionCollectorBlockEntity collector) {
                                    if (collector.getFaction() == faction) {
                                        collectors.add(collector);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Ignore chunk loading errors and continue
                    }
                }
            }
        }

        return collectors;
    }
}
