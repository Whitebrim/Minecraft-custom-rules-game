package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Rule 1: Can't shift at the same time (any 2 players)
 * If any 2 participants are crouching (sneaking) at the same game tick, both die.
 */
public class Rule1SimultaneousJump extends AbstractRule {
    
    // Cooldown to prevent multiple triggers (20 ticks = 1 second)
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    public Rule1SimultaneousJump() {
        super(1, "Нельзя двоим одновременно быть на шифте");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        // Check cooldown
        long currentTime = server.getOverworld().getTime();
        if (currentTime - lastTriggerTime < COOLDOWN_TICKS) {
            return;
        }
        
        List<ServerPlayerEntity> players = gameManager.getOnlineParticipants();
        
        // Need at least 2 players
        if (players.size() < 2) {
            return;
        }
        
        // Filter to only valid targets (alive, in survival, etc.)
        List<ServerPlayerEntity> validPlayers = players.stream()
            .filter(this::isValidTarget)
            .toList();
        
        if (validPlayers.size() < 2) {
            return;
        }
        
        // Find all crouching players
        List<ServerPlayerEntity> crouchingPlayers = validPlayers.stream()
            .filter(ServerPlayerEntity::isSneaking)
            .toList();
        
        // If 2 or more are crouching, kill all crouching players
        if (crouchingPlayers.size() >= 2) {
            lastTriggerTime = currentTime;
            for (ServerPlayerEntity player : crouchingPlayers) {
                killPlayer(player);
            }
        }
    }
    
    @Override
    public void reset() {
        lastTriggerTime = 0;
    }
}
