package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Rule 2: Can't shift-craft
 * Players cannot use shift-click to take crafting results.
 * Only regular click is allowed.
 */
public class Rule2NoShiftCraft extends AbstractRule {
    
    public Rule2NoShiftCraft() {
        super(2, "Нельзя забирать крафт через Shift");
    }
    
    /**
     * Check if player should be killed for shift-crafting
     */
    public boolean shouldKillPlayer(ServerPlayerEntity player) {
        return isValidTarget(player);
    }
    
    /**
     * Actually kill the player after the craft completes
     */
    public void killPlayerForViolation(ServerPlayerEntity player) {
        if (!isValidTarget(player)) return;
        killPlayer(player);
    }
}
