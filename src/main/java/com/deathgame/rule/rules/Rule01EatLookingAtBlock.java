package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * Rule 1: Can't eat while looking at a block
 * If a player finishes eating food and has a block targeted (crosshair on block), they die.
 */
public class Rule01EatLookingAtBlock extends AbstractRule {
    
    public Rule01EatLookingAtBlock() {
        super(1, "Нельзя есть смотря на блоки");
    }
    
    /**
     * Called from EatFoodMixin when player finishes eating
     */
    public void onPlayerEat(ServerPlayerEntity player) {
        if (!isValidTarget(player)) return;
        
        // Check if player is looking at a block
        HitResult hitResult = player.raycast(5.0, 0, false);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            DeathGameMod.LOGGER.info("[Rule1] Player {} ate while looking at block", player.getName().getString());
            killPlayer(player);
        }
    }
}
