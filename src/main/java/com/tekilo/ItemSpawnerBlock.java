package com.tekilo;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ItemSpawnerBlock extends BlockWithEntity {

    public static final MapCodec<ItemSpawnerBlock> CODEC = createCodec(ItemSpawnerBlock::new);

    public ItemSpawnerBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ItemSpawnerBlockEntity(pos, state);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.ITEM_SPAWNER, ItemSpawnerBlockEntity::tick);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient()) {
            // Проверяем права OP
            if (player instanceof ServerPlayerEntity serverPlayer) {
                if (!serverPlayer.hasPermissionLevel(2)) {
                    player.sendMessage(Text.translatable("message.tekilo.item_spawner.no_permission"), true);
                    return ActionResult.FAIL;
                }

                // Открываем GUI
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof NamedScreenHandlerFactory screenHandlerFactory) {
                    player.openHandledScreen(screenHandlerFactory);
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, net.minecraft.world.BlockView world, BlockPos pos) {
        // Только OP может ломать
        if (player instanceof ServerPlayerEntity serverPlayer) {
            if (serverPlayer.hasPermissionLevel(2)) {
                return super.calcBlockBreakingDelta(state, player, world, pos);
            }
        }
        return 0.0f; // Неразрушимый для обычных игроков
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        // Блок был удален - очищаем зону захвата
        ZoneCaptureManager.unregisterZone(pos);
        super.onStateReplaced(state, world, pos, moved);
    }
}
