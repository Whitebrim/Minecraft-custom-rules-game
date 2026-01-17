package com.deathgame.command;

import com.deathgame.DeathGameMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ParticipateCommand {
    
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("participate")
                .executes(ParticipateCommand::execute)
        );
    }
    
    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("Эта команда только для игроков!"));
            return 0;
        }
        
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) return 0;
        
        boolean success = DeathGameMod.getInstance().getGameManager().addParticipant(player);
        
        return success ? 1 : 0;
    }
}
