package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Rule 7: Can't overflow hunger bars
 * If eating food would cause food points to exceed 20, the player dies.
 * Exactly 20 is allowed (e.g., 15 + 5 = 20 is fine, 18 + 4 = 22 is death)
 */
public class Rule7HungerOverflow extends AbstractRule {
    
    // Thread-local flag to communicate between HungerManager.eat and LivingEntity.eatFood
    private static final ThreadLocal<Boolean> pendingKill = ThreadLocal.withInitial(() -> false);
    
    public Rule7HungerOverflow() {
        super(7, "Нельзя переполнять шкалу голода");
    }
    
    public static void setPendingKill(boolean value) {
        pendingKill.set(value);
    }
    
    public static boolean isPendingKill() {
        return pendingKill.get();
    }
    
    public static void clearPendingKill() {
        pendingKill.set(false);
    }
    
    /**
     * Check if player should be killed for hunger overflow
     */
    public boolean shouldKillPlayer(ServerPlayerEntity player) {
        return isValidTarget(player);
    }
    
    /**
     * Actually kill the player after food is consumed
     */
    public void killPlayerForViolation(ServerPlayerEntity player) {
        if (!isValidTarget(player)) return;
        killPlayer(player);
    }
}
