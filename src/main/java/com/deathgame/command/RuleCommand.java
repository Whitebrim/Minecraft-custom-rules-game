package com.deathgame.command;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.RuleManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class RuleCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Admin commands - require permission level 2
        dispatcher.register(
            CommandManager.literal("rule")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("enable")
                    .then(CommandManager.argument("id", IntegerArgumentType.integer(1, RuleManager.TOTAL_RULES))
                        .executes(RuleCommand::enableRule)))
                .then(CommandManager.literal("disable")
                    .then(CommandManager.argument("id", IntegerArgumentType.integer(1, RuleManager.TOTAL_RULES))
                        .executes(RuleCommand::disableRule)))
                .then(CommandManager.literal("reveal")
                    .then(CommandManager.argument("id", IntegerArgumentType.integer(1, RuleManager.TOTAL_RULES))
                        .executes(RuleCommand::revealRule)))
        );
        
        // /rulelist - available to all players
        dispatcher.register(
            CommandManager.literal("rulelist")
                .executes(RuleCommand::listRules)
        );
    }
    
    private static int enableRule(CommandContext<ServerCommandSource> context) {
        int ruleId = IntegerArgumentType.getInteger(context, "id");
        RuleManager ruleManager = DeathGameMod.getInstance().getRuleManager();
        
        if (ruleManager.enableRule(ruleId)) {
            DeathGameMod.getInstance().getGameManager().saveState();
            context.getSource().sendFeedback(
                () -> Text.literal("Правило #" + ruleId + " включено.").formatted(Formatting.GREEN),
                true
            );
            return 1;
        } else {
            context.getSource().sendError(Text.literal("Не удалось включить правило #" + ruleId));
            return 0;
        }
    }
    
    private static int disableRule(CommandContext<ServerCommandSource> context) {
        int ruleId = IntegerArgumentType.getInteger(context, "id");
        RuleManager ruleManager = DeathGameMod.getInstance().getRuleManager();
        
        if (ruleManager.disableRule(ruleId)) {
            DeathGameMod.getInstance().getGameManager().saveState();
            context.getSource().sendFeedback(
                () -> Text.literal("Правило #" + ruleId + " отключено.").formatted(Formatting.YELLOW),
                true
            );
            return 1;
        } else {
            context.getSource().sendError(Text.literal("Не удалось отключить правило #" + ruleId));
            return 0;
        }
    }
    
    private static int revealRule(CommandContext<ServerCommandSource> context) {
        int ruleId = IntegerArgumentType.getInteger(context, "id");
        RuleManager ruleManager = DeathGameMod.getInstance().getRuleManager();
        
        if (ruleManager.isRuleRevealed(ruleId)) {
            context.getSource().sendError(Text.literal("Правило #" + ruleId + " уже раскрыто!"));
            return 0;
        }
        
        if (ruleManager.revealRule(ruleId)) {
            DeathGameMod.getInstance().getGameManager().saveState();
            context.getSource().sendFeedback(
                () -> Text.literal("Правило #" + ruleId + " раскрыто!").formatted(Formatting.GOLD),
                true
            );
            return 1;
        } else {
            context.getSource().sendError(Text.literal("Не удалось раскрыть правило #" + ruleId));
            return 0;
        }
    }
    
    private static int listRules(CommandContext<ServerCommandSource> context) {
        RuleManager ruleManager = DeathGameMod.getInstance().getRuleManager();
        ServerCommandSource source = context.getSource();
        
        source.sendFeedback(
            () -> Text.literal("═══ Статус правил ═══").formatted(Formatting.GOLD),
            false
        );
        
        for (int i = 1; i <= RuleManager.TOTAL_RULES; i++) {
            final int ruleId = i;
            boolean enabled = ruleManager.isRuleEnabled(ruleId);
            boolean revealed = ruleManager.isRuleRevealed(ruleId);
            String description = ruleManager.getRuleDescription(ruleId);
            
            Formatting color = enabled ? (revealed ? Formatting.GREEN : Formatting.WHITE) : Formatting.GRAY;
            String status = enabled ? (revealed ? "✓" : "○") : "✗";
            String desc = revealed ? " - " + description : "";
            
            source.sendFeedback(
                () -> Text.literal("[" + status + "] Правило #" + ruleId + desc).formatted(color),
                false
            );
        }
        
        source.sendFeedback(
            () -> Text.literal("Раскрыто: " + ruleManager.getRevealedCount() + "/" + RuleManager.TOTAL_RULES).formatted(Formatting.YELLOW),
            false
        );
        
        return 1;
    }
}
