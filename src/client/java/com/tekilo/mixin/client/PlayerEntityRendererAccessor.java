package com.tekilo.mixin.client;

import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor для получения доступа к полю model в PlayerEntityRenderer
 */
@Mixin(PlayerEntityRenderer.class)
public interface PlayerEntityRendererAccessor {
    @Accessor("model")
    PlayerEntityModel getPlayerModel();
}
