package com.tekilo;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class TeamManager {
	private static final String COMMUNIST_TEAM_NAME = "communists";
	private static final String CAPITALIST_TEAM_NAME = "capitalists";
	private static final String COMMUNIST_SPY_NAME = "communist_spies";
	private static final String CAPITALIST_SPY_NAME = "capitalist_spies";

	public static void initializeTeams(MinecraftServer server) {
		Scoreboard scoreboard = server.getScoreboard();

		// Создаём команду коммунистов
		Team communistTeam = scoreboard.getTeam(COMMUNIST_TEAM_NAME);
		if (communistTeam == null) {
			communistTeam = scoreboard.addTeam(COMMUNIST_TEAM_NAME);
			communistTeam.setDisplayName(Text.literal("Коммунисты"));
			communistTeam.setColor(Formatting.RED);
			communistTeam.setFriendlyFireAllowed(false); // Запрещаем урон по своим
			System.out.println("[TekiloMod] Created team: " + COMMUNIST_TEAM_NAME);
		}

		// Создаём команду капиталистов
		Team capitalistTeam = scoreboard.getTeam(CAPITALIST_TEAM_NAME);
		if (capitalistTeam == null) {
			capitalistTeam = scoreboard.addTeam(CAPITALIST_TEAM_NAME);
			capitalistTeam.setDisplayName(Text.literal("Капиталисты"));
			capitalistTeam.setColor(Formatting.BLUE);
			capitalistTeam.setFriendlyFireAllowed(false); // Запрещаем урон по своим
			System.out.println("[TekiloMod] Created team: " + CAPITALIST_TEAM_NAME);
		}

		Team communistSpyTeam = scoreboard.getTeam(COMMUNIST_SPY_NAME);
		if (communistSpyTeam == null) {
			communistSpyTeam = scoreboard.addTeam(COMMUNIST_SPY_NAME);
			communistSpyTeam.setDisplayName(Text.literal("Шпионы-Коммунисты"));
			communistSpyTeam.setColor(Formatting.DARK_RED);
			communistSpyTeam.setFriendlyFireAllowed(true);
			System.out.println("[TekiloMod] Created team: " + COMMUNIST_SPY_NAME);
		}

		Team capitalistSpyTeam = scoreboard.getTeam(CAPITALIST_SPY_NAME);
		if (capitalistSpyTeam == null) {
			capitalistSpyTeam = scoreboard.addTeam(CAPITALIST_SPY_NAME);
			capitalistSpyTeam.setDisplayName(Text.literal("Шпионы-Капиталисты"));
			capitalistSpyTeam.setColor(Formatting.DARK_BLUE);
			capitalistSpyTeam.setFriendlyFireAllowed(true);
			System.out.println("[TekiloMod] Created team: " + CAPITALIST_SPY_NAME);
		}
	}

	public static void addPlayerToTeam(MinecraftServer server, ServerPlayerEntity player, FactionManager.Faction faction) {
		addPlayerToTeam(server, player, faction, false);
	}

	public static void addPlayerToTeam(MinecraftServer server, ServerPlayerEntity player, FactionManager.Faction faction, boolean isSpy) {
		Scoreboard scoreboard = server.getScoreboard();
		String playerName = player.getName().getString();

		// Удаляем игрока из всех команд
		scoreboard.clearTeam(playerName);

		// Добавляем в нужную команду
		Team team = null;
		switch (faction) {
			case COMMUNIST:
				team = scoreboard.getTeam(isSpy ? COMMUNIST_SPY_NAME : COMMUNIST_TEAM_NAME);
				break;
			case CAPITALIST:
				team = scoreboard.getTeam(isSpy ? CAPITALIST_SPY_NAME : CAPITALIST_TEAM_NAME);
				break;
			case NONE:
				return;
		}

		if (team != null) {
			scoreboard.addScoreHolderToTeam(playerName, team);
			System.out.println("[TekiloMod] Added player " + playerName + " to team " + team.getName());
		}
	}

	public static void removePlayerFromAllTeams(MinecraftServer server, ServerPlayerEntity player) {
		Scoreboard scoreboard = server.getScoreboard();
		String playerName = player.getName().getString();
		scoreboard.clearTeam(playerName);
	}
}
