package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Rule 5: Can't have empty hotbar for 30 seconds
 * If all 9 hotbar slots are empty for 30 consecutive seconds, the player dies.
 * Timer resets on respawn and doesn't tick while player is dead.
 */
public class Rule5EmptyHotbar extends AbstractRule {
    
    private static final int EMPTY_THRESHOLD_TICKS = 30 * 20; // 30 seconds in ticks
    
    private final Map<UUID, Integer> emptyHotbarTicks = new HashMap<>();
    
    public Rule5EmptyHotbar() {
        super(5, "Нельзя иметь пустой хотбар более 30 секунд");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        for (UUID uuid : gameManager.getParticipants()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            
            // isValidTarget now includes isDead() check, so dead players are skipped
            if (player == null || !isValidTarget(player)) {
                // Don't remove timer here - player might just be dead temporarily
                // Timer will be reset on respawn via onPlayerRespawn()
                continue;
            }
            
            if (isHotbarEmpty(player)) {
                int ticks = emptyHotbarTicks.getOrDefault(uuid, 0) + 1;
                emptyHotbarTicks.put(uuid, ticks);
                
                if (ticks >= EMPTY_THRESHOLD_TICKS) {
                    killPlayer(player);
                    emptyHotbarTicks.remove(uuid);
                }
            } else {
                emptyHotbarTicks.remove(uuid);
            }
        }
    }
    
    private boolean isHotbarEmpty(ServerPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity player) {
        // Reset timer on respawn
        emptyHotbarTicks.remove(player.getUuid());
    }
    
    @Override
    public void reset() {
        emptyHotbarTicks.clear();
    }
}
