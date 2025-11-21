#!/usr/bin/env python3
"""
Конвертер 3D моделей (GLB) в схематики Minecraft (Sponge Schematic v2)
"""
import trimesh
import numpy as np
import nbtlib
from nbtlib import Compound, Int, String, IntArray, List, Byte, Short
import sys
import gzip
from collections import defaultdict

# Палитра блоков Minecraft с их примерными RGB цветами
MINECRAFT_BLOCKS = {
    # Камень и серые блоки
    'minecraft:stone': (128, 128, 128),
    'minecraft:cobblestone': (127, 127, 127),
    'minecraft:andesite': (132, 135, 133),
    'minecraft:polished_andesite': (138, 141, 139),
    'minecraft:gray_concrete': (54, 57, 61),
    'minecraft:light_gray_concrete': (125, 125, 115),

    # Коричневые блоки
    'minecraft:oak_planks': (162, 130, 78),
    'minecraft:spruce_planks': (114, 84, 48),
    'minecraft:dark_oak_planks': (66, 43, 20),
    'minecraft:brown_concrete': (96, 59, 32),
    'minecraft:dirt': (134, 96, 67),

    # Светлые блоки
    'minecraft:sandstone': (216, 203, 155),
    'minecraft:smooth_sandstone': (222, 207, 163),
    'minecraft:quartz_block': (235, 233, 227),
    'minecraft:white_concrete': (207, 213, 214),
    'minecraft:bone_block': (229, 225, 207),

    # Темные блоки
    'minecraft:coal_block': (26, 26, 26),
    'minecraft:black_concrete': (8, 10, 15),
    'minecraft:deepslate': (75, 75, 75),
    'minecraft:deepslate_tiles': (55, 55, 55),

    # Красноватые/розовые
    'minecraft:red_sandstone': (186, 99, 43),
    'minecraft:terracotta': (152, 93, 66),
    'minecraft:red_concrete': (142, 33, 33),
    'minecraft:bricks': (150, 97, 83),

    # Оранжевые/желтые
    'minecraft:orange_concrete': (224, 97, 1),
    'minecraft:yellow_concrete': (240, 175, 21),
    'minecraft:gold_block': (255, 215, 0),
    'minecraft:honey_block': (255, 161, 18),

    # Зеленые
    'minecraft:moss_block': (90, 108, 64),
    'minecraft:green_concrete': (73, 91, 36),
    'minecraft:lime_concrete': (94, 169, 24),

    # Синие
    'minecraft:blue_concrete': (45, 47, 143),
    'minecraft:light_blue_concrete': (36, 137, 199),
    'minecraft:cyan_concrete': (21, 119, 136),
}

def find_closest_block(rgb):
    """Находит ближайший блок Minecraft по RGB цвету"""
    r, g, b = rgb
    min_distance = float('inf')
    closest_block = 'minecraft:stone'

    for block, (br, bg, bb) in MINECRAFT_BLOCKS.items():
        # Евклидово расстояние в RGB пространстве
        distance = ((r - br) ** 2 + (g - bg) ** 2 + (b - bb) ** 2) ** 0.5
        if distance < min_distance:
            min_distance = distance
            closest_block = block

    return closest_block

