package com.tekilo;

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

public class ItemSpawnerBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos>, Inventory {
    private DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
    private int radius = 50;
    private int spawnInterval = 6000; // ticks (5 minutes)
    private int itemCount = 1;
    private boolean spawnInChests = false;
    private boolean useGlobalSettings = true;
    private boolean enabled = true; // ON/OFF toggle
    private int tickCounter = 0;
    private static final Random RANDOM = new Random(); // Reuse random instance

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
            }
            markDirty();
        }

        @Override
        public int size() {
            return 6;
        }
    };

    public ItemSpawnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_SPAWNER, pos, state);
        // Регистрируем зону захвата
        ZoneCaptureManager.registerZone(pos);
    }

    public static void tick(World world, BlockPos pos, BlockState state, ItemSpawnerBlockEntity blockEntity) {
        if (world.isClient()) return;

        // Only spawn items if enabled
        if (blockEntity.enabled) {
            blockEntity.tickCounter++;
            if (blockEntity.tickCounter >= blockEntity.spawnInterval) {
                blockEntity.tickCounter = 0;
                blockEntity.spawnItems((ServerWorld) world);
            }
        }

        // Обновляем систему захвата зон только раз в секунду и только для первого спавнера
        // чтобы избежать множественных вызовов
        if (blockEntity.tickCounter % 20 == 0) {
            ZoneCaptureManager.tickOnce((ServerWorld) world);
        }
    }

    private void spawnItems(ServerWorld world) {
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) continue;

            int count = useGlobalSettings ? itemCount : stack.getCount();

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

    // NamedScreenHandlerFactory implementation
    @Override
    public Text getDisplayName() {
        return Text.translatable("block.tekilo.item_spawner");
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return pos;
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
