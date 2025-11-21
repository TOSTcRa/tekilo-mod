#!/usr/bin/env python3
"""
Генератор текстур для Tekilo мода
Создает пиксельарт текстуры согласно описаниям из todo.txt
"""

from PIL import Image, ImageDraw, ImageFont
import os
from pathlib import Path

# Создаем директории
BLOCK_DIR = Path("src/main/resources/assets/tekilo/textures/block")
ITEM_DIR = Path("src/main/resources/assets/tekilo/textures/item")
MOB_EFFECT_DIR = Path("src/main/resources/assets/tekilo/textures/mob_effect")
BLOCK_DIR.mkdir(parents=True, exist_ok=True)
ITEM_DIR.mkdir(parents=True, exist_ok=True)
MOB_EFFECT_DIR.mkdir(parents=True, exist_ok=True)

# Цветовая палитра из todo.txt
COLORS = {
    # Communist colors
    'soviet_red': (220, 20, 60),        # #DC143C
    'soviet_gold': (255, 215, 0),       # #FFD700
    'soviet_dark_brown': (62, 39, 35),  # #3E2723

    # Capitalist colors
    'usa_blue': (0, 82, 180),           # #0052B4
    'usa_white': (255, 255, 255),       # #FFFFFF
    'usa_gold': (255, 215, 0),          # #FFD700
    'usa_silver': (192, 192, 192),

    # Joint colors
    'paper_white': (245, 245, 245),     # #F5F5F5
    'weed_green': (34, 139, 34),        # #228B22
    'paper_brown': (139, 69, 19),       # #8B4513

    # Spawner Linker colors
    'ender_purple': (139, 0, 139),      # #8B008B
    'wood_brown': (101, 67, 33),        # #654321
    'redstone_red': (255, 0, 0),        # #FF0000
    'ender_particles': (255, 182, 255),

    # General colors
    'gray': (128, 128, 128),
    'dark_gray': (64, 64, 64),
    'light_gray': (192, 192, 192),
    'black': (0, 0, 0),
    'shadow': (0, 0, 0, 128),
}


def create_communist_collector():
    """Создает текстуру Communist Collector - красный сундук с серпом и молотом"""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Деревянная рамка
    draw.rectangle([0, 0, 15, 15], fill=COLORS['soviet_dark_brown'])

    # Красная основа
    draw.rectangle([1, 1, 14, 14], fill=COLORS['soviet_red'])

    # Тени для объема
    draw.line([1, 14, 14, 14], fill=(180, 10, 40))  # Нижняя тень
    draw.line([14, 1, 14, 14], fill=(180, 10, 40))  # Правая тень

    # Светлые грани
    draw.line([1, 1, 14, 1], fill=(240, 40, 80))   # Верхний свет
    draw.line([1, 1, 1, 14], fill=(240, 40, 80))   # Левый свет

    # Серп и молот в центре (упрощенная версия)
    # Молот
    draw.rectangle([7, 5, 8, 10], fill=COLORS['soviet_gold'])  # Рукоять
    draw.rectangle([5, 5, 10, 7], fill=COLORS['soviet_gold'])  # Боек

    # Серп (дуга)
    draw.point((6, 8), fill=COLORS['soviet_gold'])
    draw.point((5, 9), fill=COLORS['soviet_gold'])
    draw.point((6, 10), fill=COLORS['soviet_gold'])
    draw.point((7, 10), fill=COLORS['soviet_gold'])
    draw.point((8, 10), fill=COLORS['soviet_gold'])
    draw.point((9, 9), fill=COLORS['soviet_gold'])
    draw.point((10, 8), fill=COLORS['soviet_gold'])

    # Красная звезда в углу
    draw.point((2, 2), fill=COLORS['soviet_gold'])
    draw.point((3, 2), fill=COLORS['soviet_gold'])
    draw.point((2, 3), fill=COLORS['soviet_gold'])

    # Металлические петли
    draw.rectangle([4, 0, 5, 2], fill=COLORS['gray'])
    draw.rectangle([10, 0, 11, 2], fill=COLORS['gray'])

    img.save(BLOCK_DIR / 'communist_collector.png')
    print("✓ Communist Collector создан")


