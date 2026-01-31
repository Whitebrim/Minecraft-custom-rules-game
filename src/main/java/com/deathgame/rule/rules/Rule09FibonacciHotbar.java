package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Rule 9: Hotbar item counts must not exceed Fibonacci numbers
 * Slot 1 = max 0 items, slot 2 = max 1, slot 3 = max 1, slot 4 = max 2,
 * slot 5 = max 3, slot 6 = max 5, slot 7 = max 8, slot 8 = max 13, slot 9 = max 34
 * Fibonacci sequence: 1, 1, 2, 3, 5, 8, 13, 21, 34
 */
public class Rule09FibonacciHotbar extends AbstractRule {
    
    // Fibonacci limits for each hotbar slot (0-indexed internally, but 1-indexed for players)
    // Slot 1 (index 0) = 0, Slot 2 (index 1) = 1, etc.
    private static final int[] FIBONACCI_LIMITS = {1, 1, 2, 3, 5, 8, 13, 21, 34};
    
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    public Rule09FibonacciHotbar() {
        super(9, "Нельзя иметь в хотбаре больше предметов чем числа Фибоначчи (слот 1=1, слот 9=34)");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        long currentTime = server.getOverworld().getTime();
        if (currentTime - lastTriggerTime < COOLDOWN_TICKS) {
            return;
        }
        
        for (ServerPlayerEntity player : gameManager.getOnlineParticipants()) {
            if (!isValidTarget(player)) continue;
            
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = player.getInventory().getStack(slot);
                int count = stack.isEmpty() ? 0 : stack.getCount();
                int maxAllowed = FIBONACCI_LIMITS[slot];
                
                if (count > maxAllowed) {
                    DeathGameMod.LOGGER.info("[Rule9] Player {} has {} items in slot {} (max: {})", 
                        player.getName().getString(), count, slot + 1, maxAllowed);
                    lastTriggerTime = currentTime;
                    killPlayer(player);
                    break;
                }
            }
        }
    }
    
    @Override
    public void reset() {
        lastTriggerTime = 0;
    }
}
