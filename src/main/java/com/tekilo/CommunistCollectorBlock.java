package com.tekilo;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class CommunistCollectorBlock extends FactionCollectorBlock {
    public static final MapCodec<CommunistCollectorBlock> CODEC = createCodec(CommunistCollectorBlock::new);

    public CommunistCollectorBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends FactionCollectorBlock> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CommunistCollectorBlockEntity(pos, state);
    }

    @Override
    protected FactionManager.Faction getFaction() {
        return FactionManager.Faction.COMMUNIST;
    }
}
