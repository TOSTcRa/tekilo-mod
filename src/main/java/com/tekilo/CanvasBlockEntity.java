package com.tekilo;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class CanvasBlockEntity extends BlockEntity {
    // Пиксели картины, размер зависит от canvasWidth/canvasHeight
    private int[] pixels = new int[16 * 16];
    private int canvasWidth = 1; // Размер холста в блоках (1x1, 2x2, и т.д.)
    private int canvasHeight = 1;
    private boolean editable = true; // Если false - картина завершена, не открывается GUI
    private boolean sizeChosen = false; // Был ли выбран размер холста

    public CanvasBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CANVAS, pos, state);
        // Инициализируем белым цветом (по умолчанию 16x16)
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0xFFFFFF; // Белый
        }
        System.out.println("[TekiloMod] CanvasBlockEntity created at " + pos + " (empty/white pixels)");
    }

    public int getPixel(int x, int y) {
        int pixelWidth = canvasWidth * 16;
        int pixelHeight = canvasHeight * 16;
        if (x < 0 || x >= pixelWidth || y < 0 || y >= pixelHeight) return 0xFFFFFF;
        return pixels[y * pixelWidth + x];
    }

    public void setPixel(int x, int y, int color) {
        int pixelWidth = canvasWidth * 16;
        int pixelHeight = canvasHeight * 16;
        if (x < 0 || x >= pixelWidth || y < 0 || y >= pixelHeight) return;
        pixels[y * pixelWidth + x] = color;
        markDirty();
    }

    public int[] getPixels() {
        return pixels;
    }

    public void setPixels(int[] newPixels) {
        int expectedSize = canvasWidth * 16 * canvasHeight * 16;
        if (newPixels.length == expectedSize) {
            this.pixels = newPixels;
            markDirty();

            // Debug
            boolean hasContent = false;
            for (int pixel : newPixels) {
                if (pixel != 0xFFFFFF) {
                    hasContent = true;
                    break;
                }
            }
            System.out.println("[TekiloMod] CanvasBlockEntity.setPixels at " + pos + ", hasContent=" + hasContent + ", size=" + canvasWidth + "x" + canvasHeight);
        } else {
            System.out.println("[TekiloMod] CanvasBlockEntity.setPixels REJECTED: expected " + expectedSize + " but got " + newPixels.length);
        }
    }

    public int getCanvasWidth() { return canvasWidth; }
    public int getCanvasHeight() { return canvasHeight; }
    public boolean isSizeChosen() { return sizeChosen; }

    public void setCanvasSize(int width, int height) {
        if (width < 1) width = 1;
        if (height < 1) height = 1;
        if (width > 6) width = 6;
        if (height > 6) height = 6;

        this.canvasWidth = width;
        this.canvasHeight = height;
        this.sizeChosen = true;

        // Пересоздаём массив пикселей под новый размер
        int newSize = width * 16 * height * 16;
        this.pixels = new int[newSize];
        for (int i = 0; i < newSize; i++) {
            pixels[i] = 0xFFFFFF;
        }

        markDirty();
        System.out.println("[TekiloMod] CanvasBlockEntity.setCanvasSize at " + pos + " to " + width + "x" + height + " blocks (" + (width*16) + "x" + (height*16) + " pixels)");
    }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) {
        this.editable = editable;
        markDirty();
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putIntArray("pixels", pixels);
        view.putInt("canvasWidth", canvasWidth);
        view.putInt("canvasHeight", canvasHeight);
        view.putBoolean("editable", editable);
        view.putBoolean("sizeChosen", sizeChosen);

        // Debug: логируем запись данных
        boolean hasContent = false;
        for (int pixel : pixels) {
            if (pixel != 0xFFFFFF) {
                hasContent = true;
                break;
            }
        }
        System.out.println("[TekiloMod] CanvasBlockEntity.writeData at " + pos + ", hasContent=" + hasContent + ", editable=" + editable + ", size=" + canvasWidth + "x" + canvasHeight);
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        // Сначала читаем размеры
        canvasWidth = view.getInt("canvasWidth", 1);
        canvasHeight = view.getInt("canvasHeight", 1);
        editable = view.getBoolean("editable", true);
        sizeChosen = view.getBoolean("sizeChosen", false);

        if (canvasWidth < 1) canvasWidth = 1;
        if (canvasHeight < 1) canvasHeight = 1;
        if (canvasWidth > 6) canvasWidth = 6;
        if (canvasHeight > 6) canvasHeight = 6;

        int expectedSize = canvasWidth * 16 * canvasHeight * 16;

        boolean hadPixels = view.getOptionalIntArray("pixels").isPresent();
        view.getOptionalIntArray("pixels").ifPresent(loadedPixels -> {
            if (loadedPixels.length == expectedSize) {
                pixels = loadedPixels;
                System.out.println("[TekiloMod] CanvasBlockEntity.readData at " + pos + " - loaded " + loadedPixels.length + " pixels for " + canvasWidth + "x" + canvasHeight + " canvas");
            } else {
                System.out.println("[TekiloMod] CanvasBlockEntity.readData at " + pos + " - invalid pixels length: " + loadedPixels.length + ", expected " + expectedSize);
                // Пересоздаём массив правильного размера
                pixels = new int[expectedSize];
                for (int i = 0; i < expectedSize; i++) {
                    pixels[i] = 0xFFFFFF;
                }
            }
        });

        if (!hadPixels) {
            System.out.println("[TekiloMod] CanvasBlockEntity.readData at " + pos + " - NO pixels in data!");
            pixels = new int[expectedSize];
            for (int i = 0; i < expectedSize; i++) {
                pixels[i] = 0xFFFFFF;
            }
        }

        // Debug: проверяем что загрузили
        boolean hasContent = false;
        for (int pixel : pixels) {
            if (pixel != 0xFFFFFF) {
                hasContent = true;
                break;
            }
        }
        System.out.println("[TekiloMod] CanvasBlockEntity.readData result: hasContent=" + hasContent + ", editable=" + editable + ", size=" + canvasWidth + "x" + canvasHeight);
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        System.out.println("[TekiloMod] CanvasBlockEntity.toUpdatePacket called at " + pos);
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        System.out.println("[TekiloMod] CanvasBlockEntity.toInitialChunkDataNbt called at " + pos);
        // Используем createNbtWithIdentifyingData, который вызывает writeFullData
        NbtCompound nbt = createNbtWithIdentifyingData(registries);
        System.out.println("[TekiloMod] toInitialChunkDataNbt result keys: " + nbt.getKeys());
        return nbt;
    }

    @Override
    public void writeFullData(WriteView view) {
        super.writeFullData(view);
        view.putIntArray("pixels", pixels);
        view.putInt("canvasWidth", canvasWidth);
        view.putInt("canvasHeight", canvasHeight);
        view.putBoolean("editable", editable);
        view.putBoolean("sizeChosen", sizeChosen);

        // Debug: логируем запись полных данных
        boolean hasContent = false;
        for (int pixel : pixels) {
            if (pixel != 0xFFFFFF) {
                hasContent = true;
                break;
            }
        }
        System.out.println("[TekiloMod] CanvasBlockEntity.writeFullData at " + pos + ", hasContent=" + hasContent + ", editable=" + editable + ", size=" + canvasWidth + "x" + canvasHeight);
    }
}
