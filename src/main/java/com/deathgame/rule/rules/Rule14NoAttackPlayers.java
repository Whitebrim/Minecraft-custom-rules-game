package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Rule 14: Can't attack other participants
 * If a player attacks another participant, the attacker dies.
 */
public class Rule14NoAttackPlayers extends AbstractRule {
    
    public Rule14NoAttackPlayers() {
        super(14, "Нельзя ударять другого участника");
    }
    
    /**
     * Called when a player attacks another entity
     */
    public void onPlayerAttack(ServerPlayerEntity attacker, ServerPlayerEntity victim) {
        if (!isValidTarget(attacker)) return;
        
        // Check if victim is a participant
        if (!DeathGameMod.getInstance().getGameManager().isParticipant(victim)) {
            return;
        }
        
        DeathGameMod.LOGGER.info("[Rule14] Player {} attacked participant {}", 
            attacker.getName().getString(), victim.getName().getString());
        killPlayer(attacker);
    }
}
