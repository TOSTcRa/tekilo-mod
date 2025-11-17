package com.tekilo;

import com.mojang.serialization.MapCodec;
import com.tekilo.network.OpenCanvasScreenPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class CanvasBlock extends BlockWithEntity {

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final MapCodec<CanvasBlock> CODEC = createCodec(CanvasBlock::new);

    // Тонкие формы для каждого направления (толщина 1/16 блока = 1 пиксель)
    // Холст смотрит В направлении facing, значит прижат к стене В направлении facing
    private static final VoxelShape NORTH_SHAPE = createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);  // Прижат к северной стене (смотрит на север)
    private static final VoxelShape SOUTH_SHAPE = createCuboidShape(0.0, 0.0, 15.0, 16.0, 16.0, 16.0); // Прижат к южной стене (смотрит на юг)
    private static final VoxelShape WEST_SHAPE = createCuboidShape(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);   // Прижат к западной стене (смотрит на запад)
    private static final VoxelShape EAST_SHAPE = createCuboidShape(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);  // Прижат к восточной стене (смотрит на восток)

    public CanvasBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<net.minecraft.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction clickedSide = ctx.getSide();

        // Если кликнули на горизонтальную сторону (стена), ставим на стену
        if (clickedSide.getAxis().isHorizontal()) {
            // Холст "смотрит" НА игрока (противоположно стороне на которую кликнули)
            return getDefaultState().with(FACING, clickedSide.getOpposite());
        }

        // Если кликнули на верх/низ блока, используем направление игрока
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return switch (state.get(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> SOUTH_SHAPE;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        Direction facing = state.get(FACING);
        // Блок за холстом (стена на которую он крепится)
        // Холст смотрит НА игрока, стена ПОЗАДИ холста (в направлении facing)
        BlockPos wallPos = pos.offset(facing);
        BlockState wallState = world.getBlockState(wallPos);
        // Можно ставить если есть твёрдая стена позади
        return wallState.isSideSolidFullSquare(world, wallPos, facing.getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CanvasBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CanvasBlockEntity canvas && player instanceof ServerPlayerEntity serverPlayer) {
                // Только если картина редактируемая, открываем GUI
                if (canvas.isEditable()) {
                    OpenCanvasScreenPayload payload = new OpenCanvasScreenPayload(
                        pos,
                        canvas.getPixels(),
                        canvas.getCanvasWidth(),
                        canvas.getCanvasHeight(),
                        canvas.isSizeChosen()
                    );
                    ServerPlayNetworking.send(serverPlayer, payload);
                }
                // Если не редактируемая - просто смотрим на картину, ничего не делаем
            }
        }
        return ActionResult.SUCCESS;
    }
}