def create_capitalist_collector():
    """Создает текстуру Capitalist Collector - синий сейф с долларом"""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Металлическая рамка
    draw.rectangle([0, 0, 15, 15], fill=COLORS['usa_silver'])

    # Синяя основа
    draw.rectangle([1, 1, 14, 14], fill=COLORS['usa_blue'])

    # Белые полосы (как на флаге США)
    for y in range(3, 14, 3):
        draw.line([2, y, 13, y], fill=COLORS['usa_white'], width=1)

    # Тени для объема
    draw.line([1, 14, 14, 14], fill=(0, 52, 140))  # Нижняя тень
    draw.line([14, 1, 14, 14], fill=(0, 52, 140))  # Правая тень

    # Светлые грани
    draw.line([1, 1, 14, 1], fill=(40, 122, 220))  # Верхний свет
    draw.line([1, 1, 1, 14], fill=(40, 122, 220))  # Левый свет

    # Знак доллара в центре
    # Основная линия S
    pixels = [
        (7, 5), (8, 5), (9, 5),  # Верх
        (6, 6),
        (7, 7), (8, 7),          # Середина
        (9, 8),
        (6, 9), (7, 9), (8, 9),  # Низ
    ]
    for px in pixels:
        draw.point(px, fill=COLORS['usa_gold'])

    # Вертикальная линия через $
    draw.line([7, 4, 7, 10], fill=COLORS['usa_gold'])

    # Золотой замок
    draw.rectangle([7, 12, 8, 14], fill=COLORS['usa_gold'])
    draw.point((7, 11), fill=COLORS['usa_gold'])
    draw.point((8, 11), fill=COLORS['usa_gold'])

    img.save(BLOCK_DIR / 'capitalist_collector.png')
    print("✓ Capitalist Collector создан")


