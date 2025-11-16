package com.tekilo.screen;

import com.tekilo.ItemSpawnerBlockEntity;
import com.tekilo.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class ItemSpawnerScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PropertyDelegate propertyDelegate;
    private final net.minecraft.util.math.BlockPos blockPos;

    // Client constructor (with block pos from extended data)
    public ItemSpawnerScreenHandler(int syncId, PlayerInventory playerInventory, net.minecraft.util.math.BlockPos blockPos) {
        this(syncId, playerInventory, new SimpleInventory(27), new ArrayPropertyDelegate(6), blockPos);
    }

    // Server constructor
    public ItemSpawnerScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate, net.minecraft.util.math.BlockPos blockPos) {
        super(ModScreenHandlers.ITEM_SPAWNER_SCREEN_HANDLER, syncId);
        checkSize(inventory, 27);
        this.inventory = inventory;
        this.propertyDelegate = propertyDelegate;
        this.blockPos = blockPos;

        inventory.onOpen(playerInventory.player);

        // Add spawner inventory slots (3x9 grid)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Add player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addProperties(propertyDelegate);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slotObj = this.slots.get(slot);

        if (slotObj.hasStack()) {
            ItemStack originalStack = slotObj.getStack();
            newStack = originalStack.copy();

            if (slot < this.inventory.size()) {
                if (!this.insertItem(originalStack, this.inventory.size(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, this.inventory.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slotObj.setStack(ItemStack.EMPTY);
            } else {
                slotObj.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    public int getRadius() {
        return propertyDelegate.get(0);
    }

    public int getSpawnInterval() {
        return propertyDelegate.get(1);
    }

    public int getItemCount() {
        return propertyDelegate.get(2);
    }

    public boolean isSpawnInChests() {
        return propertyDelegate.get(3) == 1;
    }

    public boolean isUseGlobalSettings() {
        return propertyDelegate.get(4) == 1;
    }

    public boolean isEnabled() {
        return propertyDelegate.get(5) == 1;
    }

    public net.minecraft.util.math.BlockPos getBlockPos() {
        return blockPos;
    }
}
