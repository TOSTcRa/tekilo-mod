package com.tekilo;

import dev.kosmx.playerAnim.api.layered.IAnimation;
import dev.kosmx.playerAnim.api.layered.ModifierLayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationAccess;
import net.minecraft.util.Identifier;

public class PlayerAnimatorInit {
    public static final Identifier ANIMATION_LAYER_ID = Identifier.of("tekilo", "animations");

    public static void init() {
        // Регистрация animation layer для каждого игрока
        PlayerAnimationAccess.REGISTER_ANIMATION_EVENT.register((player, animationStack) -> {
            ModifierLayer<IAnimation> layer = new ModifierLayer<>();
            animationStack.addAnimLayer(100, layer);
            PlayerAnimationAccess.getPlayerAssociatedData(player).set(ANIMATION_LAYER_ID, layer);
        });
    }
}
