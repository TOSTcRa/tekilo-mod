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
    }

    public static void addPlayerToTeam(MinecraftServer server, ServerPlayerEntity player, FactionManager.Faction faction) {
        Scoreboard scoreboard = server.getScoreboard();
        String playerName = player.getName().getString();

        // Удаляем игрока из всех команд
        scoreboard.clearTeam(playerName);

        // Добавляем в нужную команду
        Team team = null;
        switch (faction) {
            case COMMUNIST:
                team = scoreboard.getTeam(COMMUNIST_TEAM_NAME);
                break;
            case CAPITALIST:
                team = scoreboard.getTeam(CAPITALIST_TEAM_NAME);
                break;
            case NONE:
                // Не добавляем в команду
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
