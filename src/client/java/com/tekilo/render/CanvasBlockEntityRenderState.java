package com.tekilo.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

@Environment(EnvType.CLIENT)
public class CanvasBlockEntityRenderState extends BlockEntityRenderState {
    public int[] pixels;
    public Direction facing;
    public boolean editable;
    public Identifier textureId;
    public int canvasWidth = 1; // В блоках
    public int canvasHeight = 1; // В блоках
    public boolean isMaster = true; // Только главный блок рендерит картину
}
