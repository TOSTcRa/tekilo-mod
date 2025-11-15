package com.tekilo.animation;

import java.util.Map;
import java.util.TreeMap;

/**
 * Анимация для одной кости (головы, руки, ноги и т.д.)
 */
public class BoneAnimation {
    private final TreeMap<Float, Transform> positionKeyframes = new TreeMap<>();
    private final TreeMap<Float, Transform> rotationKeyframes = new TreeMap<>();

    public void addPositionKeyframe(float time, float x, float y, float z) {
        positionKeyframes.put(time, new Transform(x, y, z));
    }

    public void addRotationKeyframe(float time, float x, float y, float z) {
        // Конвертируем градусы в радианы
        rotationKeyframes.put(time, new Transform(
            (float) Math.toRadians(x),
            (float) Math.toRadians(y),
            (float) Math.toRadians(z)
        ));
    }

    /**
     * Получить позицию в указанное время (с интерполяцией)
     */
    public Transform getPositionAt(float time) {
        return interpolate(positionKeyframes, time);
    }

    /**
     * Получить вращение в указанное время (с интерполяцией)
     */
    public Transform getRotationAt(float time) {
        return interpolate(rotationKeyframes, time);
    }

    /**
     * Линейная интерполяция между keyframes
     */
    private Transform interpolate(TreeMap<Float, Transform> keyframes, float time) {
        if (keyframes.isEmpty()) {
            return new Transform(0, 0, 0);
        }

        // Если время точно совпадает с keyframe
        if (keyframes.containsKey(time)) {
            return keyframes.get(time);
        }

        // Находим ближайшие keyframes до и после текущего времени
        Map.Entry<Float, Transform> before = keyframes.floorEntry(time);
        Map.Entry<Float, Transform> after = keyframes.ceilingEntry(time);

        // Если нет keyframe'а до - возвращаем первый
        if (before == null) {
            return after.getValue();
        }

        // Если нет keyframe'а после - возвращаем последний
        if (after == null) {
            return before.getValue();
        }

        // Линейная интерполяция между двумя keyframes
        float beforeTime = before.getKey();
        float afterTime = after.getKey();
        float progress = (time - beforeTime) / (afterTime - beforeTime);

        Transform beforeTransform = before.getValue();
        Transform afterTransform = after.getValue();

        return Transform.lerp(beforeTransform, afterTransform, progress);
    }

    /**
     * Класс для хранения трансформации (позиция или вращение)
     */
    public static class Transform {
        public final float x, y, z;

        public Transform(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        /**
         * Линейная интерполяция между двумя трансформациями
         */
        public static Transform lerp(Transform a, Transform b, float t) {
            return new Transform(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t
            );
        }
    }
}