def create_joint():
    """Создает текстуру Joint - косяк с зеленой начинкой"""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Основная форма косяка (конус)
    # Широкая часть слева, узкая справа
    for x in range(3, 13):
        width = max(1, 4 - (x - 3) // 3)  # Сужается к концу
        y_center = 8
        for dy in range(-width, width + 1):
            if 0 <= y_center + dy < 16:
                if x < 5:  # Открытый конец с травой
                    color = COLORS['weed_green'] if abs(dy) < width else COLORS['paper_white']
                else:
                    color = COLORS['paper_white']
                draw.point((x, y_center + dy), fill=color)

    # Скрутка на конце
    draw.point((12, 8), fill=COLORS['paper_brown'])
    draw.point((13, 8), fill=COLORS['paper_brown'])

    # Складки/тени на бумаге
    for x in range(5, 12, 2):
        draw.point((x, 7), fill=(220, 220, 220))
        draw.point((x, 9), fill=(220, 220, 220))

    # Тлеющий конец (опционально)
    draw.point((3, 8), fill=(255, 100, 0, 200))  # Оранжевое свечение

    # Дымок
    draw.point((2, 7), fill=(200, 200, 200, 100))
    draw.point((1, 6), fill=(180, 180, 180, 80))
    draw.point((2, 5), fill=(160, 160, 160, 60))

    img.save(ITEM_DIR / 'joint.png')
    print("✓ Joint создан")


def create_spawner_linker():
    """Создает текстуру Spawner Linker - магический жезл с оком края"""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Деревянная рукоять (нижняя часть)
    for y in range(10, 15):
        draw.line([7, y, 8, y], fill=COLORS['wood_brown'])

    # Тень на рукояти
    draw.line([8, 10, 8, 14], fill=(81, 47, 13))

    # Основной стержень с редстоуном
    for y in range(4, 10):
        color = COLORS['ender_purple'] if y % 2 == 0 else COLORS['redstone_red']
        draw.point((7, y), fill=color)
        draw.point((8, y), fill=color)

    # Око края на верхушке (большой кристалл)
    # Основа кристалла
    draw.rectangle([6, 2, 9, 5], fill=COLORS['ender_purple'])

    # Центр ока (зеленый как ender pearl)
    draw.point((7, 3), fill=(46, 196, 182))
    draw.point((8, 3), fill=(46, 196, 182))
    draw.point((7, 4), fill=(46, 196, 182))
    draw.point((8, 4), fill=(46, 196, 182))

    # Блики на кристалле
    draw.point((6, 2), fill=(189, 100, 189))
    draw.point((9, 2), fill=(189, 100, 189))

    # Частицы эндера вокруг
    particles = [(5, 1), (10, 2), (5, 5), (10, 4), (4, 3), (11, 3)]
    for px, py in particles:
        if 0 <= px < 16 and 0 <= py < 16:
            draw.point((px, py), fill=COLORS['ender_particles'])

    # Энергетическая связь
    draw.point((6, 6), fill=(255, 100, 255, 150))
    draw.point((9, 6), fill=(255, 100, 255, 150))

    img.save(ITEM_DIR / 'spawner_linker.png')
    print("✓ Spawner Linker создан")


def create_fake_tax_bill():
    """Создает текстуру Fake Tax Bill - поддельный налоговый счет"""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Основа документа (синеватая бумага)
    draw.rectangle([2, 1, 13, 14], fill=(200, 200, 255))

    # Тени по краям
    draw.line([13, 2, 13, 14], fill=(150, 150, 205))
    draw.line([3, 14, 13, 14], fill=(150, 150, 205))

    # Текстовые линии (имитация текста)
    for y in [3, 5, 7, 9]:
        draw.line([4, y, 11, y], fill=(100, 100, 150))

    # Большая красная печать "FAKE"
    draw.rectangle([5, 6, 10, 10], fill=(255, 0, 0, 180))

    # Буквы FAKE (упрощенно)
    # F
    draw.point((6, 7), fill=(255, 255, 255))
    draw.point((6, 8), fill=(255, 255, 255))
    draw.point((6, 9), fill=(255, 255, 255))
    draw.point((7, 7), fill=(255, 255, 255))
    draw.point((7, 8), fill=(255, 255, 255))

    # A
    draw.point((8, 8), fill=(255, 255, 255))
    draw.point((8, 9), fill=(255, 255, 255))
    draw.point((9, 7), fill=(255, 255, 255))
    draw.point((9, 8), fill=(255, 255, 255))
    draw.point((9, 9), fill=(255, 255, 255))

    # Загнутый угол
    draw.polygon([(11, 1), (13, 1), (13, 3)], fill=(180, 180, 235))

    img.save(ITEM_DIR / 'fake_tax_bill.png')
    print("✓ Fake Tax Bill создан")


def create_music_disc(number, color_scheme):
    """Создает текстуру музыкального диска"""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Внешний круг диска
    for x in range(16):
        for y in range(16):
            dx = x - 7.5
            dy = y - 7.5
            dist = (dx*dx + dy*dy) ** 0.5

            if 5.5 <= dist <= 7.5:  # Внешнее кольцо
                draw.point((x, y), fill=color_scheme['outer'])
            elif 3.5 <= dist < 5.5:  # Среднее кольцо (основной цвет)
                draw.point((x, y), fill=color_scheme['main'])
            elif 2 <= dist < 3.5:    # Внутреннее кольцо
                draw.point((x, y), fill=(40, 40, 40))
            elif dist < 2:           # Центр (дырка)
                draw.point((x, y), fill=(20, 20, 20))

    # Блики для реалистичности
    draw.point((5, 5), fill=(255, 255, 255, 100))
    draw.point((6, 6), fill=(255, 255, 255, 80))
    draw.point((10, 10), fill=(255, 255, 255, 60))

    # Отражение света (дуга)
    for i in range(3, 7):
        draw.point((i, 4), fill=(255, 255, 255, 40))
        draw.point((4, i), fill=(255, 255, 255, 40))

    filename = f'music_disc_sound_{number}.png'
    img.save(ITEM_DIR / filename)
    print(f"✓ Music Disc {number} создан")


def create_rabbit_paintings():
    """Создает три вариации картин с кроликом и часами"""
    backgrounds = [
        ('brown', (101, 67, 33)),      # Коричневый фон
        ('green', (34, 100, 34)),      # Зеленый фон
        ('blue', (30, 60, 120))        # Синий фон
    ]

    for i, (name, bg_color) in enumerate(backgrounds, 1):
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)

        # Рамка картины
        draw.rectangle([0, 0, 15, 15], fill=(92, 51, 23))  # Деревянная рамка
        draw.rectangle([1, 1, 14, 14], fill=bg_color)      # Фон картины

        # Тело кролика (белый овал)
        draw.ellipse([5, 8, 10, 13], fill=(255, 255, 255))

        # Голова кролика
        draw.ellipse([6, 6, 9, 9], fill=(255, 255, 255))

        # Уши кролика
        draw.line([6, 6, 5, 3], fill=(255, 255, 255), width=1)
        draw.line([9, 6, 10, 3], fill=(255, 255, 255), width=1)

        # Глаза кролика
        draw.point((7, 7), fill=(0, 0, 0))
        draw.point((8, 7), fill=(0, 0, 0))

        # Нос
        draw.point((7, 8), fill=(255, 182, 193))

        # Часы в углу
        draw.ellipse([11, 2, 14, 5], fill=(255, 215, 0))  # Золотые часы

        # Стрелки часов
        draw.line([12, 3, 13, 3], fill=(0, 0, 0))  # Горизонтальная
        draw.line([12, 3, 12, 2], fill=(0, 0, 0))  # Вертикальная

        # Вариации для каждой картины
        if i == 1:
            # Добавим морковку
            draw.polygon([(3, 11), (4, 13), (5, 11)], fill=(255, 140, 0))
        elif i == 2:
            # Добавим цветок
            draw.point((3, 12), fill=(255, 255, 0))
            draw.point((2, 11), fill=(255, 20, 147))
            draw.point((4, 11), fill=(255, 20, 147))
        else:
            # Добавим траву
            draw.line([2, 14, 2, 13], fill=(0, 255, 0))
            draw.line([3, 14, 3, 12], fill=(0, 255, 0))
            draw.line([4, 14, 4, 13], fill=(0, 255, 0))

        suffix = '' if i == 1 else f'_{i}'
        filename = f'rabbit_clock_painting{suffix}.png'
        img.save(ITEM_DIR / filename)
        print(f"✓ Rabbit Clock Painting {i} создан")


