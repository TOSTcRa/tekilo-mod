#!/bin/bash

echo "🚀 Генератор ассетов для Tekilo мода"
echo "===================================="

# Генерация текстур
echo -e "\n1️⃣ Генерация текстур..."
if command -v python3 &> /dev/null; then
    python3 generate_textures.py
else
    echo "⚠️ Python3 не найден, используем ImageMagick для базовых текстур"
    ./generate_textures.sh
fi

# Генерация JSON моделей
echo -e "\n2️⃣ Проверка JSON моделей..."
python3 generate_json_models.py

# Проверка результатов
echo -e "\n3️⃣ Итоговая проверка..."
echo "======================="

# Подсчет файлов
BLOCK_TEXTURES=$(ls src/main/resources/assets/tekilo/textures/block/*.png 2>/dev/null | wc -l)
ITEM_TEXTURES=$(ls src/main/resources/assets/tekilo/textures/item/*.png 2>/dev/null | wc -l)
BLOCKSTATES=$(ls src/main/resources/assets/tekilo/blockstates/*.json 2>/dev/null | wc -l)
BLOCK_MODELS=$(ls src/main/resources/assets/tekilo/models/block/*.json 2>/dev/null | wc -l)
ITEM_MODELS=$(ls src/main/resources/assets/tekilo/models/item/*.json 2>/dev/null | wc -l)
ITEMS=$(ls src/main/resources/assets/tekilo/items/*.json 2>/dev/null | wc -l)

echo "📊 Статистика:"
echo "  • Текстуры блоков: $BLOCK_TEXTURES"
echo "  • Текстуры предметов: $ITEM_TEXTURES"
echo "  • Blockstates: $BLOCKSTATES"
echo "  • Модели блоков: $BLOCK_MODELS"
echo "  • Модели предметов: $ITEM_MODELS"
echo "  • Определения предметов: $ITEMS"

echo -e "\n✅ Генерация завершена!"
echo "Теперь можешь запустить: ./gradlew runClient"