package com.tekilo;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class CapitalistCollectorBlockEntity extends FactionCollectorBlockEntity {
    public CapitalistCollectorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAPITALIST_COLLECTOR, pos, state);
    }

    @Override
    public FactionManager.Faction getFaction() {
        return FactionManager.Faction.CAPITALIST;
    }
}