def create_deliciousness_effect():
    """Создает иконку эффекта Deliciousness - психоделическая иконка с косяком"""
    img = Image.new('RGBA', (18, 18), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # Фиолетово-розовый фон (цвет эффекта 0xFF00FF)
    for x in range(18):
        for y in range(18):
            dx = x - 9
            dy = y - 9
            dist = (dx*dx + dy*dy) ** 0.5

            if dist <= 8:
                # Градиент от центра (розовый) к краям (фиолетовый)
                ratio = dist / 8.0
                r = int(255 * (1 - ratio * 0.3))
                g = int(0 + ratio * 100)
                b = int(255 * (1 - ratio * 0.2))
                draw.point((x, y), fill=(r, g, b))

    # Косяк в центре (упрощенный)
    # Белая бумага
    draw.rectangle([7, 8, 11, 10], fill=(245, 245, 245))

    # Зеленая трава внутри (с одной стороны)
    draw.line([7, 9, 8, 9], fill=(34, 139, 34), width=1)

    # Оранжевый тлеющий конец
    draw.point((11, 9), fill=(255, 140, 0))
    draw.point((12, 9), fill=(255, 100, 0))

    # Дым (серые частицы)
    draw.point((13, 9), fill=(200, 200, 200, 180))
    draw.point((14, 8), fill=(180, 180, 180, 140))
    draw.point((14, 10), fill=(180, 180, 180, 140))
    draw.point((15, 7), fill=(160, 160, 160, 100))
    draw.point((15, 11), fill=(160, 160, 160, 100))

    # Добавляем радужные блики для психоделического эффекта
    rainbow_pixels = [
        (5, 5, (255, 0, 255)),
        (12, 5, (255, 255, 0)),
        (5, 12, (0, 255, 255)),
        (12, 12, (255, 0, 128)),
        (9, 3, (128, 255, 0)),
        (9, 14, (255, 128, 255)),
    ]

    for px, py, color in rainbow_pixels:
        draw.point((px, py), fill=color)

    # Добавляем "звездочки" для усиления психоделического эффекта
    sparkles = [(4, 9), (14, 5), (14, 13), (6, 14)]
    for sx, sy in sparkles:
        draw.point((sx, sy), fill=(255, 255, 255, 200))

    img.save(MOB_EFFECT_DIR / 'deliciousness.png')
    print("✓ Deliciousness Effect иконка создана")


def main():
    """Главная функция генерации всех текстур"""
    print("🎨 Начинаем генерацию текстур для Tekilo мода...")
    print("=" * 50)

    # Блоки
    print("\n📦 Создание текстур блоков:")
    create_communist_collector()
    create_capitalist_collector()

    # Предметы
    print("\n🎮 Создание текстур предметов:")
    create_joint()
    create_spawner_linker()
    create_fake_tax_bill()

    # Музыкальные диски
    print("\n💿 Создание музыкальных дисков:")
    create_music_disc(1, {
        'outer': (100, 20, 20),    # Темно-красный
        'main': (220, 20, 60)      # Красный
    })
    create_music_disc(2, {
        'outer': (0, 40, 100),      # Темно-синий
        'main': (0, 82, 180)        # Синий
    })

    # Картины
    print("\n🖼️ Создание картин:")
    create_rabbit_paintings()

    # Эффекты
    print("\n✨ Создание иконок эффектов:")
    create_deliciousness_effect()

    print("\n" + "=" * 50)
    print("✅ Все текстуры успешно созданы!")
    print("\nСледующие шаги:")
    print("1. Запустите игру: ./gradlew runClient")
    print("2. Проверьте текстуры в игре")
    print("3. При желании отредактируйте их в любом графическом редакторе")

    # Проверка наличия всех файлов
    print("\n📋 Проверка созданных файлов:")

    blocks_to_check = ['communist_collector.png', 'capitalist_collector.png']
    items_to_check = [
        'joint.png', 'spawner_linker.png', 'fake_tax_bill.png',
        'music_disc_sound_1.png', 'music_disc_sound_2.png',
        'rabbit_clock_painting.png', 'rabbit_clock_painting_2.png', 'rabbit_clock_painting_3.png'
    ]
    effects_to_check = ['deliciousness.png']

    all_good = True
    for block in blocks_to_check:
        path = BLOCK_DIR / block
        if path.exists():
            print(f"  ✓ {block} - {path.stat().st_size} bytes")
        else:
            print(f"  ✗ {block} - НЕ НАЙДЕН")
            all_good = False

    for item in items_to_check:
        path = ITEM_DIR / item
        if path.exists():
            print(f"  ✓ {item} - {path.stat().st_size} bytes")
        else:
            print(f"  ✗ {item} - НЕ НАЙДЕН")
            all_good = False

    for effect in effects_to_check:
        path = MOB_EFFECT_DIR / effect
        if path.exists():
            print(f"  ✓ {effect} - {path.stat().st_size} bytes")
        else:
            print(f"  ✗ {effect} - НЕ НАЙДЕН")
            all_good = False

    if all_good:
        print("\n🎉 Все текстуры на месте!")
    else:
        print("\n⚠️ Некоторые текстуры не были созданы. Проверьте ошибки выше.")


if __name__ == "__main__":
    try:
        from PIL import Image, ImageDraw
        main()
    except ImportError:
        print("❌ Ошибка: Библиотека Pillow не установлена!")
        print("Установите её командой: pip install Pillow")
        print("Или: pip3 install Pillow")
        exit(1)