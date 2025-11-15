package com.tekilo.animation;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * Resource reload listener для автоматической загрузки анимаций
 */
public class AnimationResourceLoader implements SimpleSynchronousResourceReloadListener {
    private static final Identifier ID = Identifier.of("tekilo", "animations");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager manager) {
        // Очищаем кеш при перезагрузке ресурсов
        AnimationLoader.clear();

        // Загружаем все .animation.json файлы из animations/
        loadAnimationsFromDirectory(manager, "animations", ".animation.json");

        // Загружаем все .json файлы из player_animations/ (формат Emotecraft)
        loadAnimationsFromDirectory(manager, "player_animations", ".json");
    }

    /**
     * Загружает анимации из указанной директории
     */
    private void loadAnimationsFromDirectory(ResourceManager manager, String directory, String extension) {
        Map<Identifier, net.minecraft.resource.Resource> resources = manager.findResources(
            directory,
            id -> id.getPath().endsWith(extension)
        );

        for (Identifier resourceId : resources.keySet()) {
            try {
                // Создаем полный путь к ресурсу
                String namespace = resourceId.getNamespace();
                String path = resourceId.getPath();

                // Загружаем анимацию
                Identifier animationId = Identifier.of(namespace, path);
                AnimationLoader.loadAnimation(manager, animationId);

                System.out.println("[TekiloMod] Pre-loaded animation: " + animationId);
            } catch (Exception e) {
                System.err.println("[TekiloMod] Failed to pre-load animation: " + resourceId);
                e.printStackTrace();
            }
        }
    }
}
