package com.tekilo;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class CommunistCollectorBlockEntity extends FactionCollectorBlockEntity {
    public CommunistCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMMUNIST_COLLECTOR, pos, state);
    }

    @Override
    public FactionManager.Faction getFaction() {
        return FactionManager.Faction.COMMUNIST;
    }
}
