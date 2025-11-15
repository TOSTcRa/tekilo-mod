package com.tekilo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SpyLeakNotifier {
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static final Random random = new Random();

	public static void scheduleLeakNotification(MinecraftServer server, FactionManager.Faction faction) {
		int delayMinutes = 3 + random.nextInt(3);
		long delaySeconds = delayMinutes * 60L;

		scheduler.schedule(() -> {
			server.execute(() -> {
				notifyFaction(server, faction);
			});
		}, delaySeconds, TimeUnit.SECONDS);
	}

	private static void notifyFaction(MinecraftServer server, FactionManager.Faction faction) {
		String messageKey;

		if (faction == FactionManager.Faction.COMMUNIST) {
			messageKey = "message.tekilo.spy_leak.communist";
		} else {
			messageKey = "message.tekilo.spy_leak.capitalist";
		}

		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			FactionManager.Faction playerFaction = FactionManager.getPlayerFaction(player.getUuid());

			if (playerFaction == faction) {
				player.sendMessage(Text.translatable(messageKey), false);
			}
		}
	}

	public static void shutdown() {
		scheduler.shutdown();
	}
}
