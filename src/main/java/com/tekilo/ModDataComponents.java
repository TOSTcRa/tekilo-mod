package com.tekilo;

import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.Arrays;

public class ModDataComponents {
    // Canvas pixels component - stores variable size integers (up to 96x96 grid of RGB colors)
    public static final ComponentType<int[]> CANVAS_PIXELS = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of("tekilo", "canvas_pixels"),
        ComponentType.<int[]>builder()
            .codec(Codec.INT_STREAM.xmap(
                stream -> stream.toArray(),
                array -> Arrays.stream(array)
            ))
            .build()
    );

    // Canvas width in blocks (1-6)
    public static final ComponentType<Integer> CANVAS_WIDTH = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of("tekilo", "canvas_width"),
        ComponentType.<Integer>builder()
            .codec(Codec.INT)
            .build()
    );

    // Canvas height in blocks (1-6)
    public static final ComponentType<Integer> CANVAS_HEIGHT = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        Identifier.of("tekilo", "canvas_height"),
        ComponentType.<Integer>builder()
            .codec(Codec.INT)
            .build()
    );

    public static void initialize() {
        // Data components are registered during class loading
    }
}