def voxelize_mesh_with_colors(mesh, resolution=64):
    """Преобразует mesh в воксели с сохранением цветов"""
    print(f"Вокселизация модели с разрешением {resolution}x{resolution}x{resolution}...")

    # Нормализуем размер модели
    bounds = mesh.bounds
    size = bounds[1] - bounds[0]
    max_size = max(size)

    # Масштабируем mesh чтобы вписался в заданное разрешение
    scale = (resolution - 1) / max_size
    original_mesh = mesh.copy()
    mesh.apply_scale(scale)

    # Центрируем
    translation = -mesh.bounds[0]
    mesh.apply_translation(translation)

    # Создаем воксельную сетку
    voxels = mesh.voxelized(pitch=1.0)

    # Извлекаем цвета
    colors_map = {}
    filled = voxels.matrix
    coords = np.argwhere(filled)

    print(f"Извлечение цветов из модели ({len(coords)} вокселей)...")

    # Пробуем разные источники цветов
    vertex_colors = None

    # 1. Цвета вершин
    if hasattr(mesh.visual, 'vertex_colors') and mesh.visual.vertex_colors is not None:
        print("→ Обнаружены цвета вершин")
        vertex_colors = mesh.visual.vertex_colors

    # 2. Материалы с текстурами
    elif hasattr(mesh.visual, 'material'):
        print("→ Обнаружены материалы с текстурами")
        material = mesh.visual.material

        # Пробуем получить baseColor из PBR материала
        if hasattr(material, 'baseColorTexture') and material.baseColorTexture is not None:
            print("→ Загружаем baseColor текстуру...")
            texture_img = material.baseColorTexture

            # Конвертируем PIL Image в numpy array если нужно
            if hasattr(texture_img, 'size'):  # PIL Image
                import PIL.Image
                texture_img = np.array(texture_img)
                print(f"→ Текстура конвертирована: {texture_img.shape}")

            # Получаем UV координаты
            if hasattr(mesh.visual, 'uv') and mesh.visual.uv is not None:
                uv = mesh.visual.uv
                print(f"→ UV координаты найдены: {len(uv)} точек")

                # Создаем цвета для вершин на основе UV и текстуры
                vertex_colors = np.zeros((len(mesh.vertices), 4), dtype=np.uint8)

                for i, uv_coord in enumerate(uv):
                    if i % 50000 == 0 and i > 0:
                        print(f"   Обработка UV: {i}/{len(uv)}...")

                    u, v = uv_coord
                    # UV координаты в диапазоне [0, 1], зажимаем их
                    u = max(0, min(1, u))
                    v = max(0, min(1, v))

                    # Преобразуем в пиксельные координаты
                    x = int(u * (texture_img.shape[1] - 1))
                    y = int((1 - v) * (texture_img.shape[0] - 1))  # Инвертируем V

                    # Получаем цвет пикселя
                    if 0 <= y < texture_img.shape[0] and 0 <= x < texture_img.shape[1]:
                        pixel = texture_img[y, x]
                        # Обрабатываем разные форматы (RGB, RGBA)
                        if len(pixel) == 3:
                            vertex_colors[i] = [pixel[0], pixel[1], pixel[2], 255]
                        else:
                            vertex_colors[i] = pixel[:4]

                print(f"→ Цвета извлечены из текстуры")

    # 3. Используем face colors если есть
    elif hasattr(mesh.visual, 'face_colors') and mesh.visual.face_colors is not None:
        print("→ Обнаружены цвета граней")
        face_colors = mesh.visual.face_colors
        # Конвертируем face colors в vertex colors
        vertex_colors = np.zeros((len(mesh.vertices), 4), dtype=np.uint8)
        for face_idx, face in enumerate(mesh.faces):
            for vertex_idx in face:
                vertex_colors[vertex_idx] = face_colors[face_idx]

    if vertex_colors is not None:
        print(f"→ Сопоставление цветов с вокселями...")
        # Для каждого вокселя находим ближайшие вершины
        sample_size = min(len(coords), 5000)  # Ограничиваем для производительности
        sampled_coords = coords[np.random.choice(len(coords), sample_size, replace=False)]

        for i, coord in enumerate(sampled_coords):
            if i % 1000 == 0 and i > 0:
                print(f"   Обработано {i}/{sample_size} вокселей...")

            x, y, z = coord
            voxel_center = np.array([x, y, z]) + 0.5

            # Находим ближайшую вершину
            distances = np.linalg.norm(mesh.vertices - voxel_center, axis=1)
            closest_vertex = np.argmin(distances)

            rgb = tuple(vertex_colors[closest_vertex][:3])
            colors_map[tuple(coord)] = rgb

        print(f"✓ Извлечено {len(colors_map)} уникальных цветов")
    else:
        print("✗ Цвета не обнаружены в модели")

    return voxels, colors_map

def voxelize_mesh(mesh, resolution=64):
    """Преобразует mesh в воксели"""
    print(f"Вокселизация модели с разрешением {resolution}x{resolution}x{resolution}...")

    # Нормализуем размер модели
    bounds = mesh.bounds
    size = bounds[1] - bounds[0]
    max_size = max(size)

    # Масштабируем mesh чтобы вписался в заданное разрешение
    scale = (resolution - 1) / max_size
    mesh.apply_scale(scale)

    # Центрируем
    mesh.apply_translation(-mesh.bounds[0])

    # Создаем воксельную сетку
    voxels = mesh.voxelized(pitch=1.0)

    return voxels

def encode_varint(value):
    """Кодирует число в varint формат"""
    bytes_list = []
    while True:
        temp = value & 0x7F
        value >>= 7
        if value != 0:
            temp |= 0x80
        bytes_list.append(temp)
        if value == 0:
            break
    return bytes(bytes_list)

