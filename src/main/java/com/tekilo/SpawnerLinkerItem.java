package com.tekilo;

import com.tekilo.network.SpawnerLinkOpenData;
import com.tekilo.screen.SpawnerLinkScreenHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SpawnerLinkerItem extends Item {
    private static final String PARENT_POS_KEY = "ParentPos";

    public SpawnerLinkerItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        if (player == null) {
            return ActionResult.PASS;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof ItemSpawnerBlockEntity spawner)) {
            return ActionResult.PASS;
        }

        // Get or create NBT component
        NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtComponent.copyNbt();

        // If no parent stored, store this as parent
        if (!nbt.contains(PARENT_POS_KEY)) {
            nbt.putLong(PARENT_POS_KEY, pos.asLong());
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

            if (world.isClient()) {
                player.sendMessage(
                    Text.translatable("item.tekilo.spawner_linker.parent_set",
                        pos.getX(), pos.getY(), pos.getZ())
                    .formatted(Formatting.GREEN),
                    true
                );
            } else {
                // Spawn particles
                spawnParticles((ServerWorld) world, pos, ParticleTypes.HAPPY_VILLAGER);
            }

            return ActionResult.SUCCESS;
        }

        // Parent exists, open GUI to select unlock time
        if (!nbt.contains(PARENT_POS_KEY)) {
            return ActionResult.FAIL; // Should never happen, but safety check
        }
        long parentPosLong = nbt.getLong(PARENT_POS_KEY).orElse(0L);
        if (parentPosLong == 0L) {
            return ActionResult.FAIL;
        }
        BlockPos parentPos = BlockPos.fromLong(parentPosLong);

        // Can't link to itself
        if (parentPos.equals(pos)) {
            if (world.isClient()) {
                player.sendMessage(
                    Text.translatable("item.tekilo.spawner_linker.same_spawner")
                    .formatted(Formatting.RED),
                    true
                );
            }
            return ActionResult.FAIL;
        }

        // Check if parent still exists
        BlockEntity parentBe = world.getBlockEntity(parentPos);
        if (!(parentBe instanceof ItemSpawnerBlockEntity)) {
            if (world.isClient()) {
                player.sendMessage(
                    Text.translatable("item.tekilo.spawner_linker.parent_not_found")
                    .formatted(Formatting.RED),
                    true
                );
            }
            nbt.remove(PARENT_POS_KEY);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
            return ActionResult.FAIL;
        }

        // Open GUI for time selection
        if (!world.isClient() && player instanceof ServerPlayerEntity serverPlayer) {
            BlockPos finalParentPos = parentPos;
            BlockPos finalChildPos = pos;

            serverPlayer.openHandledScreen(new ExtendedScreenHandlerFactory<SpawnerLinkOpenData>() {
                @Override
                public Text getDisplayName() {
                    return Text.translatable("gui.tekilo.spawner_link.title");
                }

                @Override
                public SpawnerLinkOpenData getScreenOpeningData(ServerPlayerEntity player) {
                    return new SpawnerLinkOpenData(finalParentPos, finalChildPos);
                }

                @Nullable
                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    return new SpawnerLinkScreenHandler(syncId, playerInventory, finalParentPos, finalChildPos);
                }
            });

            // Clear stored parent from item
            nbt.remove(PARENT_POS_KEY);
            stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        }

        return ActionResult.SUCCESS;
    }

    private void spawnParticles(ServerWorld world, BlockPos pos, net.minecraft.particle.ParticleEffect particle) {
        for (int i = 0; i < 10; i++) {
            double x = pos.getX() + 0.5 + (world.random.nextDouble() - 0.5);
            double y = pos.getY() + 0.5 + world.random.nextDouble();
            double z = pos.getZ() + 0.5 + (world.random.nextDouble() - 0.5);
            world.spawnParticles(particle, x, y, z, 1, 0, 0, 0, 0);
        }
    }
}
