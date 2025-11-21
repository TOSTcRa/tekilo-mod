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

    // Мультиблочная структура
    private boolean isMaster = true; // true = главный блок (хранит данные), false = зависимый
    private BlockPos masterPos = null; // Позиция главного блока (для зависимых)
    private int offsetX = 0; // Смещение этого блока относительно главного (по горизонтали вдоль стены)
    private int offsetY = 0; // Смещение по вертикали

    public CanvasBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CANVAS, pos, state);
        // Инициализируем белым цветом (по умолчанию 16x16)
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0xFFFFFF; // Белый
        }
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
    }

    public boolean isEditable() { return editable; }
    public void setEditable(boolean editable) {
        this.editable = editable;
        markDirty();
    }

    // Мультиблочные методы
    public boolean isMaster() { return isMaster; }
    public void setMaster(boolean master) {
        this.isMaster = master;
        markDirty();
    }

    public BlockPos getMasterPos() { return masterPos; }
    public void setMasterPos(BlockPos pos) {
        this.masterPos = pos;
        markDirty();
    }

    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }
    public void setOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
        markDirty();
    }

    // Получить главный BlockEntity (для зависимых блоков)
    @Nullable
    public CanvasBlockEntity getMasterEntity() {
        if (isMaster) return this;
        if (masterPos == null || world == null) return null;
        BlockEntity be = world.getBlockEntity(masterPos);
        if (be instanceof CanvasBlockEntity master) {
            return master;
        }
        return null;
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putIntArray("pixels", pixels);
        view.putInt("canvasWidth", canvasWidth);
        view.putInt("canvasHeight", canvasHeight);
        view.putBoolean("editable", editable);
        view.putBoolean("sizeChosen", sizeChosen);

        // Мультиблочные данные
        view.putBoolean("isMaster", isMaster);
        view.putInt("offsetX", offsetX);
        view.putInt("offsetY", offsetY);
        if (masterPos != null) {
            view.putInt("masterX", masterPos.getX());
            view.putInt("masterY", masterPos.getY());
            view.putInt("masterZ", masterPos.getZ());
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);

        // Сначала читаем размеры
        canvasWidth = view.getInt("canvasWidth", 1);
        canvasHeight = view.getInt("canvasHeight", 1);
        editable = view.getBoolean("editable", true);
        sizeChosen = view.getBoolean("sizeChosen", false);

        // Мультиблочные данные
        isMaster = view.getBoolean("isMaster", true);
        offsetX = view.getInt("offsetX", 0);
        offsetY = view.getInt("offsetY", 0);
        // Проверяем наличие masterPos через Optional
        var optMasterX = view.getOptionalInt("masterX");
        if (optMasterX.isPresent()) {
            int mx = optMasterX.get();
            int my = view.getInt("masterY", 0);
            int mz = view.getInt("masterZ", 0);
            masterPos = new BlockPos(mx, my, mz);
        } else {
            masterPos = null;
        }

        if (canvasWidth < 1) canvasWidth = 1;
        if (canvasHeight < 1) canvasHeight = 1;
        if (canvasWidth > 6) canvasWidth = 6;
        if (canvasHeight > 6) canvasHeight = 6;

        int expectedSize = canvasWidth * 16 * canvasHeight * 16;

        boolean hadPixels = view.getOptionalIntArray("pixels").isPresent();
        view.getOptionalIntArray("pixels").ifPresent(loadedPixels -> {
            if (loadedPixels.length == expectedSize) {
                pixels = loadedPixels;
            } else {
                // Пересоздаём массив правильного размера
                pixels = new int[expectedSize];
                for (int i = 0; i < expectedSize; i++) {
                    pixels[i] = 0xFFFFFF;
                }
            }
        });

        if (!hadPixels) {
            pixels = new int[expectedSize];
            for (int i = 0; i < expectedSize; i++) {
                pixels[i] = 0xFFFFFF;
            }
        }
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbtWithIdentifyingData(registries);
    }

    @Override
    public void writeFullData(WriteView view) {
        super.writeFullData(view);
        view.putIntArray("pixels", pixels);
        view.putInt("canvasWidth", canvasWidth);
        view.putInt("canvasHeight", canvasHeight);
        view.putBoolean("editable", editable);
        view.putBoolean("sizeChosen", sizeChosen);

        // Мультиблочные данные
        view.putBoolean("isMaster", isMaster);
        view.putInt("offsetX", offsetX);
        view.putInt("offsetY", offsetY);
        if (masterPos != null) {
            view.putInt("masterX", masterPos.getX());
            view.putInt("masterY", masterPos.getY());
            view.putInt("masterZ", masterPos.getZ());
        }
    }
}
