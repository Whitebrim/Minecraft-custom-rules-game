package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * Rule 4: If you die by fall damage, everyone else dies, you survive
 * If a participant would receive lethal fall damage, they survive with half a heart,
 * and all other participants die.
 */
public class Rule4FallDamageTransfer extends AbstractRule {
    
    public Rule4FallDamageTransfer() {
        super(4, "Смертельное падение убивает всех остальных, ты выживаешь");
    }
    
    /**
     * Called from mixin when lethal fall damage would be dealt
     * Returns true if the damage should be cancelled (transfer occurs)
     */
    public boolean onLethalFallDamage(ServerPlayerEntity player, float damage) {
        if (!isValidTarget(player)) return false;
        
        // Check if this would be lethal
        if (player.getHealth() - damage > 0) {
            return false; // Not lethal, proceed normally
        }
        
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        
        // Increment trigger count for the faller only
        gameManager.incrementTriggerCount(player);
        
        // Get rule name for messages
        String ruleName = DeathGameMod.getInstance().getRuleManager().isRuleRevealed(id) 
            ? DeathGameMod.getInstance().getRuleManager().getRuleDescription(id)
            : "Правило #" + id;
        
        // Message for the faller - they triggered the rule but survived
        for (ServerPlayerEntity p : gameManager.getServer().getPlayerManager().getPlayerList()) {
            p.sendMessage(Text.literal(player.getName().getString() + " нарушил: " + ruleName).formatted(Formatting.GOLD));
        }
        
        // Set player to half a heart (1 health point = half a heart)
        player.setHealth(1.0f);
        
        // Kill all other participants (without incrementing their counters)
        // But send death messages for them
        for (UUID uuid : gameManager.getParticipants()) {
            if (uuid.equals(player.getUuid())) continue;
            
            ServerPlayerEntity otherPlayer = gameManager.getServer().getPlayerManager().getPlayer(uuid);
            if (otherPlayer != null && !otherPlayer.isSpectator() && !otherPlayer.isCreative()) {
                // Send death message for other players
                for (ServerPlayerEntity p : gameManager.getServer().getPlayerManager().getPlayerList()) {
                    p.sendMessage(Text.literal(otherPlayer.getName().getString() + " погиб от: " + ruleName).formatted(Formatting.RED));
                }
                otherPlayer.damage(otherPlayer.getDamageSources().generic(), Float.MAX_VALUE);
            }
        }
        
        return true; // Cancel original damage
    }
}
