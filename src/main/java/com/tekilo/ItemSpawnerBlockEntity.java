package com.tekilo;

import com.tekilo.network.ItemSpawnerOpenData;
import com.tekilo.screen.ItemSpawnerScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ItemSpawnerBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<ItemSpawnerOpenData>, Inventory {
    private DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
    private int radius = 50;
    private int spawnInterval = 6000; // ticks (5 minutes)
    private int itemCount = 1;
    private boolean spawnInChests = false;
    private boolean useGlobalSettings = true;
    private boolean enabled = true; // ON/OFF toggle
    private int tickCounter = 0;
    private static final Random RANDOM = new Random(); // Reuse random instance

    // Zone capture settings
    private int zoneRadius = 250;
    private int baseCaptureTime = 12000; // 10 minutes in ticks
    private int minCaptureTime = 6000; // 5 minutes in ticks
    private boolean zoneEnabled = true;
    private String zoneName = "";
    private int bossBarColor = 0; // 0=WHITE, 1=RED, 2=YELLOW, 3=GREEN, 4=BLUE, 5=PURPLE, 6=PINK
    private String captureReward = ""; // Item ID for reward, empty = no reward

    // Spawner dependency graph
    private List<BlockPos> parentSpawners = new ArrayList<>();
    private int unlockDelay = 300; // Delay in seconds (5 minutes default)
    private long unlockTime = -1; // World time when spawner will be unlocked
    private FactionManager.Faction lastRequiredFaction = FactionManager.Faction.NONE; // Track faction to detect ownership changes

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> radius;
                case 1 -> spawnInterval;
                case 2 -> itemCount;
                case 3 -> spawnInChests ? 1 : 0;
                case 4 -> useGlobalSettings ? 1 : 0;
                case 5 -> enabled ? 1 : 0;
                // Zone settings
                case 6 -> zoneRadius;
                case 7 -> baseCaptureTime;
                case 8 -> minCaptureTime;
                case 9 -> zoneEnabled ? 1 : 0;
                case 10 -> bossBarColor;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> radius = value;
                case 1 -> spawnInterval = value;
                case 2 -> itemCount = value;
                case 3 -> spawnInChests = value == 1;
                case 4 -> useGlobalSettings = value == 1;
                case 5 -> enabled = value == 1;
                // Zone settings
                case 6 -> zoneRadius = value;
                case 7 -> baseCaptureTime = value;
                case 8 -> minCaptureTime = value;
                case 9 -> zoneEnabled = value == 1;
                case 10 -> bossBarColor = value;
            }
            markDirty();
        }

        @Override
        public int size() {
            return 11;
        }
    };

    public ItemSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_SPAWNER, pos, state);
        // Регистрируем зону захвата
        ZoneCaptureManager.registerZone(pos);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ItemSpawnerBlockEntity blockEntity) {
        if (world.isClient()) return;

        // Check if spawner is unlocked based on parent dependencies
        if (!blockEntity.isUnlocked(world)) {
            blockEntity.enabled = false; // Force disable if locked
            blockEntity.tickCounter = 0; // Reset counter
            ZoneCaptureManager.tickOnce((ServerWorld) world);
            return;
        }

        // Всегда инкрементируем счётчик для zone capture
        blockEntity.tickCounter++;

        // Only spawn items if enabled
        if (blockEntity.enabled) {
            if (blockEntity.tickCounter >= blockEntity.spawnInterval) {
                blockEntity.tickCounter = 0;
                blockEntity.spawnItems((ServerWorld) world);
            }
        }

        // Обновляем систему захвата зон каждый тик для всех игроков
        // tickOnce гарантирует что вызывается только раз за игровой тик
        ZoneCaptureManager.tickOnce((ServerWorld) world);
    }

    private void spawnItems(ServerWorld world) {
        // Check if zone is captured and try to deposit in faction collectors
        FactionManager.Faction zoneOwner = ZoneCaptureManager.getZoneOwner(pos);

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            int count = useGlobalSettings ? itemCount : stack.getCount();

            // Try to deposit in faction collectors first if zone is captured
            if (zoneOwner != FactionManager.Faction.NONE) {
                boolean deposited = FactionCollectorManager.depositItems(world, zoneOwner, stack, count);
                if (deposited) {
                    continue; // Items deposited, skip normal spawn
                }
            }

            // Fall back to normal spawn if collectors not available or full
            if (spawnInChests) {
                // Спавн в сундуках в радиусе
                spawnInNearbyChests(world, stack, count);
            } else {
                // Спавн на земле в радиусе
                for (int j = 0; j < count; j++) {
                    double x = pos.getX() + RANDOM.nextDouble() * radius * 2 - radius;
                    // Limit Y range to reasonable values (current Y +/- 10 blocks)
                    double y = pos.getY() + RANDOM.nextDouble() * 20 - 10;
                    // Clamp Y to world bounds
                    y = Math.max(world.getBottomY(), Math.min(world.getTopYInclusive(), y));
                    double z = pos.getZ() + RANDOM.nextDouble() * radius * 2 - radius;

                    ItemStack spawnStack = stack.copy();
                    spawnStack.setCount(1);

                    ItemEntity itemEntity = new ItemEntity(world, x, y, z, spawnStack);
                    world.spawnEntity(itemEntity);
                }
            }
        }
    }

    private void spawnInNearbyChests(ServerWorld world, ItemStack stack, int count) {
        // Limit search to avoid performance issues with large radius
        int searchRadius = Math.min(radius, 100);

        // Use sampling instead of checking every block - check random positions
        List<BlockPos> chestPositions = new ArrayList<>();
        int maxSamples = Math.min(1000, searchRadius * searchRadius); // Limit samples

        for (int sample = 0; sample < maxSamples && chestPositions.size() < 50; sample++) {
            int dx = RANDOM.nextInt(searchRadius * 2 + 1) - searchRadius;
            int dz = RANDOM.nextInt(searchRadius * 2 + 1) - searchRadius;

            // Check a vertical column for chests
            for (int dy = -10; dy <= 10; dy++) {
                BlockPos checkPos = pos.add(dx, dy, dz);
                if (world.getBlockState(checkPos).getBlock() instanceof net.minecraft.block.ChestBlock) {
                    chestPositions.add(checkPos);
                    break; // Found one in this column, move on
                }
            }
        }

        if (chestPositions.isEmpty()) return;

        for (int i = 0; i < count; i++) {
            BlockPos chestPos = chestPositions.get(RANDOM.nextInt(chestPositions.size()));
            BlockEntity be = world.getBlockEntity(chestPos);
            if (be instanceof net.minecraft.inventory.Inventory inv) {
                ItemStack spawnStack = stack.copy();
                spawnStack.setCount(1);
                for (int slot = 0; slot < inv.size(); slot++) {
                    if (inv.getStack(slot).isEmpty()) {
                        inv.setStack(slot, spawnStack);
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        Inventories.writeData(view, items);
        view.putInt("radius", radius);
        view.putInt("spawnInterval", spawnInterval);
        view.putInt("itemCount", itemCount);
        view.putBoolean("spawnInChests", spawnInChests);
        view.putBoolean("useGlobalSettings", useGlobalSettings);
        view.putBoolean("enabled", enabled);
        // Zone capture settings
        view.putInt("zoneRadius", zoneRadius);
        view.putInt("baseCaptureTime", baseCaptureTime);
        view.putInt("minCaptureTime", minCaptureTime);
        view.putBoolean("zoneEnabled", zoneEnabled);
        view.putString("zoneName", zoneName);
        view.putInt("bossBarColor", bossBarColor);
        view.putString("captureReward", captureReward);
        // Spawner dependencies
        view.putInt("parentCount", parentSpawners.size());
        for (int i = 0; i < parentSpawners.size(); i++) {
            view.putLong("parent_" + i, parentSpawners.get(i).asLong());
        }
        view.putInt("unlockDelay", unlockDelay);
        view.putLong("unlockTime", unlockTime);
        view.putString("lastRequiredFaction", lastRequiredFaction.name());
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        Inventories.readData(view, items);
        radius = view.getInt("radius", 50);
        spawnInterval = view.getInt("spawnInterval", 6000);
        itemCount = view.getInt("itemCount", 1);
        spawnInChests = view.getBoolean("spawnInChests", false);
        useGlobalSettings = view.getBoolean("useGlobalSettings", true);
        enabled = view.getBoolean("enabled", true);
        // Zone capture settings
        zoneRadius = view.getInt("zoneRadius", 250);
        baseCaptureTime = view.getInt("baseCaptureTime", 12000);
        minCaptureTime = view.getInt("minCaptureTime", 6000);
        zoneEnabled = view.getBoolean("zoneEnabled", true);
        zoneName = view.getString("zoneName", "");
        bossBarColor = view.getInt("bossBarColor", 0);
        captureReward = view.getString("captureReward", "");
        // Spawner dependencies
        parentSpawners = new ArrayList<>();
        int parentCount = view.getInt("parentCount", 0);
        for (int i = 0; i < parentCount; i++) {
            long posLong = view.getLong("parent_" + i, 0);
            if (posLong != 0) {
                parentSpawners.add(BlockPos.fromLong(posLong));
            }
        }
        unlockDelay = view.getInt("unlockDelay", 300);
        unlockTime = view.getLong("unlockTime", -1);

        // Load last required faction
        String factionName = view.getString("lastRequiredFaction", "NONE");
        try {
            lastRequiredFaction = FactionManager.Faction.valueOf(factionName);
        } catch (IllegalArgumentException e) {
            lastRequiredFaction = FactionManager.Faction.NONE;
        }
    }

    // Геттеры и сеттеры
    public int getRadius() { return radius; }
    public void setRadius(int radius) { this.radius = radius; markDirty(); }

    public int getSpawnInterval() { return spawnInterval; }
    public void setSpawnInterval(int interval) { this.spawnInterval = interval; markDirty(); }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int count) { this.itemCount = count; markDirty(); }

    public boolean isSpawnInChests() { return spawnInChests; }
    public void setSpawnInChests(boolean value) { this.spawnInChests = value; markDirty(); }

    public boolean isUseGlobalSettings() { return useGlobalSettings; }
    public void setUseGlobalSettings(boolean value) { this.useGlobalSettings = value; markDirty(); }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { this.enabled = value; markDirty(); }

    public DefaultedList<ItemStack> getItems() { return items; }

    // Zone capture settings getters/setters
    public int getZoneRadius() { return zoneRadius; }
    public void setZoneRadius(int value) { this.zoneRadius = value; markDirty(); }

    public int getBaseCaptureTime() { return baseCaptureTime; }
    public void setBaseCaptureTime(int value) { this.baseCaptureTime = value; markDirty(); }

    public int getMinCaptureTime() { return minCaptureTime; }
    public void setMinCaptureTime(int value) { this.minCaptureTime = value; markDirty(); }

    public boolean isZoneEnabled() { return zoneEnabled; }
    public void setZoneEnabled(boolean value) { this.zoneEnabled = value; markDirty(); }

    public String getZoneName() { return zoneName; }
    public void setZoneName(String value) { this.zoneName = value; markDirty(); }

    public int getBossBarColor() { return bossBarColor; }
    public void setBossBarColor(int value) { this.bossBarColor = value; markDirty(); }

    public String getCaptureReward() { return captureReward; }
    public void setCaptureReward(String value) { this.captureReward = value; markDirty(); }

    // Spawner dependency methods
    public List<BlockPos> getParentSpawners() { return new ArrayList<>(parentSpawners); }

    public void addParentSpawner(BlockPos pos) {
        if (!parentSpawners.contains(pos)) {
            parentSpawners.add(pos);
            markDirty();
        }
    }

    public void addParentSpawner(BlockPos pos, int unlockDelaySeconds) {
        if (!parentSpawners.contains(pos)) {
            parentSpawners.add(pos);
            this.unlockDelay = unlockDelaySeconds;
            markDirty();
        }
    }

    public void removeParentSpawner(BlockPos pos) {
        parentSpawners.remove(pos);
        // Reset unlock state when removing parent
        unlockTime = -1;
        lastRequiredFaction = FactionManager.Faction.NONE;
        markDirty();
    }

    public void clearParentSpawners() {
        parentSpawners.clear();
        unlockTime = -1;
        lastRequiredFaction = FactionManager.Faction.NONE;
        markDirty();
    }

    public int getUnlockDelay() { return unlockDelay; }
    public void setUnlockDelay(int seconds) { this.unlockDelay = seconds; markDirty(); }

    /**
     * Checks if spawner is unlocked (all parents captured by same faction)
     */
    public boolean isUnlocked(World world) {
        // No parents = always unlocked
        if (parentSpawners.isEmpty()) {
            return true;
        }

        // Check if all parents are captured by the same faction
        FactionManager.Faction requiredFaction = null;
        for (BlockPos parentPos : parentSpawners) {
            BlockEntity be = world.getBlockEntity(parentPos);
            if (!(be instanceof ItemSpawnerBlockEntity parent)) {
                continue; // Parent doesn't exist or invalid
            }

            FactionManager.Faction parentOwner = ZoneCaptureManager.getZoneOwner(parentPos);
            if (parentOwner == FactionManager.Faction.NONE) {
                return false; // Parent not captured yet
            }

            if (requiredFaction == null) {
                requiredFaction = parentOwner;
            } else if (requiredFaction != parentOwner) {
                return false; // Parents owned by different factions
            }
        }

        // All parents captured by same faction
        if (requiredFaction != null) {
            // Reset timer if faction changed
            if (lastRequiredFaction != requiredFaction) {
                lastRequiredFaction = requiredFaction;
                unlockTime = world.getTime() + (unlockDelay * 20L); // Convert seconds to ticks
                markDirty();
                return false;
            }

            // Start unlock timer if not started
            if (unlockTime == -1) {
                unlockTime = world.getTime() + (unlockDelay * 20L); // Convert seconds to ticks
                markDirty();
                return false;
            }

            // Check if unlock time reached
            if (world.getTime() >= unlockTime) {
                return true;
            }

            return false; // Still waiting
        }

        // No faction owns all parents, reset state
        if (lastRequiredFaction != FactionManager.Faction.NONE) {
            lastRequiredFaction = FactionManager.Faction.NONE;
            unlockTime = -1;
            markDirty();
        }

        return false;
    }

    // NamedScreenHandlerFactory implementation
    @Override
    public Text getDisplayName() {
        return Text.translatable("block.tekilo.item_spawner");
    }

    @Override
    public ItemSpawnerOpenData getScreenOpeningData(ServerPlayerEntity player) {
        return new ItemSpawnerOpenData(pos, zoneName, captureReward);
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new ItemSpawnerScreenHandler(syncId, playerInventory, this, propertyDelegate, pos);
    }

    // Inventory implementation
    @Override
    public int size() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        ItemStack result = Inventories.splitStack(items, slot, amount);
        if (!result.isEmpty()) {
            markDirty();
        }
        return result;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(items, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxCountPerStack()) {
            stack.setCount(getMaxCountPerStack());
        }
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return Inventory.canPlayerUse(this, player);
    }

    @Override
    public void clear() {
        items.clear();
        markDirty();
    }
}
