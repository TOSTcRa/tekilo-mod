#!/bin/bash

# Создаем директории если их нет
mkdir -p src/main/resources/assets/tekilo/textures/block
mkdir -p src/main/resources/assets/tekilo/textures/item

echo "Генерация недостающих текстур для Tekilo мод..."

# === БЛОКИ ===

# Communist Collector - красный ящик с серпом и молотом
echo "Создаю communist_collector.png..."
magick -size 16x16 xc:darkred \
    -draw "fill red rectangle 1,1 14,14" \
    -draw "fill yellow circle 8,8 8,4" \
    -draw "fill red text 5,11 '☭'" \
    -depth 8 \
    src/main/resources/assets/tekilo/textures/block/communist_collector.png

# Capitalist Collector - зеленый ящик с долларом
echo "Создаю capitalist_collector.png..."
magick -size 16x16 xc:darkgreen \
    -draw "fill green rectangle 1,1 14,14" \
    -draw "fill gold circle 8,8 8,4" \
    -draw "fill darkgreen text 6,11 '$'" \
    -depth 8 \
    src/main/resources/assets/tekilo/textures/block/capitalist_collector.png

# === ПРЕДМЕТЫ ===

# Fake Tax Bill - модифицированная версия tax_bill
echo "Создаю fake_tax_bill.png..."
if [ -f "src/main/resources/assets/tekilo/textures/item/tax_bill.png" ]; then
    magick src/main/resources/assets/tekilo/textures/item/tax_bill.png \
        -modulate 90,70,110 \
        -draw "fill rgba(255,0,0,0.3) rectangle 0,0 16,16" \
        src/main/resources/assets/tekilo/textures/item/fake_tax_bill.png
else
    # Создаем с нуля если оригинал не найден
    magick -size 16x16 xc:lightyellow \
        -draw "fill brown rectangle 2,2 14,14" \
        -draw "fill red text 4,10 'FAKE'" \
        -depth 8 \
        src/main/resources/assets/tekilo/textures/item/fake_tax_bill.png
fi

# Joint - сигарета с зеленым дымом
echo "Создаю joint.png..."
magick -size 16x16 xc:transparent \
    -draw "fill white rectangle 4,8 12,10" \
    -draw "fill orange rectangle 4,8 6,10" \
    -draw "fill green circle 13,6 13,4" \
    -draw "fill 'rgba(0,255,0,0.5)' circle 14,4 14,2" \
    -depth 8 \
    src/main/resources/assets/tekilo/textures/item/joint.png

# Spawner Linker - технический инструмент
echo "Создаю spawner_linker.png..."
magick -size 16x16 xc:transparent \
    -draw "fill gray rectangle 6,2 10,14" \
    -draw "fill cyan circle 8,4 8,2" \
    -draw "fill blue circle 8,12 8,10" \
    -draw "stroke cyan stroke-width 1 line 8,4 8,12" \
    -depth 8 \
    src/main/resources/assets/tekilo/textures/item/spawner_linker.png

# Music Disc 1 - красный диск
echo "Создаю music_disc_sound_1.png..."
magick -size 16x16 xc:transparent \
    -draw "fill darkred circle 8,8 8,1" \
    -draw "fill red circle 8,8 8,3" \
    -draw "fill black circle 8,8 8,6" \
    -draw "fill white circle 8,8 8,7" \
    -depth 8 \
    src/main/resources/assets/tekilo/textures/item/music_disc_sound_1.png

# Music Disc 2 - синий диск
echo "Создаю music_disc_sound_2.png..."
magick -size 16x16 xc:transparent \
    -draw "fill darkblue circle 8,8 8,1" \
    -draw "fill blue circle 8,8 8,3" \
    -draw "fill black circle 8,8 8,6" \
    -draw "fill white circle 8,8 8,7" \
    -depth 8 \
    src/main/resources/assets/tekilo/textures/item/music_disc_sound_2.png

# Rabbit Clock Paintings - картины с кроликом и часами
for i in 1 2 3; do
    name="rabbit_clock_painting"
    if [ $i -gt 1 ]; then
        name="${name}_${i}"
    fi

    echo "Создаю ${name}.png..."

    # Разные цвета фона для вариаций
    case $i in
        1) bg_color="brown" ;;
        2) bg_color="darkgreen" ;;
        3) bg_color="darkblue" ;;
    esac

    magick -size 16x16 xc:${bg_color} \
        -draw "fill sienna rectangle 1,1 15,15" \
        -draw "fill white ellipse 8,10 3,4" \
        -draw "fill white ellipse 6,6 1,2" \
        -draw "fill white ellipse 10,6 1,2" \
        -draw "fill pink circle 8,10 8,9" \
        -draw "fill gold circle 12,3 12,2" \
        -draw "stroke black stroke-width 1 line 12,3 13,2" \
        -depth 8 \
        src/main/resources/assets/tekilo/textures/item/${name}.png
done

echo "✓ Все базовые текстуры созданы!"
echo ""
echo "Теперь ты можешь:"
echo "1. Использовать эти текстуры как есть"
echo "2. Отредактировать их в любом графическом редакторе"
echo "3. Заменить их на более качественные версии позже"