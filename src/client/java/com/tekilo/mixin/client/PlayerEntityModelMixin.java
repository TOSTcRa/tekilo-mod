package com.tekilo.mixin.client;

import com.tekilo.animation.AnimationData;
import com.tekilo.animation.BoneAnimation;
import com.tekilo.animation.PlayerEntityRenderStateExt;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin для применения кастомных анимаций к модели игрока
 */
@Mixin(PlayerEntityModel.class)
public class PlayerEntityModelMixin {
    // Храним оригинальные позиции частей тела для правильного применения анимаций
    private final java.util.Map<ModelPart, float[]> originalPositions = new java.util.HashMap<>();

    // Поля для рефлексии
    private static volatile java.lang.reflect.Field xField = null;
    private static volatile java.lang.reflect.Field yField = null;
    private static volatile java.lang.reflect.Field zField = null;
    private static volatile boolean fieldsInitialized = false;
    private static final Object initLock = new Object();

    /**
     * Перехватываем setAngles и применяем кастомную анимацию после стандартной
     */
    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)V",
            at = @At("RETURN"))
    private void onSetAngles(PlayerEntityRenderState state, CallbackInfo ci) {
        if (state == null) {
            return;
        }

        try {
            PlayerEntityRenderStateExt stateExt = (PlayerEntityRenderStateExt) state;
            AnimationData animation = stateExt.tekilo$getAnimationData();

            if (animation == null) {
                return;
            }

            float time = stateExt.tekilo$getAnimationTime();
            PlayerEntityModel model = (PlayerEntityModel) (Object) this;

            // Debug: печатаем время анимации каждые 20 кадров
            if (System.currentTimeMillis() % 1000 < 50) {
                System.out.println("[TekiloMod] Applying animation at time: " + time + "s");
            }

            // Применяем анимацию для каждой части тела
            applyBoneAnimation(model.head, animation.getBoneAnimation("head"), time, "head");
            applyBoneAnimation(model.body, animation.getBoneAnimation("body"), time, "body");
            applyBoneAnimation(model.rightArm, animation.getBoneAnimation("rightArm"), time, "rightArm");
            applyBoneAnimation(model.leftArm, animation.getBoneAnimation("leftArm"), time, "leftArm");
            applyBoneAnimation(model.rightLeg, animation.getBoneAnimation("rightLeg"), time, "rightLeg");
            applyBoneAnimation(model.leftLeg, animation.getBoneAnimation("leftLeg"), time, "leftLeg");
        } catch (Exception e) {
            // Не ломаем рендеринг, если что-то пошло не так
            System.err.println("[TekiloMod] Error in setAngles: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Инициализирует доступ к полям позиции через рефлексию
     */
    private static void initFields() {
        if (fieldsInitialized) {
            return;
        }

        synchronized (initLock) {
            // Double-check locking
            if (fieldsInitialized) {
                return;
            }

            try {
                // Пробуем разные варианты имен полей (для разных версий Yarn/Mojmap)
                String[][] fieldNames = {
                    {"pivotX", "pivotY", "pivotZ"},  // Yarn mappings
                    {"x", "y", "z"},                  // Возможные старые имена
                    {"xRot", "yRot", "zRot"}          // Другой вариант
                };

                java.lang.reflect.Field tempX = null;
                java.lang.reflect.Field tempY = null;
                java.lang.reflect.Field tempZ = null;

                for (String[] names : fieldNames) {
                    try {
                        tempX = ModelPart.class.getDeclaredField(names[0]);
                        tempY = ModelPart.class.getDeclaredField(names[1]);
                        tempZ = ModelPart.class.getDeclaredField(names[2]);

                        tempX.setAccessible(true);
                        tempY.setAccessible(true);
                        tempZ.setAccessible(true);

                        // Only assign if all three fields found successfully
                        xField = tempX;
                        yField = tempY;
                        zField = tempZ;

                        System.out.println("[TekiloMod] Found ModelPart position fields: " + names[0] + ", " + names[1] + ", " + names[2]);
                        break;
                    } catch (NoSuchFieldException ignored) {
                        // Try next set of field names
                    }
                }

                if (xField == null) {
                    System.err.println("[TekiloMod] Could not find position fields in ModelPart!");
                    System.err.println("[TekiloMod] Available fields:");
                    for (java.lang.reflect.Field f : ModelPart.class.getDeclaredFields()) {
                        System.err.println("  - " + f.getName() + " : " + f.getType().getSimpleName());
                    }
                }
            } catch (Exception e) {
                System.err.println("[TekiloMod] Error initializing fields: " + e.getMessage());
                e.printStackTrace();
            }

            fieldsInitialized = true;
        }
    }

    /**
     * Устанавливает позицию ModelPart через рефлексию
     */
    private static void setPosition(ModelPart part, float x, float y, float z) {
        if (!fieldsInitialized) {
            initFields();
        }

        try {
            if (xField != null) xField.setFloat(part, x);
            if (yField != null) yField.setFloat(part, y);
            if (zField != null) zField.setFloat(part, z);
        } catch (IllegalAccessException e) {
            System.err.println("[TekiloMod] Error setting position: " + e.getMessage());
        }
    }

    /**
     * Получает позицию ModelPart через рефлексию
     */
    private static float[] getPosition(ModelPart part) {
        if (!fieldsInitialized) {
            initFields();
        }

        try {
            float x = xField != null ? xField.getFloat(part) : 0;
            float y = yField != null ? yField.getFloat(part) : 0;
            float z = zField != null ? zField.getFloat(part) : 0;
            return new float[]{x, y, z};
        } catch (IllegalAccessException e) {
            System.err.println("[TekiloMod] Error getting position: " + e.getMessage());
            return new float[]{0, 0, 0};
        }
    }

    /**
     * Применяет анимацию к одной части тела (ModelPart)
     */
    private void applyBoneAnimation(ModelPart part, BoneAnimation boneAnim, float time, String boneName) {
        if (part == null || boneAnim == null) {
            return;
        }

        try {
            // Получаем трансформации на текущем времени
            BoneAnimation.Transform rotation = boneAnim.getRotationAt(time);
            BoneAnimation.Transform position = boneAnim.getPositionAt(time);

            // Проверяем есть ли трансформации
            boolean hasRotation = rotation != null && (rotation.x != 0 || rotation.y != 0 || rotation.z != 0);
            boolean hasPosition = position != null && (position.x != 0 || position.y != 0 || position.z != 0);

            if (hasRotation) {
                // Применяем вращение (в радианах)
                part.pitch = rotation.x;
                part.yaw = rotation.y;
                part.roll = rotation.z;
            }

            if (hasPosition) {
                // Сохраняем оригинальную позицию при первом применении
                if (!originalPositions.containsKey(part)) {
                    float[] origPos = getPosition(part);
                    originalPositions.put(part, new float[]{origPos[0], origPos[1], origPos[2]});
                }

                // Применяем позицию через accessor
                // В Bedrock формате позиции в пикселях (16 пикселей = 1 блок)
                float scale = 1.0f / 16.0f;

                // Получаем ОРИГИНАЛЬНУЮ позицию (не текущую!)
                float[] origPos = originalPositions.get(part);

                // Применяем офсет анимации относительно оригинала
                float newX = origPos[0] + position.x * scale;
                float newY = origPos[1] + position.y * scale;
                float newZ = origPos[2] + position.z * scale;

                setPosition(part, newX, newY, newZ);
            }
        } catch (Exception e) {
            // Игнорируем ошибки анимации
            System.err.println("[TekiloMod] Error applying bone animation: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
