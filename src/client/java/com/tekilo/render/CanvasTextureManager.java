package com.tekilo.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Environment(EnvType.CLIENT)
public class CanvasTextureManager {
    private static CanvasTextureManager INSTANCE;

    private final TextureManager textureManager;
    private final Map<BlockPos, NativeImageBackedTexture> textures = new ConcurrentHashMap<>();
    private final Map<BlockPos, Identifier> textureIds = new ConcurrentHashMap<>();
    private final Map<BlockPos, int[]> lastPixelData = new ConcurrentHashMap<>();

    private CanvasTextureManager(TextureManager textureManager) {
        this.textureManager = textureManager;
    }

    public static void initialize() {
        if (INSTANCE == null) {
            INSTANCE = new CanvasTextureManager(MinecraftClient.getInstance().getTextureManager());
        }
    }

    public static CanvasTextureManager getInstance() {
        if (INSTANCE == null) {
            initialize();
        }
        return INSTANCE;
    }

    public Identifier getOrCreateTexture(BlockPos pos, int[] pixels) {
        if (pixels == null || pixels.length == 0) {
            return null;
        }

        // Calculate dimensions from pixel count (must be square multiple of 16)
        int totalPixels = pixels.length;
        int pixelWidth = (int) Math.sqrt(totalPixels);
        int pixelHeight = totalPixels / pixelWidth;

        if (pixelWidth * pixelHeight != totalPixels) {
            return null;
        }

        // Check if we already have this texture and if pixels changed
        if (textureIds.containsKey(pos)) {
            int[] lastPixels = lastPixelData.get(pos);
            if (lastPixels != null && lastPixels.length == pixels.length && !pixelsChanged(lastPixels, pixels)) {
                // Pixels haven't changed, return cached texture
                return textureIds.get(pos);
            }
        }

        // Create or update texture
        NativeImageBackedTexture texture = textures.get(pos);
        Identifier textureId = textureIds.get(pos);

        // Check if texture exists but has wrong size
        if (texture != null && texture.getImage() != null) {
            NativeImage existingImage = texture.getImage();
            if (existingImage.getWidth() != pixelWidth || existingImage.getHeight() != pixelHeight) {
                // Size changed, need to recreate texture
                texture.close();
                textureManager.destroyTexture(textureId);
                textures.remove(pos);
                textureIds.remove(pos);
                texture = null;
                textureId = null;
            }
        }

        if (texture == null) {
            // Create new texture with dynamic size
            String textureName = "canvas_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
            texture = new NativeImageBackedTexture(textureName, pixelWidth, pixelHeight, false);
            textureId = Identifier.of("tekilo", "canvas_dynamic/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());

            textureManager.registerTexture(textureId, texture);
            textures.put(pos, texture);
            textureIds.put(pos, textureId);
        }

        // Update pixel data
        NativeImage image = texture.getImage();
        if (image != null) {
            for (int y = 0; y < pixelHeight; y++) {
                for (int x = 0; x < pixelWidth; x++) {
                    int rgb = pixels[y * pixelWidth + x];
                    // Convert RGB to ABGR (NativeImage format)
                    int r = (rgb >> 16) & 0xFF;
                    int g = (rgb >> 8) & 0xFF;
                    int b = rgb & 0xFF;
                    int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
                    image.setColorArgb(x, y, abgr);
                }
            }
            texture.upload();
        }

        // Cache the pixel data
        lastPixelData.put(pos, pixels.clone());

        return textureId;
    }

    private boolean pixelsChanged(int[] old, int[] newPixels) {
        if (old.length != newPixels.length) return true;
        for (int i = 0; i < old.length; i++) {
            if (old[i] != newPixels[i]) return true;
        }
        return false;
    }

    public void removeTexture(BlockPos pos) {
        NativeImageBackedTexture texture = textures.remove(pos);
        Identifier textureId = textureIds.remove(pos);
        lastPixelData.remove(pos);

        if (texture != null) {
            texture.close();
        }
        if (textureId != null) {
            textureManager.destroyTexture(textureId);
        }
    }

    public void clearAllTextures() {
        for (Map.Entry<BlockPos, NativeImageBackedTexture> entry : textures.entrySet()) {
            entry.getValue().close();
            Identifier id = textureIds.get(entry.getKey());
            if (id != null) {
                textureManager.destroyTexture(id);
            }
        }
        textures.clear();
        textureIds.clear();
        lastPixelData.clear();
    }

    public boolean hasTexture(BlockPos pos) {
        return textureIds.containsKey(pos);
    }

    public Identifier getTextureId(BlockPos pos) {
        return textureIds.get(pos);
    }
}
