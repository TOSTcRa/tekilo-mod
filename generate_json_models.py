#!/usr/bin/env python3
"""
Генератор JSON моделей для Tekilo мода
Автоматически создает все необходимые JSON файлы для новых блоков и предметов
"""

import json
import os
from pathlib import Path

# Базовые пути
BASE_DIR = Path("src/main/resources/assets/tekilo")
BLOCKSTATES_DIR = BASE_DIR / "blockstates"
MODELS_BLOCK_DIR = BASE_DIR / "models/block"
MODELS_ITEM_DIR = BASE_DIR / "models/item"
ITEMS_DIR = BASE_DIR / "items"

# Создаем директории если их нет
for dir in [BLOCKSTATES_DIR, MODELS_BLOCK_DIR, MODELS_ITEM_DIR, ITEMS_DIR]:
    dir.mkdir(parents=True, exist_ok=True)


def create_blockstate(block_name):
    """Создает blockstate JSON для блока"""
    content = {
        "variants": {
            "": {"model": f"tekilo:block/{block_name}"}
        }
    }

    file_path = BLOCKSTATES_DIR / f"{block_name}.json"
    with open(file_path, 'w') as f:
        json.dump(content, f, indent=2)
    print(f"✓ Создан blockstate: {file_path}")


def create_block_model(block_name, texture_type="all"):
    """Создает модель блока"""
    if texture_type == "all":
        # Простой куб с одной текстурой на все стороны
        content = {
            "parent": "minecraft:block/cube_all",
            "textures": {
                "all": f"tekilo:block/{block_name}"
            }
        }
    elif texture_type == "column":
        # Колонна с разными текстурами сверху/снизу и по бокам
        content = {
            "parent": "minecraft:block/cube_column",
            "textures": {
                "end": f"tekilo:block/{block_name}_top",
                "side": f"tekilo:block/{block_name}"
            }
        }
    elif texture_type == "orientable":
        # Блок с направлением (как печка)
        content = {
            "parent": "minecraft:block/orientable",
            "textures": {
                "top": f"tekilo:block/{block_name}_top",
                "front": f"tekilo:block/{block_name}_front",
                "side": f"tekilo:block/{block_name}"
            }
        }

    file_path = MODELS_BLOCK_DIR / f"{block_name}.json"
    with open(file_path, 'w') as f:
        json.dump(content, f, indent=2)
    print(f"✓ Создана модель блока: {file_path}")


def create_block_item_model(block_name):
    """Создает модель блока для инвентаря"""
    content = {
        "parent": f"tekilo:block/{block_name}"
    }

    file_path = MODELS_ITEM_DIR / f"{block_name}.json"
    with open(file_path, 'w') as f:
        json.dump(content, f, indent=2)
    print(f"✓ Создана модель блока для инвентаря: {file_path}")


def create_item_model(item_name, parent="minecraft:item/generated"):
    """Создает модель предмета"""
    content = {
        "parent": parent,
        "textures": {
            "layer0": f"tekilo:item/{item_name}"
        }
    }

    # Специальные родители для разных типов предметов
    if "sword" in item_name or "pickaxe" in item_name or "axe" in item_name:
        content["parent"] = "minecraft:item/handheld"
    elif "disc" in item_name:
        content["parent"] = "minecraft:item/generated"

    file_path = MODELS_ITEM_DIR / f"{item_name}.json"
    with open(file_path, 'w') as f:
        json.dump(content, f, indent=2)
    print(f"✓ Создана модель предмета: {file_path}")


def create_item_definition(item_name):
    """Создает определение предмета (новый формат MC 1.21+)"""
    content = {
        "model": {
            "type": "minecraft:model",
            "model": f"tekilo:item/{item_name}"
        }
    }

    file_path = ITEMS_DIR / f"{item_name}.json"
    with open(file_path, 'w') as f:
        json.dump(content, f, indent=2)
    print(f"✓ Создано определение предмета: {file_path}")


def create_full_block(block_name, texture_type="all"):
    """Создает все файлы для блока"""
    print(f"\n📦 Создание файлов для блока: {block_name}")
    create_blockstate(block_name)
    create_block_model(block_name, texture_type)
    create_block_item_model(block_name)
    create_item_definition(block_name)


def create_full_item(item_name):
    """Создает все файлы для предмета"""
    print(f"\n🎮 Создание файлов для предмета: {item_name}")
    create_item_model(item_name)
    create_item_definition(item_name)


def check_missing_files():
    """Проверяет какие файлы отсутствуют для существующих текстур"""
    print("\n🔍 Проверка отсутствующих JSON файлов...")

    # Проверяем блоки
    block_textures = [f.stem for f in (Path("src/main/resources/assets/tekilo/textures/block")).glob("*.png")]
    for block in block_textures:
        missing = []
        if not (BLOCKSTATES_DIR / f"{block}.json").exists():
            missing.append("blockstate")
        if not (MODELS_BLOCK_DIR / f"{block}.json").exists():
            missing.append("block model")
        if not (MODELS_ITEM_DIR / f"{block}.json").exists():
            missing.append("item model")
        if not (ITEMS_DIR / f"{block}.json").exists():
            missing.append("item definition")

        if missing:
            print(f"  ⚠️ {block}: отсутствуют {', '.join(missing)}")

    # Проверяем предметы
    item_textures = [f.stem for f in (Path("src/main/resources/assets/tekilo/textures/item")).glob("*.png")]
    for item in item_textures:
        missing = []
        if not (MODELS_ITEM_DIR / f"{item}.json").exists():
            missing.append("item model")
        if not (ITEMS_DIR / f"{item}.json").exists():
            missing.append("item definition")

        if missing:
            print(f"  ⚠️ {item}: отсутствуют {', '.join(missing)}")


def main():
    """Главная функция"""
    print("🎨 Генератор JSON моделей для Tekilo мода")
    print("=" * 50)

    # Проверяем что отсутствует
    check_missing_files()

    print("\n" + "=" * 50)
    print("📝 Примеры использования:")
    print("\nДля создания всех файлов блока:")
    print('  create_full_block("my_new_block")')
    print("\nДля создания всех файлов предмета:")
    print('  create_full_item("my_new_item")')
    print("\nДля создания блока с разными текстурами:")
    print('  create_full_block("my_column", "column")')
    print('  create_full_block("my_furnace", "orientable")')

    # Если хочешь автоматически создать недостающие файлы, раскомментируй:
    # print("\n🔧 Создание недостающих файлов...")
    # create_full_block("communist_collector")
    # create_full_block("capitalist_collector")
    # create_full_item("joint")
    # create_full_item("spawner_linker")


if __name__ == "__main__":
    main()