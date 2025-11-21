package com.tekilo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;

public class FactionCollectorScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private static final int ROWS = 6; // 6 rows like double chest
    private static final int COLUMNS = 9;

    public FactionCollectorScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(ROWS * COLUMNS));
    }

    public FactionCollectorScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModScreenHandlers.FACTION_COLLECTOR_SCREEN_HANDLER, syncId);
        checkSize(inventory, ROWS * COLUMNS);
        this.inventory = inventory;
        inventory.onOpen(playerInventory.player);

        // Collector inventory slots (54 slots, 6 rows)
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                this.addSlot(new Slot(inventory, col + row * COLUMNS, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory (3 rows)
        int playerInventoryY = 18 + ROWS * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, playerInventoryY + row * 18));
            }
        }

        // Player hotbar
        int hotbarY = playerInventoryY + 3 * 18 + 4;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slotObj = this.slots.get(slot);

        if (slotObj.hasStack()) {
            ItemStack originalStack = slotObj.getStack();
            newStack = originalStack.copy();

            if (slot < ROWS * COLUMNS) {
                // From collector to player inventory
                if (!this.insertItem(originalStack, ROWS * COLUMNS, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player inventory to collector
                if (!this.insertItem(originalStack, 0, ROWS * COLUMNS, false)) {
                    return ItemStack.EMPTY;
                }
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

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.inventory.onClose(player);
    }

    public Inventory getInventory() {
        return inventory;
    }
}
