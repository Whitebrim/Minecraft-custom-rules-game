package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Rule 20: Can't jump more than 10 times per life
 * After 10 jumps, the player dies. Counter resets on respawn.
 */
public class Rule20JumpLimit extends AbstractRule {
    
    private static final int MAX_JUMPS = 9;
    
    // Track jump count per player
    private final Map<UUID, Integer> jumpCounts = new HashMap<>();
    
    // Track if player was on ground last tick (for jump detection)
    private final Map<UUID, Boolean> wasOnGround = new HashMap<>();
    
    public Rule20JumpLimit() {
        super(20, "Нельзя прыгать больше 10 раз за жизнь");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        for (ServerPlayerEntity player : gameManager.getOnlineParticipants()) {
            if (!isValidTarget(player)) continue;
            
            UUID uuid = player.getUuid();
            boolean wasGrounded = wasOnGround.getOrDefault(uuid, true);
            boolean isGrounded = player.isOnGround();
            boolean hasUpwardVelocity = player.getVelocity().y > 0.1;
            
            // Detect jump: was on ground, now not on ground, with upward velocity
            if (wasGrounded && !isGrounded && hasUpwardVelocity) {
                int jumps = jumpCounts.getOrDefault(uuid, 0) + 1;
                jumpCounts.put(uuid, jumps);
                
//                DeathGameMod.LOGGER.info("[Rule20] Player {} jumped ({}/{})",
//                    player.getName().getString(), jumps, MAX_JUMPS);
                
                if (jumps > MAX_JUMPS) {
                    DeathGameMod.LOGGER.info("[Rule20] Player {} exceeded jump limit", 
                        player.getName().getString());
                    killPlayer(player);
                    // Reset after death
                    jumpCounts.remove(uuid);
                }
            }
            
            // Update ground tracking
            wasOnGround.put(uuid, isGrounded);
        }
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        jumpCounts.remove(uuid);
        wasOnGround.remove(uuid);
        DeathGameMod.LOGGER.info("[Rule20] Reset jump counter for {} on respawn", player.getName().getString());
    }
    
    @Override
    public void reset() {
        jumpCounts.clear();
        wasOnGround.clear();
    }
}
