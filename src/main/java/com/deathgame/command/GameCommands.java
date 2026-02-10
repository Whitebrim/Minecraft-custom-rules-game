package com.deathgame.command;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

import static com.deathgame.rule.RuleManager.TOTAL_RULES;

public class GameCommands {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("deathgame")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("start")
                    .executes(GameCommands::startGame))
                .then(CommandManager.literal("stop")
                    .executes(GameCommands::stopGame))
                .then(CommandManager.literal("reset")
                    .executes(GameCommands::resetGame))
                .then(CommandManager.literal("kick")
                    .then(CommandManager.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            GameManager gm = DeathGameMod.getInstance().getGameManager();
                            for (UUID uuid : gm.getParticipants()) {
                                ServerPlayerEntity p = gm.getServer().getPlayerManager().getPlayer(uuid);
                                if (p != null) {
                                    builder.suggest(p.getName().getString());
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(GameCommands::kickParticipant)))
                .then(CommandManager.literal("status")
                    .executes(GameCommands::showStatus))
        );
    }
    
    private static int startGame(CommandContext<ServerCommandSource> context) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        
        if (gameManager.getParticipants().isEmpty()) {
            context.getSource().sendError(Text.literal("Нет участников! Используйте /participate для регистрации."));
            return 0;
        }
        
        if (gameManager.startGame()) {
            context.getSource().sendFeedback(
                () -> Text.literal("Игра запущена!").formatted(Formatting.GREEN),
                true
            );
            return 1;
        } else {
            context.getSource().sendError(Text.literal("Игра уже запущена!"));
            return 0;
        }
    }
    
    private static int stopGame(CommandContext<ServerCommandSource> context) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        
        if (gameManager.stopGame()) {
            context.getSource().sendFeedback(
                () -> Text.literal("Игра остановлена.").formatted(Formatting.YELLOW),
                true
            );
            return 1;
        } else {
            context.getSource().sendError(Text.literal("Игра не запущена!"));
            return 0;
        }
    }
    
    private static int resetGame(CommandContext<ServerCommandSource> context) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        gameManager.resetGame();
        
        context.getSource().sendFeedback(
            () -> Text.literal("Игра полностью сброшена.").formatted(Formatting.YELLOW),
            true
        );
        return 1;
    }
    
    private static int kickParticipant(CommandContext<ServerCommandSource> context) {
        String playerName = StringArgumentType.getString(context, "player");
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        
        ServerPlayerEntity targetPlayer = gameManager.getServer().getPlayerManager().getPlayer(playerName);
        
        boolean success;
        if (targetPlayer != null) {
            success = gameManager.removeParticipant(targetPlayer);
        } else {
            success = gameManager.removeParticipantByName(playerName);
        }
        
        if (success) {
            context.getSource().sendFeedback(
                () -> Text.literal(playerName + " удалён из участников.").formatted(Formatting.YELLOW),
                true
            );
            return 1;
        } else {
            context.getSource().sendError(Text.literal("Игрок " + playerName + " не найден среди участников."));
            return 0;
        }
    }
    
    private static int showStatus(CommandContext<ServerCommandSource> context) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(
            () -> Text.literal("═══ Death Game Status ═══").formatted(Formatting.GOLD),
            false
        );
        
        String status = gameManager.isGameRunning() ? "ЗАПУЩЕНА" : "ОСТАНОВЛЕНА";
        Formatting statusColor = gameManager.isGameRunning() ? Formatting.GREEN : Formatting.RED;
        source.sendFeedback(
            () -> Text.literal("Статус: " + status).formatted(statusColor),
            false
        );
        
        if (gameManager.isGameRunning()) {
            String timeStr = GameManager.formatTime(gameManager.getElapsedTicks());
            source.sendFeedback(
                () -> Text.literal("⏱ Время: " + timeStr).formatted(Formatting.AQUA),
                false
            );
        }
        
        source.sendFeedback(
            () -> Text.literal("Участники (" + gameManager.getParticipants().size() + "/" + GameManager.MAX_PARTICIPANTS + "):").formatted(Formatting.YELLOW),
            false
        );
        
        for (UUID uuid : gameManager.getParticipants()) {
            ServerPlayerEntity player = gameManager.getServer().getPlayerManager().getPlayer(uuid);
            String name = player != null ? player.getName().getString() : uuid.toString();
            String onlineStatus = player != null ? " (онлайн)" : " (оффлайн)";
            
            source.sendFeedback(
                () -> Text.literal("  - " + name + onlineStatus).formatted(Formatting.WHITE),
                false
            );
        }
        
        int revealed = DeathGameMod.getInstance().getRuleManager().getRevealedCount();
        source.sendFeedback(
            () -> Text.literal("Правил раскрыто: " + revealed + "/" + TOTAL_RULES).formatted(Formatting.AQUA),
            false
        );
        
        return 1;
    }
}
