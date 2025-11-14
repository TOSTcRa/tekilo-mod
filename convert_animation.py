#!/usr/bin/env python3
"""
Конвертер анимаций из формата GeckoLib в формат Emotecraft/PlayerAnimator
Использование: python3 convert_animation.py input.animation.json output.json
"""

import json
import sys
from collections import defaultdict

def convert_geckolib_to_emotecraft(geckolib_data, animation_name="custom_animation", author="unknown"):
    """Конвертирует GeckoLib анимацию в формат Emotecraft"""

    if "animations" not in geckolib_data:
        raise ValueError("Неверный формат GeckoLib: отсутствует ключ 'animations'")

    # Берем первую анимацию
    first_anim_name = list(geckolib_data["animations"].keys())[0]
    geckolib_anim = geckolib_data["animations"][first_anim_name]

    # Получаем длительность анимации в секундах
    animation_length = geckolib_anim.get("animation_length", 1.0)

    # Конвертируем в тики (1 секунда = 20 тиков)
    total_ticks = int(animation_length * 20)

    # Собираем все keyframes
    all_keyframes = defaultdict(lambda: defaultdict(dict))

    bones = geckolib_anim.get("bones", {})

    # Маппинг костей GeckoLib -> Emotecraft
    bone_mapping = {
        "torso": "torso",
        "head": "head",
        "right_arm": "rightArm",
        "left_arm": "leftArm",
        "right_leg": "rightLeg",
        "left_leg": "leftLeg"
    }

    for bone_name, bone_data in bones.items():
        if bone_name not in bone_mapping:
            print(f"Предупреждение: кость '{bone_name}' не поддерживается, пропущена")
            continue

        emotecraft_bone = bone_mapping[bone_name]

        # Обрабатываем позицию
        if "position" in bone_data:
            for time_str, pos_data in bone_data["position"].items():
                time_seconds = float(time_str)
                tick = int(time_seconds * 20)
                vector = pos_data["vector"]

                if not all_keyframes[tick][emotecraft_bone]:
                    all_keyframes[tick][emotecraft_bone] = {}

                # В Emotecraft: x, y, z
                if vector[0] != 0:
                    all_keyframes[tick][emotecraft_bone]["x"] = vector[0]
                if vector[1] != 0:
                    all_keyframes[tick][emotecraft_bone]["y"] = vector[1]
                if vector[2] != 0:
                    all_keyframes[tick][emotecraft_bone]["z"] = vector[2]

        # Обрабатываем вращение
        if "rotation" in bone_data:
            for time_str, rot_data in bone_data["rotation"].items():
                time_seconds = float(time_str)
                tick = int(time_seconds * 20)
                vector = rot_data["vector"]

                if not all_keyframes[tick][emotecraft_bone]:
                    all_keyframes[tick][emotecraft_bone] = {}

                # В Emotecraft: pitch (X), yaw (Y), roll (Z)
                # GeckoLib использует градусы, конвертируем в радианы
                import math
                if vector[0] != 0:
                    all_keyframes[tick][emotecraft_bone]["pitch"] = math.radians(vector[0])
                if vector[1] != 0:
                    all_keyframes[tick][emotecraft_bone]["yaw"] = math.radians(vector[1])
                if vector[2] != 0:
                    all_keyframes[tick][emotecraft_bone]["roll"] = math.radians(vector[2])

    # Формируем массив moves
    moves = []
    sorted_ticks = sorted(all_keyframes.keys())

    for tick in sorted_ticks:
        for bone, transforms in all_keyframes[tick].items():
            move = {
                "tick": tick,
                "easing": "EASEINOUTQUAD" if tick > 0 else "LINEAR",
                "turn": 0,
                bone: transforms
            }
            moves.append(move)

    # Создаем структуру Emotecraft
    emotecraft_data = {
        "name": animation_name,
        "author": author,
        "description": f"Converted from GeckoLib animation '{first_anim_name}'",
        "emote": {
            "isLoop": "false",
            "returnTick": 10,
            "beginTick": 0,
            "endTick": max(10, total_ticks // 4),
            "stopTick": total_ticks,
            "degrees": False,  # Используем радианы
            "moves": moves
        }
    }

    return emotecraft_data


def main():
    if len(sys.argv) < 2:
        print("Использование: python3 convert_animation.py input.animation.json [output.json] [name] [author]")
        sys.exit(1)

    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 else "converted_animation.json"
    animation_name = sys.argv[3] if len(sys.argv) > 3 else "converted_animation"
    author = sys.argv[4] if len(sys.argv) > 4 else "tekilo"

    print(f"Чтение файла: {input_file}")
    with open(input_file, 'r', encoding='utf-8') as f:
        geckolib_data = json.load(f)

    print("Конвертация...")
    emotecraft_data = convert_geckolib_to_emotecraft(geckolib_data, animation_name, author)

    print(f"Сохранение в: {output_file}")
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(emotecraft_data, f, indent=4, ensure_ascii=False)

    print("✓ Конвертация завершена успешно!")
    print(f"  Длительность: {emotecraft_data['emote']['stopTick']} тиков")
    print(f"  Keyframes: {len(emotecraft_data['emote']['moves'])}")


if __name__ == "__main__":
    main()
