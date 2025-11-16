package com.tekilo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FactionPersistence {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FACTIONS_FILE = "tekilo_factions.json";
	private static final String PLAYER_TYPES_FILE = "tekilo_player_types.json";

	public static void save(MinecraftServer server) {
		saveFactions(server);
		savePlayerTypes(server);
	}

	public static void saveFactions(MinecraftServer server) {
		try {
			File worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile();
			if (!worldDir.exists()) {
				worldDir.mkdirs();
			}
			File factionsFile = new File(worldDir, FACTIONS_FILE);

			Map<String, String> data = new HashMap<>();
			Map<UUID, FactionManager.Faction> factions = FactionManager.getAllFactions();

			for (Map.Entry<UUID, FactionManager.Faction> entry : factions.entrySet()) {
				data.put(entry.getKey().toString(), entry.getValue().name());
			}

			try (FileWriter writer = new FileWriter(factionsFile)) {
				GSON.toJson(data, writer);
			}
		} catch (Exception e) {
			System.err.println("[TekiloMod] Failed to save factions: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void savePlayerTypes(MinecraftServer server) {
		try {
			File worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile();
			if (!worldDir.exists()) {
				worldDir.mkdirs();
			}
			File typesFile = new File(worldDir, PLAYER_TYPES_FILE);

			Map<String, String> data = new HashMap<>();
			Map<UUID, FactionManager.PlayerType> types = FactionManager.getAllPlayerTypes();

			for (Map.Entry<UUID, FactionManager.PlayerType> entry : types.entrySet()) {
				data.put(entry.getKey().toString(), entry.getValue().name());
			}

			try (FileWriter writer = new FileWriter(typesFile)) {
				GSON.toJson(data, writer);
			}
		} catch (Exception e) {
			System.err.println("[TekiloMod] Failed to save player types: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void load(MinecraftServer server) {
		loadFactions(server);
		loadPlayerTypes(server);
	}

	public static void loadFactions(MinecraftServer server) {
		try {
			File worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile();
			File factionsFile = new File(worldDir, FACTIONS_FILE);

			if (!factionsFile.exists()) {
				return;
			}

			try (FileReader reader = new FileReader(factionsFile)) {
				Type type = new TypeToken<Map<String, String>>() {
				}.getType();

				Map<String, String> data = GSON.fromJson(reader, type);

				if (data != null) {
					for (Map.Entry<String, String> entry : data.entrySet()) {
						try {
							UUID playerId = UUID.fromString(entry.getKey());
							FactionManager.Faction faction = FactionManager.Faction.valueOf(entry.getValue());
							FactionManager.setPlayerFaction(playerId, faction);
						} catch (Exception e) {
							System.err.println("[TekiloMod] Failed to parse faction entry: " + e.getMessage());
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("[TekiloMod] Failed to load factions: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void loadPlayerTypes(MinecraftServer server) {
		try {
			File worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile();
			File typesFile = new File(worldDir, PLAYER_TYPES_FILE);

			if (!typesFile.exists()) {
				return;
			}

			try (FileReader reader = new FileReader(typesFile)) {
				Type type = new TypeToken<Map<String, String>>() {
				}.getType();

				Map<String, String> data = GSON.fromJson(reader, type);

				if (data != null) {
					for (Map.Entry<String, String> entry : data.entrySet()) {
						try {
							UUID playerId = UUID.fromString(entry.getKey());
							FactionManager.PlayerType playerType = FactionManager.PlayerType.valueOf(entry.getValue());
							FactionManager.setPlayerType(playerId, playerType);
						} catch (Exception e) {
							System.err.println("[TekiloMod] Failed to parse player types entry: " + e.getMessage());
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println("[TekiloMod] Failed to load player types: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
