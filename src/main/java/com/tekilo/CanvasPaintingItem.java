package com.tekilo;

import net.minecraft.block.BlockState;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class CanvasPaintingItem extends Item {

    public CanvasPaintingItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getBlockPos();
        Direction side = context.getSide();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        // Картина ставится только на горизонтальные стороны (стены)
        if (!side.getAxis().isHorizontal()) {
            return ActionResult.FAIL;
        }

        // Получаем размеры картины
        Integer itemWidth = stack.get(ModDataComponents.CANVAS_WIDTH);
        Integer itemHeight = stack.get(ModDataComponents.CANVAS_HEIGHT);
        int canvasWidth = (itemWidth != null) ? itemWidth : 1;
        int canvasHeight = (itemHeight != null) ? itemHeight : 1;

        // FACING указывает КУДА СМОТРИТ холст (на игрока)
        Direction facing = side;

        // Позиция главного блока (левый нижний угол с точки зрения смотрящего)
        BlockPos masterPos = blockPos.offset(side);

        // Проверяем что можно разместить ВСЕ блоки структуры
        for (int dy = 0; dy < canvasHeight; dy++) {
            for (int dx = 0; dx < canvasWidth; dx++) {
                BlockPos checkPos = CanvasBlock.getMultiblockPos(masterPos, facing, dx, dy);

                // Проверяем что позиция свободна
                if (!world.getBlockState(checkPos).isReplaceable()) {
                    return ActionResult.FAIL;
                }

                // Проверяем что есть стена позади (в противоположном направлении)
                BlockPos wallPos = checkPos.offset(facing.getOpposite());
                BlockState wallState = world.getBlockState(wallPos);
                if (!wallState.isSideSolidFullSquare(world, wallPos, facing)) {
                    return ActionResult.FAIL;
                }
            }
        }

        if (!world.isClient()) {
            BlockState canvasState = ModBlocks.CANVAS.getDefaultState().with(CanvasBlock.FACING, facing);

            // Размещаем все блоки структуры
            for (int dy = 0; dy < canvasHeight; dy++) {
                for (int dx = 0; dx < canvasWidth; dx++) {
                    BlockPos placePos = CanvasBlock.getMultiblockPos(masterPos, facing, dx, dy);
                    world.setBlockState(placePos, canvasState);

                    if (world.getBlockEntity(placePos) instanceof CanvasBlockEntity canvas) {
                        canvas.setCanvasSize(canvasWidth, canvasHeight);

                        if (dx == 0 && dy == 0) {
                            // Главный блок - хранит данные картины
                            canvas.setMaster(true);
                            canvas.setOffset(0, 0);

                            int[] pixels = stack.get(ModDataComponents.CANVAS_PIXELS);
                            int expectedSize = canvasWidth * 16 * canvasHeight * 16;
                            if (pixels != null && pixels.length == expectedSize) {
                                canvas.setPixels(pixels.clone());
                            }

                            System.out.println("[TekiloMod] CanvasPaintingItem: Placed MASTER canvas at " + placePos);
                        } else {
                            // Зависимый блок - ссылается на главный
                            canvas.setMaster(false);
                            canvas.setMasterPos(masterPos);
                            canvas.setOffset(dx, dy);

                            System.out.println("[TekiloMod] CanvasPaintingItem: Placed SLAVE canvas at " + placePos + " offset(" + dx + "," + dy + ")");
                        }

                        canvas.setEditable(false);
                        canvas.markDirty();

                        // Синхронизируем с клиентами
                        if (world instanceof ServerWorld serverWorld) {
                            BlockEntityUpdateS2CPacket packet = (BlockEntityUpdateS2CPacket) canvas.toUpdatePacket();
                            if (packet != null) {
                                for (ServerPlayerEntity serverPlayer : serverWorld.getPlayers()) {
                                    if (serverPlayer.squaredDistanceTo(placePos.getX(), placePos.getY(), placePos.getZ()) < 256 * 256) {
                                        serverPlayer.networkHandler.sendPacket(packet);
                                    }
                                }
                            }
                            serverWorld.getChunkManager().markForUpdate(placePos);
                        }

                        world.updateListeners(placePos, canvasState, canvasState, net.minecraft.block.Block.NOTIFY_ALL);
                    }
                }
            }

            // Consume item if not in creative mode
            if (player != null && !player.isCreative()) {
                stack.decrement(1);
            }
        }

        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);

        int[] pixels = stack.get(ModDataComponents.CANVAS_PIXELS);
        Integer canvasWidth = stack.get(ModDataComponents.CANVAS_WIDTH);
        Integer canvasHeight = stack.get(ModDataComponents.CANVAS_HEIGHT);

        int width = (canvasWidth != null) ? canvasWidth : 1;
        int height = (canvasHeight != null) ? canvasHeight : 1;
        int expectedSize = width * 16 * height * 16;

        if (pixels != null && pixels.length == expectedSize) {
            textConsumer.accept(Text.translatable("item.tekilo.canvas_painting.custom").formatted(Formatting.AQUA));

            // Show canvas size
            textConsumer.accept(Text.literal(width + "x" + height + " blocks").formatted(Formatting.GREEN));

            // Count unique colors
            int uniqueColors = countUniqueColors(pixels);
            textConsumer.accept(Text.translatable("item.tekilo.canvas_painting.colors", uniqueColors).formatted(Formatting.GRAY));

            // Show if it's empty (all white)
            boolean isEmpty = true;
            for (int pixel : pixels) {
                if (pixel != 0xFFFFFF) {
                    isEmpty = false;
                    break;
                }
            }
            if (isEmpty) {
                textConsumer.accept(Text.translatable("item.tekilo.canvas_painting.empty").formatted(Formatting.DARK_GRAY));
            }
        } else {
            textConsumer.accept(Text.translatable("item.tekilo.canvas_painting.blank").formatted(Formatting.GRAY));
        }
    }

    private int countUniqueColors(int[] pixels) {
        java.util.Set<Integer> colors = new java.util.HashSet<>();
        for (int pixel : pixels) {
            colors.add(pixel);
        }
        return colors.size();
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        int[] pixels = stack.get(ModDataComponents.CANVAS_PIXELS);
        Integer canvasWidth = stack.get(ModDataComponents.CANVAS_WIDTH);
        Integer canvasHeight = stack.get(ModDataComponents.CANVAS_HEIGHT);

        int width = (canvasWidth != null) ? canvasWidth : 1;
        int height = (canvasHeight != null) ? canvasHeight : 1;
        int expectedSize = width * 16 * height * 16;

        if (pixels != null && pixels.length == expectedSize) {
            // Check if not empty
            boolean hasContent = false;
            for (int pixel : pixels) {
                if (pixel != 0xFFFFFF) {
                    hasContent = true;
                    break;
                }
            }
            if (hasContent) {
                // Return custom tooltip data for rendering preview
                return Optional.of(new CanvasPaintingTooltipData(pixels, width, height));
            }
        }
        return Optional.empty();
    }

    // Custom tooltip data record
    public record CanvasPaintingTooltipData(int[] pixels, int canvasWidth, int canvasHeight) implements TooltipData {
    }

    // Helper method to create a canvas painting with pixels
    public static ItemStack createWithPixels(int[] pixels) {
        return createWithPixels(pixels, 1, 1);
    }

    // Helper method to create a canvas painting with pixels and size
    public static ItemStack createWithPixels(int[] pixels, int canvasWidth, int canvasHeight) {
        ItemStack stack = new ItemStack(ModItems.CANVAS_PAINTING);
        int expectedSize = canvasWidth * 16 * canvasHeight * 16;
        if (pixels != null && pixels.length == expectedSize) {
            stack.set(ModDataComponents.CANVAS_PIXELS, pixels.clone());
            stack.set(ModDataComponents.CANVAS_WIDTH, canvasWidth);
            stack.set(ModDataComponents.CANVAS_HEIGHT, canvasHeight);
        }
        return stack;
    }
}
