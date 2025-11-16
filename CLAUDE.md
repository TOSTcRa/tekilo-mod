# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Tekilo is a Minecraft Fabric mod (1.21.10) written in Java 21. The mod adds faction systems, custom items/blocks, game mechanics, and animations using GeckoLib.

## Build Commands

```bash
# Build the mod JAR
./gradlew build

# Run Minecraft client with mod loaded (development)
./gradlew runClient

# Run Minecraft server with mod loaded (development)
./gradlew runServer

# Clean build artifacts
./gradlew clean

# Compile classes only
./gradlew classes
```

## Architecture

### Source Structure
- `src/main/java/com/tekilo/` - Server-side logic (28 classes)
- `src/client/java/com/tekilo/` - Client-side logic (rendering, animations, mixins)
- `src/main/resources/assets/tekilo/` - Textures, models, sounds, translations

### Key Modules

**Registry Classes** - Central registration for game objects:
- `ModItems.java` - Item registry
- `ModBlocks.java` - Block registry
- `ModBlockEntities.java` - Block entity registry
- `ModDataComponents.java` - Data components

**Faction System**:
- `FactionManager.java` - Faction state management
- `FactionPersistence.java` - World save/load
- `FactionCommand.java` - In-game commands
- `TeamManager.java` - Team coordination

**Network Layer** (`network/` package):
- 7 payload classes for client-server synchronization
- `ServerNetworkHandler.java` - Server-side packet processing

**Entry Points**:
- `TekiloMod.java` - Main mod initializer (implements `ModInitializer`)
- `FaceFlashClientMod.java` - Client-side initializer

### Patterns Used
- Fabric API event handlers for game mechanics
- Block entities for complex stateful blocks
- Custom network payloads for synchronization
- Mixins for client-side modifications
- GeckoLib for animated models (geo/ and animations/ folders)

## Important Notes

- Read Fabric documentation before making changes: https://fabricmc.net/wiki/
- Mod ID: `tekilo` (namespace for assets and registries)
- Mod manifest: `src/main/resources/fabric.mod.json`
- Mixin config: `src/main/resources/tekilo.client.mixins.json`
