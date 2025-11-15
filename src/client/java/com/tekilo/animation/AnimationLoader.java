package com.tekilo.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Загрузчик анимаций из JSON файлов
 */
public class AnimationLoader {
    private static final Map<String, AnimationData> ANIMATIONS = new HashMap<>();

    /**
     * Маппинг имен костей из Bedrock/GeckoLib формата в Minecraft формат
     */
    private static final Map<String, String> BONE_NAME_MAPPING;

    static {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("torso", "body");
        mapping.put("body", "body");
        mapping.put("head", "head");
        mapping.put("right_arm", "rightArm");
        mapping.put("rightArm", "rightArm");
        mapping.put("left_arm", "leftArm");
        mapping.put("leftArm", "leftArm");
        mapping.put("right_leg", "rightLeg");
        mapping.put("rightLeg", "rightLeg");
        mapping.put("left_leg", "leftLeg");
        mapping.put("leftLeg", "leftLeg");
        BONE_NAME_MAPPING = Map.copyOf(mapping);
    }

    /**
     * Загружает анимацию из JSON файла
     */
    public static AnimationData loadAnimation(ResourceManager resourceManager, Identifier animationId) {
        if (ANIMATIONS.containsKey(animationId.toString())) {
            return ANIMATIONS.get(animationId.toString());
        }

        try {
            var resource = resourceManager.getResource(animationId);
            if (resource.isEmpty()) {
                System.err.println("[TekiloMod] Animation file not found: " + animationId);
                return null;
            }

            try (var reader = new InputStreamReader(resource.get().getInputStream())) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                AnimationData data = parseAnimation(root, animationId.getPath());

                if (data != null) {
                    ANIMATIONS.put(animationId.toString(), data);
                    System.out.println("[TekiloMod] Loaded animation: " + animationId);
                }

                return data;
            }
        } catch (Exception e) {
            System.err.println("[TekiloMod] Failed to load animation: " + animationId);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Парсит JSON в AnimationData
     * Поддерживает два формата:
     * 1. Bedrock/GeckoLib формат (.animation.json) с полем "animations"
     * 2. Emotecraft формат (.json) с полем "emote"
     */
    private static AnimationData parseAnimation(JsonObject root, String name) {
        try {
            // Проверяем, какой формат используется
            if (root.has("animations")) {
                // Формат Bedrock/GeckoLib
                return parseBedrockFormat(root, name);
            } else if (root.has("emote")) {
                // Формат Emotecraft
                return parseEmotecraftFormat(root, name);
            } else {
                System.err.println("[TekiloMod] Unknown animation format: " + name);
                return null;
            }
        } catch (Exception e) {
            System.err.println("[TekiloMod] Failed to parse animation: " + name);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Парсит анимацию в формате Bedrock/GeckoLib
     */
    private static AnimationData parseBedrockFormat(JsonObject root, String name) {
        JsonObject animations = root.getAsJsonObject("animations");
        if (animations == null) {
            return null;
        }

        // Берем первую анимацию из файла
        String firstAnimationName = animations.keySet().iterator().next();
        JsonObject animation = animations.getAsJsonObject(firstAnimationName);

        float animationLength = animation.get("animation_length").getAsFloat();
        AnimationData data = new AnimationData(name, animationLength);

        JsonObject bones = animation.getAsJsonObject("bones");
        if (bones != null) {
            for (String boneName : bones.keySet()) {
                BoneAnimation boneAnim = parseBone(bones.getAsJsonObject(boneName));
                // Маппим имя кости из Bedrock формата в Minecraft формат
                String minecraftBoneName = BONE_NAME_MAPPING.getOrDefault(boneName, boneName);
                data.addBoneAnimation(minecraftBoneName, boneAnim);
            }
        }

        return data;
    }

    /**
     * Парсит анимацию в формате Emotecraft
     */
    private static AnimationData parseEmotecraftFormat(JsonObject root, String name) {
        JsonObject emote = root.getAsJsonObject("emote");
        if (emote == null) {
            return null;
        }

        // Вычисляем длину анимации из stopTick
        int stopTick = emote.get("stopTick").getAsInt();
        float animationLength = stopTick / 20.0f; // тики в секунды

        AnimationData data = new AnimationData(name, animationLength);

        JsonArray moves = emote.getAsJsonArray("moves");
        if (moves != null) {
            parseEmotecraftMoves(moves, data);
        }

        return data;
    }

    /**
     * Парсит массив moves из формата Emotecraft
     */
    private static void parseEmotecraftMoves(JsonArray moves, AnimationData data) {
        // Группируем moves по костям
        Map<String, Map<Integer, JsonObject>> boneKeyframes = new HashMap<>();

        for (JsonElement moveElement : moves) {
            JsonObject move = moveElement.getAsJsonObject();
            int tick = move.get("tick").getAsInt();
            float time = tick / 20.0f; // тики в секунды

            // Проверяем каждую возможную кость
            for (String boneName : new String[]{"torso", "head", "leftArm", "rightArm", "leftLeg", "rightLeg"}) {
                if (move.has(boneName)) {
                    boneKeyframes.computeIfAbsent(boneName, k -> new TreeMap<>())
                        .put(tick, move.getAsJsonObject(boneName));
                }
            }
        }

        // Создаем BoneAnimation для каждой кости
        for (Map.Entry<String, Map<Integer, JsonObject>> entry : boneKeyframes.entrySet()) {
            String boneName = entry.getKey();
            Map<Integer, JsonObject> keyframes = entry.getValue();

            BoneAnimation boneAnim = new BoneAnimation();

            for (Map.Entry<Integer, JsonObject> kf : keyframes.entrySet()) {
                float time = kf.getKey() / 20.0f;
                JsonObject transform = kf.getValue();

                // Позиция (если есть x, y, z)
                if (transform.has("x") || transform.has("y") || transform.has("z")) {
                    float x = transform.has("x") ? transform.get("x").getAsFloat() : 0;
                    float y = transform.has("y") ? transform.get("y").getAsFloat() : 0;
                    float z = transform.has("z") ? transform.get("z").getAsFloat() : 0;
                    boneAnim.addPositionKeyframe(time, x, y, z);
                }

                // Вращение (pitch, yaw, roll в радианах уже)
                if (transform.has("pitch") || transform.has("yaw") || transform.has("roll")) {
                    float pitch = transform.has("pitch") ? transform.get("pitch").getAsFloat() : 0;
                    float yaw = transform.has("yaw") ? transform.get("yaw").getAsFloat() : 0;
                    float roll = transform.has("roll") ? transform.get("roll").getAsFloat() : 0;
                    // В Emotecraft формате углы уже в радианах, поэтому используем напрямую
                    boneAnim.addRotationKeyframe(time,
                        (float) Math.toDegrees(pitch),
                        (float) Math.toDegrees(yaw),
                        (float) Math.toDegrees(roll));
                }
            }

            // Маппим имя кости
            String minecraftBoneName = BONE_NAME_MAPPING.getOrDefault(boneName, boneName);
            data.addBoneAnimation(minecraftBoneName, boneAnim);
        }
    }

    /**
     * Парсит анимацию одной кости
     */
    private static BoneAnimation parseBone(JsonObject boneData) {
        BoneAnimation boneAnim = new BoneAnimation();

        // Парсим позицию
        if (boneData.has("position")) {
            JsonObject position = boneData.getAsJsonObject("position");
            for (String timeStr : position.keySet()) {
                float time = Float.parseFloat(timeStr);
                JsonObject keyframe = position.getAsJsonObject(timeStr);
                JsonArray vector = keyframe.getAsJsonArray("vector");

                boneAnim.addPositionKeyframe(
                    time,
                    vector.get(0).getAsFloat(),
                    vector.get(1).getAsFloat(),
                    vector.get(2).getAsFloat()
                );
            }
        }

        // Парсим вращение
        if (boneData.has("rotation")) {
            JsonObject rotation = boneData.getAsJsonObject("rotation");
            for (String timeStr : rotation.keySet()) {
                float time = Float.parseFloat(timeStr);
                JsonObject keyframe = rotation.getAsJsonObject(timeStr);
                JsonArray vector = keyframe.getAsJsonArray("vector");

                boneAnim.addRotationKeyframe(
                    time,
                    vector.get(0).getAsFloat(),
                    vector.get(1).getAsFloat(),
                    vector.get(2).getAsFloat()
                );
            }
        }

        return boneAnim;
    }

    /**
     * Получить загруженную анимацию
     */
    public static AnimationData getAnimation(String id) {
        return ANIMATIONS.get(id);
    }

    /**
     * Очистить кеш анимаций
     */
    public static void clear() {
        ANIMATIONS.clear();
    }
}
