package com.tekilo.screen;

import com.tekilo.ModScreenHandlers;
import com.tekilo.network.SpawnerLinkOpenData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

public class SpawnerLinkScreenHandler extends ScreenHandler {
    private final BlockPos parentPos;
    private final BlockPos childPos;

    public SpawnerLinkScreenHandler(int syncId, PlayerInventory playerInventory, SpawnerLinkOpenData data) {
        this(syncId, playerInventory, data.parentPos(), data.childPos());
    }

    public SpawnerLinkScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos parentPos, BlockPos childPos) {
        super(ModScreenHandlers.SPAWNER_LINK_SCREEN_HANDLER, syncId);
        this.parentPos = parentPos;
        this.childPos = childPos;
    }

    public BlockPos getParentPos() {
        return parentPos;
    }

    public BlockPos getChildPos() {
        return childPos;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
