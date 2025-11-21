package com.tekilo.render;

import com.tekilo.CanvasBlock;
import com.tekilo.CanvasBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class CanvasBlockEntityRenderer implements BlockEntityRenderer<CanvasBlockEntity, CanvasBlockEntityRenderState> {

    private static final Identifier CANVAS_TEXTURE = Identifier.of("tekilo", "textures/block/canvas.png");

    public CanvasBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public CanvasBlockEntityRenderState createRenderState() {
        return new CanvasBlockEntityRenderState();
    }

    private static boolean updateLoggedOnce = false;

    @Override
    public void updateRenderState(CanvasBlockEntity entity, CanvasBlockEntityRenderState state, float tickDelta, Vec3d cameraPos, ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay) {
        if (entity == null) {
            return;
        }

        // КРИТИЧЕСКИ ВАЖНО: устанавливаем type для поиска рендерера
        state.type = entity.getType();

        // Мультиблочная поддержка - только главный блок рендерит картину
        state.isMaster = entity.isMaster();

        // Копируем данные из BlockEntity в RenderState с null checks
        int[] entityPixels = entity.getPixels();
        if (entityPixels == null) {
            state.pixels = new int[0];
        } else {
            state.pixels = entityPixels.clone();
        }

        if (entity.getCachedState() != null && entity.getCachedState().contains(CanvasBlock.FACING)) {
            state.facing = entity.getCachedState().get(CanvasBlock.FACING);
        } else {
            state.facing = Direction.NORTH;
        }

        state.editable = entity.isEditable();
        state.canvasWidth = entity.getCanvasWidth();
        state.canvasHeight = entity.getCanvasHeight();
        state.blockState = entity.getCachedState();
        state.pos = entity.getPos();
        state.lightmapCoordinates = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        state.crumblingOverlay = crumblingOverlay;

        // Validate canvas dimensions
        if (state.canvasWidth < 1 || state.canvasWidth > 6 || state.canvasHeight < 1 || state.canvasHeight > 6) {
            state.canvasWidth = 1;
            state.canvasHeight = 1;
        }

        // Debug: логируем один раз
        if (!updateLoggedOnce) {
            boolean hasContent = false;
            for (int pixel : state.pixels) {
                if (pixel != 0xFFFFFF) {
                    hasContent = true;
                    break;
                }
            }
            System.out.println("[TekiloMod] CanvasBlockEntityRenderer.updateRenderState called! pos=" + entity.getPos() + ", hasContent=" + hasContent + ", size=" + state.canvasWidth + "x" + state.canvasHeight + ", isMaster=" + state.isMaster);
            updateLoggedOnce = true;
        }

        // Создаём или обновляем динамическую текстуру (только для главного блока)
        if (state.isMaster) {
            int expectedSize = state.canvasWidth * 16 * state.canvasHeight * 16;
            if (state.pixels != null && state.pixels.length == expectedSize && state.pos != null) {
                state.textureId = CanvasTextureManager.getInstance().getOrCreateTexture(state.pos, state.pixels);
            } else {
                state.textureId = null;
            }
        } else {
            state.textureId = null;
        }
    }

    private static boolean loggedOnce = false;

    @Override
    public void render(CanvasBlockEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        try {
            if (state.facing == null) {
                return;
            }

            final Direction facing = state.facing;
            final int light = state.lightmapCoordinates;

            // Debug: логируем один раз
            if (!loggedOnce) {
                System.out.println("[TekiloMod] CanvasBlockEntityRenderer.render called! pos=" + state.pos + ", facing=" + state.facing + ", isMaster=" + state.isMaster + ", textureId=" + state.textureId);
                loggedOnce = true;
            }

            // 1. Рендерим заднюю сторону (canvas.png) для ВСЕХ блоков
            RenderLayer backLayer = RenderLayer.getEntitySolid(CANVAS_TEXTURE);
            queue.submitCustom(matrices, backLayer, (entry, consumer) -> {
                renderBackQuad(consumer, entry, facing, light);
            });

            // 2. Рендерим переднюю сторону с картиной ТОЛЬКО для master блоков
            if (state.isMaster && state.textureId != null) {
                int pixelWidth = state.canvasWidth * 16;
                int pixelHeight = state.canvasHeight * 16;
                int expectedSize = pixelWidth * pixelHeight;

                if (state.pixels != null && state.pixels.length == expectedSize) {
                    final Identifier textureId = state.textureId;
                    final int cWidth = state.canvasWidth;
                    final int cHeight = state.canvasHeight;

                    RenderLayer frontLayer = RenderLayer.getEntitySolid(textureId);
                    queue.submitCustom(matrices, frontLayer, (entry, consumer) -> {
                        renderCanvasQuad(consumer, entry, facing, light, cWidth, cHeight);
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("[TekiloMod] CanvasBlockEntityRenderer.render EXCEPTION: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void renderCanvasQuad(VertexConsumer consumer, MatrixStack.Entry entry, Direction facing, int light, int canvasWidth, int canvasHeight) {
        Matrix4f posMatrix = entry.getPositionMatrix();

        float offset = 0.001f;
        float w = canvasWidth;
        float h = canvasHeight;

        float nx = 0, ny = 0, nz = 0;
        float x1, y1, z1;
        float x2, y2, z2;
        float x3, y3, z3;
        float x4, y4, z4;

        // facing указывает КУДА СМОТРИТ холст
        // Передняя грань рисуется на "передней" стороне thin блока
        // Нормаль направлена туда КУДА смотрит холст
        switch (facing) {
            case SOUTH -> {
                // Холст смотрит на ЮГ, прижат к северу
                // VoxelShape: z=0-1/16, передняя грань на z=1/16
                nz = 1; // Нормаль на юг
                float zPos = 1.0f / 16.0f + offset;
                // Картина растет вправо (WEST для зрителя на юге) и вверх (+Y)
                x1 = 1; y1 = h; z1 = zPos;
                x2 = 1; y2 = 0; z2 = zPos;
                x3 = 1 - w; y3 = 0; z3 = zPos;
                x4 = 1 - w; y4 = h; z4 = zPos;
            }
            case NORTH -> {
                // Холст смотрит на СЕВЕР, прижат к югу
                // VoxelShape: z=15/16-1, передняя грань на z=15/16
                nz = -1; // Нормаль на север
                float zPos = 15.0f / 16.0f - offset;
                // Картина растет вправо (EAST для зрителя на севере) и вверх (+Y)
                x1 = 0; y1 = h; z1 = zPos;
                x2 = 0; y2 = 0; z2 = zPos;
                x3 = w; y3 = 0; z3 = zPos;
                x4 = w; y4 = h; z4 = zPos;
            }
            case EAST -> {
                // Холст смотрит на ВОСТОК, прижат к западу
                // VoxelShape: x=0-1/16, передняя грань на x=1/16
                nx = 1; // Нормаль на восток
                float xPos = 1.0f / 16.0f + offset;
                // Картина растет вправо (SOUTH для зрителя на востоке) и вверх (+Y)
                x1 = xPos; y1 = h; z1 = 0;
                x2 = xPos; y2 = 0; z2 = 0;
                x3 = xPos; y3 = 0; z3 = w;
                x4 = xPos; y4 = h; z4 = w;
            }
            case WEST -> {
                // Холст смотрит на ЗАПАД, прижат к востоку
                // VoxelShape: x=15/16-1, передняя грань на x=15/16
                nx = -1; // Нормаль на запад
                float xPos = 15.0f / 16.0f - offset;
                // Картина растет вправо (NORTH для зрителя на западе) и вверх (+Y)
                x1 = xPos; y1 = h; z1 = 1;
                x2 = xPos; y2 = 0; z2 = 1;
                x3 = xPos; y3 = 0; z3 = 1 - w;
                x4 = xPos; y4 = h; z4 = 1 - w;
            }
            default -> {
                return;
            }
        }

        // Рисуем один квад с текстурой всего холста
        consumer.vertex(posMatrix, x1, y1, z1)
            .color(1.0f, 1.0f, 1.0f, 1.0f)
            .texture(0, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, nx, ny, nz);

        consumer.vertex(posMatrix, x2, y2, z2)
            .color(1.0f, 1.0f, 1.0f, 1.0f)
            .texture(0, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, nx, ny, nz);

        consumer.vertex(posMatrix, x3, y3, z3)
            .color(1.0f, 1.0f, 1.0f, 1.0f)
            .texture(1, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, nx, ny, nz);

        consumer.vertex(posMatrix, x4, y4, z4)
            .color(1.0f, 1.0f, 1.0f, 1.0f)
            .texture(1, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, nx, ny, nz);
    }

    // Рендерит заднюю сторону блока (у стены) с текстурой canvas.png
    private void renderBackQuad(VertexConsumer consumer, MatrixStack.Entry entry, Direction facing, int light) {
        Matrix4f posMatrix = entry.getPositionMatrix();

        float offset = 0.001f;

        float nx = 0, ny = 0, nz = 0;
        float x1, y1, z1;
        float x2, y2, z2;
        float x3, y3, z3;
        float x4, y4, z4;

        // facing указывает КУДА СМОТРИТ холст
        // Задняя сторона смотрит в ПРОТИВОПОЛОЖНОМ направлении (к стене)
        switch (facing) {
            case SOUTH -> {
                // Холст смотрит на юг, задняя сторона смотрит на СЕВЕР
                nz = -1;
                float zPos = 0.0f + offset; // Северный край VoxelShape (z=0-1/16)
                x1 = 0; y1 = 1; z1 = zPos;
                x2 = 0; y2 = 0; z2 = zPos;
                x3 = 1; y3 = 0; z3 = zPos;
                x4 = 1; y4 = 1; z4 = zPos;
            }
            case NORTH -> {
                // Холст смотрит на север, задняя сторона смотрит на ЮГ
                nz = 1;
                float zPos = 1.0f - offset; // Южный край VoxelShape (z=15/16-1)
                x1 = 1; y1 = 1; z1 = zPos;
                x2 = 1; y2 = 0; z2 = zPos;
                x3 = 0; y3 = 0; z3 = zPos;
                x4 = 0; y4 = 1; z4 = zPos;
            }
            case EAST -> {
                // Холст смотрит на восток, задняя сторона смотрит на ЗАПАД
                nx = -1;
                float xPos = 0.0f + offset; // Западный край VoxelShape (x=0-1/16)
                x1 = xPos; y1 = 1; z1 = 1;
                x2 = xPos; y2 = 0; z2 = 1;
                x3 = xPos; y3 = 0; z3 = 0;
                x4 = xPos; y4 = 1; z4 = 0;
            }
            case WEST -> {
                // Холст смотрит на запад, задняя сторона смотрит на ВОСТОК
                nx = 1;
                float xPos = 1.0f - offset; // Восточный край VoxelShape (x=15/16-1)
                x1 = xPos; y1 = 1; z1 = 0;
                x2 = xPos; y2 = 0; z2 = 0;
                x3 = xPos; y3 = 0; z3 = 1;
                x4 = xPos; y4 = 1; z4 = 1;
            }
            default -> {
                return;
            }
        }

        // Рисуем квад с текстурой canvas.png (16x16)
        consumer.vertex(posMatrix, x1, y1, z1)
            .color(1.0f, 1.0f, 1.0f, 1.0f)
            .texture(0, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, nx, ny, nz);

        consumer.vertex(posMatrix, x2, y2, z2)
            .color(1.0f, 1.0f, 1.0f, 1.0f)
            .texture(0, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, nx, ny, nz);

        consumer.vertex(posMatrix, x3, y3, z3)
            .color(1.0f, 1.0f, 1.0f, 1.0f)
            .texture(1, 1)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, nx, ny, nz);

        consumer.vertex(posMatrix, x4, y4, z4)
            .color(1.0f, 1.0f, 1.0f, 1.0f)
            .texture(1, 0)
            .overlay(OverlayTexture.DEFAULT_UV)
            .light(light)
            .normal(entry, nx, ny, nz);
    }
}
