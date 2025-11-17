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
            System.out.println("[TekiloMod] CanvasBlockEntityRenderer.updateRenderState called! pos=" + entity.getPos() + ", hasContent=" + hasContent + ", size=" + state.canvasWidth + "x" + state.canvasHeight);
            updateLoggedOnce = true;
        }

        // Создаём или обновляем динамическую текстуру
        int expectedSize = state.canvasWidth * 16 * state.canvasHeight * 16;
        if (state.pixels != null && state.pixels.length == expectedSize && state.pos != null) {
            state.textureId = CanvasTextureManager.getInstance().getOrCreateTexture(state.pos, state.pixels);
        } else {
            state.textureId = null;
        }
    }

    private static boolean loggedOnce = false;

    @Override
    public void render(CanvasBlockEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        try {
            int pixelWidth = state.canvasWidth * 16;
            int pixelHeight = state.canvasHeight * 16;
            int expectedSize = pixelWidth * pixelHeight;

            if (state.pixels == null || state.pixels.length != expectedSize) {
                return;
            }
            if (state.facing == null) {
                return;
            }
            if (state.textureId == null) {
                return;
            }

            // Debug: логируем один раз
            if (!loggedOnce) {
                boolean hasContent = false;
                for (int pixel : state.pixels) {
                    if (pixel != 0xFFFFFF) {
                        hasContent = true;
                        break;
                    }
                }
                System.out.println("[TekiloMod] CanvasBlockEntityRenderer.render called! pos=" + state.pos + ", hasContent=" + hasContent + ", facing=" + state.facing + ", size=" + state.canvasWidth + "x" + state.canvasHeight + ", textureId=" + state.textureId);
                loggedOnce = true;
            }

            final Direction facing = state.facing;
            final int light = state.lightmapCoordinates;
            final Identifier textureId = state.textureId;
            final int cWidth = state.canvasWidth;
            final int cHeight = state.canvasHeight;

            // Используем динамическую текстуру вместо отдельных квадов для каждого пикселя
            RenderLayer textureLayer = RenderLayer.getEntitySolid(textureId);

            // Один квад для всей текстуры - ОГРОМНАЯ оптимизация!
            queue.submitCustom(matrices, textureLayer, (entry, consumer) -> {
                renderCanvasQuad(consumer, entry, facing, light, cWidth, cHeight);
            });
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

        // VoxelShape блока тонкий (1/16) и прилегает к стене
        // Картина расширяется от блока в зависимости от направления
        switch (facing) {
            case NORTH -> {
                nz = -1;
                float zPos = 15.0f / 16.0f - offset;
                // Картина растет влево (-X) и вверх (+Y)
                x1 = 1; y1 = h; z1 = zPos;
                x2 = 1; y2 = 0; z2 = zPos;
                x3 = 1 - w; y3 = 0; z3 = zPos;
                x4 = 1 - w; y4 = h; z4 = zPos;
            }
            case SOUTH -> {
                nz = 1;
                float zPos = 1.0f / 16.0f + offset;
                // Картина растет вправо (+X) и вверх (+Y)
                x1 = 0; y1 = h; z1 = zPos;
                x2 = 0; y2 = 0; z2 = zPos;
                x3 = w; y3 = 0; z3 = zPos;
                x4 = w; y4 = h; z4 = zPos;
            }
            case WEST -> {
                nx = -1;
                float xPos = 15.0f / 16.0f - offset;
                // Картина растет назад (-Z) и вверх (+Y)
                x1 = xPos; y1 = h; z1 = 1;
                x2 = xPos; y2 = 0; z2 = 1;
                x3 = xPos; y3 = 0; z3 = 1 - w;
                x4 = xPos; y4 = h; z4 = 1 - w;
            }
            case EAST -> {
                nx = 1;
                float xPos = 1.0f / 16.0f + offset;
                // Картина растет вперед (+Z) и вверх (+Y)
                x1 = xPos; y1 = h; z1 = 0;
                x2 = xPos; y2 = 0; z2 = 0;
                x3 = xPos; y3 = 0; z3 = w;
                x4 = xPos; y4 = h; z4 = w;
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
}


