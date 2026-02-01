package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Rule 18: Can't have more experience levels than occupied inventory slots
 * If player's XP level > number of non-empty slots in main inventory (36 slots), they die.
 */
public class Rule18XpSlotsLimit extends AbstractRule {
    
    // Main inventory size (hotbar 9 + main 27 = 36 slots, excluding armor and offhand)
    private static final int MAIN_INVENTORY_SIZE = 36;
    
    public Rule18XpSlotsLimit() {
        super(18, "Нельзя иметь больше уровней опыта чем занятых слотов в инвентаре");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        for (ServerPlayerEntity player : gameManager.getOnlineParticipants()) {
            if (!isValidTarget(player)) continue;
            
            int xpLevel = player.experienceLevel;
            int occupiedSlots = countOccupiedSlots(player);
            
            if (xpLevel > occupiedSlots) {
                DeathGameMod.LOGGER.info("[Rule18] Player {} has {} XP levels but only {} occupied slots", 
                    player.getName().getString(), xpLevel, occupiedSlots);
                killPlayer(player);
            }
        }
    }
    
    /**
     * Count non-empty slots in player's main inventory (hotbar + main inventory, excluding armor/offhand)
     */
    private int countOccupiedSlots(ServerPlayerEntity player) {
        int count = 0;
        
        // Main inventory slots (0-35): includes hotbar (0-8) and main inventory (9-35)
        for (int i = 0; i < MAIN_INVENTORY_SIZE; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                count++;
            }
        }
        
        return count;
    }
    
    @Override
    public void reset() {
        // No state to reset
    }
}
