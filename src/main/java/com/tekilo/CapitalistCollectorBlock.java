package com.tekilo;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class CapitalistCollectorBlock extends FactionCollectorBlock {
    public static final MapCodec<CapitalistCollectorBlock> CODEC = createCodec(CapitalistCollectorBlock::new);

    public CapitalistCollectorBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends FactionCollectorBlock> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CapitalistCollectorBlockEntity(pos, state);
    }

    @Override
    protected FactionManager.Faction getFaction() {
        return FactionManager.Faction.CAPITALIST;
    }
}
