package com.tekilo;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class StalinStatueBlock extends Block {

    public enum StatuePart implements StringIdentifiable {
        BOTTOM("bottom"),
        MIDDLE("middle"),
        TOP("top");

        private final String name;

        StatuePart(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return name;
        }
    }

    public static final EnumProperty<StatuePart> PART = EnumProperty.of("part", StatuePart.class);

    private static final VoxelShape SHAPE = VoxelShapes.cuboid(0.125, 0, 0.125, 0.875, 1, 0.875);

    public StalinStatueBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(PART, StatuePart.BOTTOM));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();

        // Проверяем, есть ли место для всех трех блоков
        if (pos.getY() > world.getTopYInclusive() - 2) {
            return null;
        }

        if (!world.getBlockState(pos.up()).canReplace(ctx) ||
            !world.getBlockState(pos.up(2)).canReplace(ctx)) {
            return null;
        }

        return getDefaultState().with(PART, StatuePart.BOTTOM);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        if (!world.isClient() && state.get(PART) == StatuePart.BOTTOM) {
            // Размещаем средний и верхний блоки
            world.setBlockState(pos.up(), state.with(PART, StatuePart.MIDDLE), Block.NOTIFY_ALL);
            world.setBlockState(pos.up(2), state.with(PART, StatuePart.TOP), Block.NOTIFY_ALL);
        }
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            StatuePart part = state.get(PART);
            BlockPos bottomPos = getBottomPos(pos, part);

            // Удаляем все три части статуи
            if (part != StatuePart.BOTTOM) {
                world.setBlockState(bottomPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
            if (part != StatuePart.MIDDLE) {
                world.setBlockState(bottomPos.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
            if (part != StatuePart.TOP) {
                world.setBlockState(bottomPos.up(2), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
            }
        }

        return super.onBreak(world, pos, state, player);
    }

    private BlockPos getBottomPos(BlockPos pos, StatuePart part) {
        return switch (part) {
            case BOTTOM -> pos;
            case MIDDLE -> pos.down();
            case TOP -> pos.down(2);
        };
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        StatuePart part = state.get(PART);
        if (part == StatuePart.BOTTOM) {
            return true;
        }

        // Средний и верхний блоки должны иметь нижнюю часть
        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);

        if (part == StatuePart.MIDDLE) {
            return belowState.isOf(this) && belowState.get(PART) == StatuePart.BOTTOM;
        } else { // TOP
            return belowState.isOf(this) && belowState.get(PART) == StatuePart.MIDDLE;
        }
    }
}
