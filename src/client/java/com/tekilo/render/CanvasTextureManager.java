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
import java.util.Arrays;

@Environment(EnvType.CLIENT)
public class CanvasTextureManager {
    private static CanvasTextureManager INSTANCE;

    private final TextureManager textureManager;
    private final Map<BlockPos, NativeImageBackedTexture> textures = new ConcurrentHashMap<>();
    private final Map<BlockPos, Identifier> textureIds = new ConcurrentHashMap<>();
    private final Map<BlockPos, Integer> lastPixelHash = new ConcurrentHashMap<>();

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

        // Prevent division by zero
        if (pixelWidth == 0) {
            System.err.println("[TekiloMod] Invalid pixel array size: " + totalPixels);
            return null;
        }

        int pixelHeight = totalPixels / pixelWidth;

        if (pixelWidth * pixelHeight != totalPixels || pixelWidth > 96 || pixelHeight > 96) {
            System.err.println("[TekiloMod] Invalid canvas dimensions: " + pixelWidth + "x" + pixelHeight);
            return null;
        }

        // Check if we already have this texture and if pixels changed
        if (textureIds.containsKey(pos)) {
            Integer lastHash = lastPixelHash.get(pos);
            int currentHash = Arrays.hashCode(pixels);
            if (lastHash != null && lastHash == currentHash) {
                // Pixels haven't changed (hash match), return cached texture
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

        // Cache the pixel data hash instead of cloning entire array
        lastPixelHash.put(pos, Arrays.hashCode(pixels));

        return textureId;
    }

    public void removeTexture(BlockPos pos) {
        NativeImageBackedTexture texture = textures.remove(pos);
        Identifier textureId = textureIds.remove(pos);
        lastPixelHash.remove(pos);

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
        lastPixelHash.clear();
    }

    public boolean hasTexture(BlockPos pos) {
        return textureIds.containsKey(pos);
    }

    public Identifier getTextureId(BlockPos pos) {
        return textureIds.get(pos);
    }
}