def voxels_to_schematic(voxels, output_file, block_type="minecraft:stone", colors_map=None, use_colors=False):
    """Преобразует воксели в схематику Minecraft (Sponge Schematic v2)"""
    print("Создание схематики...")

    # Получаем заполненные воксели
    filled = voxels.matrix
    coords = np.argwhere(filled)

    if len(coords) == 0:
        print("Ошибка: нет заполненных вокселей!")
        return False

    # Получаем границы
    min_coord = coords.min(axis=0)
    max_coord = coords.max(axis=0)
    size = max_coord - min_coord + 1
    width, height, length = int(size[0]), int(size[1]), int(size[2])

    print(f"Размер структуры: {width}x{height}x{length} блоков")
    print(f"Количество блоков: {len(coords)}")

    # Создаем палитру блоков и массив блоков
    palette = {'minecraft:air': 0}
    block_to_index = {'minecraft:air': 0}
    next_index = 1

    # Инициализируем массив нулями (воздух)
    block_data = np.zeros((width, height, length), dtype=np.int32)

    # Заполняем блоками
    if use_colors and colors_map:
        print("Использую цвета из модели для подбора блоков...")
        # Собираем статистику по цветам для градиента
        height_colors = defaultdict(list)
        for coord in coords:
            if tuple(coord) in colors_map:
                y = coord[1]
                height_colors[y].append(colors_map[tuple(coord)])

        # Вычисляем средний цвет для каждого уровня высоты
        avg_colors = {}
        for y, colors in height_colors.items():
            avg_color = tuple(np.mean(colors, axis=0).astype(int))
            avg_colors[y] = find_closest_block(avg_color)

    for i, coord in enumerate(coords):
        if i % 10000 == 0 and i > 0:
            print(f"Обработано {i}/{len(coords)} блоков...")

        x, y, z = coord - min_coord

        # Определяем блок
        if use_colors and colors_map and tuple(coord) in colors_map:
            rgb = colors_map[tuple(coord)]
            block = find_closest_block(rgb)
        elif use_colors and avg_colors and (coord[1] in avg_colors):
            block = avg_colors[coord[1]]
        else:
            # Используем градиент по высоте
            height_ratio = (coord[1] - min_coord[1]) / max(1, size[1] - 1)
            if height_ratio < 0.3:
                block = 'minecraft:deepslate'
            elif height_ratio < 0.5:
                block = 'minecraft:stone'
            elif height_ratio < 0.7:
                block = 'minecraft:andesite'
            else:
                block = 'minecraft:smooth_stone'

        # Добавляем блок в палитру если его еще нет
        if block not in block_to_index:
            block_to_index[block] = next_index
            palette[block] = next_index
            next_index += 1

        block_data[x, y, z] = block_to_index[block]

    print(f"Использовано блоков разных типов: {len(palette)}")

    # Преобразуем в плоский массив (YZX порядок) с varint кодированием
    flat_bytes = bytearray()
    for y in range(height):
        for z in range(length):
            for x in range(width):
                flat_bytes.extend(encode_varint(int(block_data[x, y, z])))

    # Создаем NBT структуру (Sponge Schematic v2)
    nbt_data = Compound({
        'Version': Int(2),
        'DataVersion': Int(3953),  # Minecraft 1.21.1
        'Width': Short(width),
        'Height': Short(height),
        'Length': Short(length),
        'Offset': IntArray([0, 0, 0]),
        'Metadata': Compound({
            'Name': String('genghis_khan_statue'),
            'Author': String('Tekilo Mod'),
        }),
        'Palette': Compound({block: Int(idx) for block, idx in palette.items()}),
        'BlockData': nbtlib.ByteArray(flat_bytes),
        'PaletteMax': Int(len(palette))
    })

    # Сохраняем
    print(f"Сохранение в {output_file}...")
    nbt_file = nbtlib.File(nbt_data, gzipped=True)
    nbt_file.save(output_file)

    return True

def main():
    if len(sys.argv) < 2:
        print("Использование: python3 glb_to_schematic_v2.py <файл.glb> [разрешение] [--colors]")
        print("Пример: python3 glb_to_schematic_v2.py model.glb 64")
        print("        python3 glb_to_schematic_v2.py model.glb 128 --colors")
        sys.exit(1)

    input_file = sys.argv[1]
    resolution = int(sys.argv[2]) if len(sys.argv) > 2 else 64
    use_colors = '--colors' in sys.argv

    output_file = input_file.rsplit('.', 1)[0] + '.schem'

    print(f"Загрузка модели: {input_file}")
    print(f"Режим: {'Разноцветная схематика' if use_colors else 'Градиент по высоте'}")

    try:
        # Загружаем GLB
        mesh = trimesh.load(input_file, force='mesh')

        # Если это сцена с несколькими объектами, объединяем
        if isinstance(mesh, trimesh.Scene):
            print("Обнаружена сцена с несколькими объектами, объединяем...")
            mesh = mesh.dump(concatenate=True)

        print(f"Модель загружена: {len(mesh.vertices)} вершин, {len(mesh.faces)} полигонов")

        # Проверяем наличие цветов в модели
        colors_map = None
        if use_colors:
            voxels, colors_map = voxelize_mesh_with_colors(mesh, resolution)
            if not colors_map:
                print("Цвета в модели не обнаружены, используем градиент по высоте...")
        else:
            voxels = voxelize_mesh(mesh, resolution)

        # Конвертируем в схематику
        success = voxels_to_schematic(voxels, output_file, colors_map=colors_map, use_colors=use_colors)

        if success:
            print(f"\n✓ Готово! Схематика сохранена: {output_file}")
            print(f"\nДля загрузки в Minecraft используй WorldEdit:")
            print(f"  //schem load {output_file.rsplit('/', 1)[-1].rsplit('.', 1)[0]}")
            print(f"  //paste")

    except Exception as e:
        print(f"Ошибка: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    main()
