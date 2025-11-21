#!/usr/bin/env python3
"""
Интерактивный помощник для добавления новых блоков и предметов в Tekilo мод
"""

import sys
import json
from pathlib import Path
from generate_json_models import create_full_block, create_full_item


def main():
    print("🎮 Добавление нового ассета в Tekilo мод")
    print("=" * 40)

    # Выбор типа
    print("\nЧто ты хочешь добавить?")
    print("1. Блок")
    print("2. Предмет")
    choice = input("\nВыбор (1 или 2): ").strip()

    if choice == "1":
        # Добавление блока
        name = input("\nВведи название блока (например, magic_stone): ").strip()
        if not name:
            print("❌ Название не может быть пустым!")
            return

        print("\nКакой тип текстур?")
        print("1. Одна текстура на все стороны (по умолчанию)")
        print("2. Колонна (разные текстуры сверху/снизу и по бокам)")
        print("3. Ориентируемый (как печка, с фронтальной стороной)")
        texture_type = input("\nВыбор (1-3, Enter для 1): ").strip()

        texture_map = {"1": "all", "2": "column", "3": "orientable", "": "all"}
        texture_type = texture_map.get(texture_type, "all")

        print(f"\n🔨 Создаю файлы для блока '{name}'...")
        create_full_block(name, texture_type)

        print(f"\n✅ Готово! Теперь добавь текстуру(ы):")
        if texture_type == "all":
            print(f"  • src/main/resources/assets/tekilo/textures/block/{name}.png")
        elif texture_type == "column":
            print(f"  • src/main/resources/assets/tekilo/textures/block/{name}.png (боковая)")
            print(f"  • src/main/resources/assets/tekilo/textures/block/{name}_top.png (верх/низ)")
        elif texture_type == "orientable":
            print(f"  • src/main/resources/assets/tekilo/textures/block/{name}.png (боковая)")
            print(f"  • src/main/resources/assets/tekilo/textures/block/{name}_top.png (верх)")
            print(f"  • src/main/resources/assets/tekilo/textures/block/{name}_front.png (передняя)")

    elif choice == "2":
        # Добавление предмета
        name = input("\nВведи название предмета (например, magic_wand): ").strip()
        if not name:
            print("❌ Название не может быть пустым!")
            return

        print(f"\n🔨 Создаю файлы для предмета '{name}'...")
        create_full_item(name)

        print(f"\n✅ Готово! Теперь добавь текстуру:")
        print(f"  • src/main/resources/assets/tekilo/textures/item/{name}.png")

    else:
        print("❌ Неверный выбор!")
        return

    print("\n💡 Не забудь:")
    print(f"  1. Зарегистрировать {'блок в ModBlocks.java' if choice == '1' else 'предмет в ModItems.java'}")
    print("  2. Добавить перевод в lang/*.json файлы")
    print("  3. Добавить рецепт крафта в data/tekilo/recipe/")
    print("  4. Запустить ./gradlew runClient для проверки")


if __name__ == "__main__":
    main()