package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Rule 15: Can't send chat messages
 * If a player sends a chat message, they die.
 * Commands (starting with /) are allowed.
 */
public class Rule15NoChat extends AbstractRule {
    
    public Rule15NoChat() {
        super(15, "Нельзя писать в чат");
    }
    
    /**
     * Called when a player sends a chat message
     */
    public void onChatMessage(ServerPlayerEntity player, String message) {
        if (!isValidTarget(player)) return;
        
        // Commands are allowed
        if (message.startsWith("/")) {
            return;
        }
        
        DeathGameMod.LOGGER.info("[Rule15] Player {} sent chat message", player.getName().getString());
        killPlayer(player);
    }
}
