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
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import org.jetbrains.annotations.Nullable;

public class CanvasBlock extends BlockWithEntity {

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final MapCodec<CanvasBlock> CODEC = createCodec(CanvasBlock::new);

    // Тонкие формы для каждого направления (толщина 1/16 блока = 1 пиксель)
    // facing = КУДА СМОТРИТ холст (не где стена!)
    // Должны соответствовать blockstates rotations модели canvas.json (element от [0,0,0] до [16,16,1])
    // facing=south: без поворота → z=0-1 (прижат к северу, смотрит на юг)
    // facing=north: y=180 → z=15-16 (прижат к югу, смотрит на север)
    // facing=west: y=90 → x=15-16 (прижат к востоку, смотрит на запад)
    // facing=east: y=270 → x=0-1 (прижат к западу, смотрит на восток)
    private static final VoxelShape NORTH_SHAPE = createCuboidShape(0.0, 0.0, 15.0, 16.0, 16.0, 16.0); // Смотрит на север, прижат к югу
    private static final VoxelShape SOUTH_SHAPE = createCuboidShape(0.0, 0.0, 0.0, 16.0, 16.0, 1.0);   // Смотрит на юг, прижат к северу
    private static final VoxelShape WEST_SHAPE = createCuboidShape(15.0, 0.0, 0.0, 16.0, 16.0, 16.0);  // Смотрит на запад, прижат к востоку
    private static final VoxelShape EAST_SHAPE = createCuboidShape(0.0, 0.0, 0.0, 1.0, 16.0, 16.0);    // Смотрит на восток, прижат к западу

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
            // FACING указывает КУДА СМОТРИТ холст (на игрока)
            // Кликнули на SOUTH сторону блока → блок размещается южнее → холст смотрит на ЮГ
            return getDefaultState().with(FACING, clickedSide);
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
        // FACING указывает КУДА СМОТРИТ холст
        // Стена находится ПОЗАДИ холста (в противоположном направлении)
        // Например: FACING=SOUTH (смотрит на юг) → стена на СЕВЕРЕ
        BlockPos wallPos = pos.offset(facing.getOpposite());
        BlockState wallState = world.getBlockState(wallPos);
        // Можно ставить если стена имеет solid face в сторону холста
        return wallState.isSideSolidFullSquare(world, wallPos, facing);
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
        // INVISIBLE - модель блока не рендерится, только BlockEntity рендерер
        return BlockRenderType.INVISIBLE;
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

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CanvasBlockEntity canvas) {
                // Найти главный блок
                CanvasBlockEntity master = canvas.getMasterEntity();
                if (master != null && !master.isEditable()) {
                    // Это завершённая картина - удаляем всю структуру
                    Direction facing = state.get(FACING);
                    int width = master.getCanvasWidth();
                    int height = master.getCanvasHeight();
                    BlockPos masterPos = master.getPos();

                    // Дропаем только один предмет (картину с данными)
                    if (!player.isCreative()) {
                        ItemStack paintingItem = CanvasPaintingItem.createWithPixels(
                            master.getPixels(), width, height
                        );
                        Block.dropStack(world, pos, paintingItem);
                    }

                    // Удаляем все блоки структуры (кроме текущего, он удалится автоматически)
                    for (int dy = 0; dy < height; dy++) {
                        for (int dx = 0; dx < width; dx++) {
                            BlockPos blockPos = getMultiblockPos(masterPos, facing, dx, dy);
                            if (!blockPos.equals(pos)) {
                                // Удаляем без дропа
                                world.removeBlock(blockPos, false);
                            }
                        }
                    }
                }
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    // Вычисляет позицию блока в мультиблочной структуре
    // dx - смещение по горизонтали (вдоль стены), dy - по вертикали
    public static BlockPos getMultiblockPos(BlockPos masterPos, Direction facing, int dx, int dy) {
        // Главный блок - левый нижний угол картины (с точки зрения смотрящего на неё)
        // dx растёт вправо, dy растёт вверх
        // facing указывает КУДА СМОТРИТ холст
        // "вправо" для зрителя (смотрящего на холст) = facing.rotateYClockwise()
        // Например: facing=SOUTH (смотрит на юг), зритель на юге смотрит на север, вправо = WEST
        Direction right = facing.rotateYClockwise();
        return masterPos.offset(right, dx).up(dy);
    }
}
