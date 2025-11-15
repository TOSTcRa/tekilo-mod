package com.tekilo;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TellInfoCommand {
	private static final Map<UUID, Long> lastUsed = new HashMap<>();
	private static final long COOLDOWN_MS = 60000;

	public static void register() {
		CommandRegistrationCallback.EVENT.register(TellInfoCommand::registerCommand);
	}

	private static void registerCommand(
		CommandDispatcher<ServerCommandSource> dispatcher,
		CommandRegistryAccess registryAccess,
		CommandManager.RegistrationEnvironment environment
	) {
		dispatcher.register(CommandManager.literal("tellinfo")
			.then(CommandManager.argument("player", EntityArgumentType.player())
				.then(CommandManager.argument("message", StringArgumentType.greedyString())
					.executes(TellInfoCommand::execute)
				)
			)
		);
	}

	private static int execute(CommandContext<ServerCommandSource> context) {
		try {
			ServerPlayerEntity sender = context.getSource().getPlayerOrThrow();
			ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
			String messageText = StringArgumentType.getString(context, "message");

			if (!FactionManager.isSpy(sender.getUuid())) {
				sender.sendMessage(Text.translatable("command.tekilo.tellinfo.not_spy"), false);
				return 0;
			}

			long currentTime = System.currentTimeMillis();
			if (lastUsed.containsKey(sender.getUuid())) {
				long timeSinceLastUse = currentTime - lastUsed.get(sender.getUuid());
				if (timeSinceLastUse < COOLDOWN_MS) {
					long secondsLeft = (COOLDOWN_MS - timeSinceLastUse) / 1000;
					sender.sendMessage(Text.translatable("command.tekilo.tellinfo.cooldown", secondsLeft), false);
					return 0;
				}
			}

			FactionManager.Faction senderFaction = FactionManager.getPlayerFaction(sender.getUuid());

			ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

			String title;
			if (senderFaction == FactionManager.Faction.COMMUNIST) {
				title = "Информация от НКВД";
			} else {
				title = "Шпионский отчет";
			}

			// Создаём содержимое книги используя Components API
			RawFilteredPair<Text> page = RawFilteredPair.of(Text.literal(messageText));
			List<RawFilteredPair<Text>> pages = List.of(page);

			WrittenBookContentComponent bookContent = new WrittenBookContentComponent(
				RawFilteredPair.of(title),
				sender.getName().getString(),
				0, // generation - оригинал
				pages,
				true // resolved
			);

			book.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, bookContent);

			target.giveItemStack(book);

			sender.sendMessage(Text.translatable("command.tekilo.tellinfo.success", target.getName()), false);

			lastUsed.put(sender.getUuid(), currentTime);
			SpyLeakNotifier.scheduleLeakNotification(context.getSource().getServer(), senderFaction);
			return 1;
		} catch (Exception e) {
			context.getSource().sendError(Text.literal("Ошибка при выполнении команды: " + e.getMessage()));
			e.printStackTrace();
			return 0;
		}
	}
}
