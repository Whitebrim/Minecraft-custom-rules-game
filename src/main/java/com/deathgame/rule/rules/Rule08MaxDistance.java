package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

/**
 * Rule 8: Can't be more than 50 blocks away from any other player
 * If a player is more than 50 blocks away from ALL other participants, they die.
 */
public class Rule08MaxDistance extends AbstractRule {
    
    private static final double MAX_DISTANCE = 50.0;
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    public Rule08MaxDistance() {
        super(8, "Нельзя отходить дальше 50 блоков от любого другого игрока");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        long currentTime = server.getOverworld().getTime();
        if (currentTime - lastTriggerTime < COOLDOWN_TICKS) {
            return;
        }
        
        List<ServerPlayerEntity> players = gameManager.getOnlineParticipants()
            .stream()
            .filter(this::isValidTarget)
            .toList();
        
        // Need at least 2 players for distance check
        if (players.size() < 2) return;
        
        Set<ServerPlayerEntity> toKill = new HashSet<>();
        
        for (ServerPlayerEntity player : players) {
            boolean hasSomeoneClose = false;
            
            for (ServerPlayerEntity other : players) {
                if (other.equals(player)) continue;
                
                // Must be in same dimension
                if (!player.getWorld().equals(other.getWorld())) continue;
                
                double distance = player.getPos().distanceTo(other.getPos());
                if (distance <= MAX_DISTANCE) {
                    hasSomeoneClose = true;
                    break;
                }
            }
            
            if (!hasSomeoneClose) {
                DeathGameMod.LOGGER.info("[Rule8] Player {} is too far from all other players", 
                    player.getName().getString());
                toKill.add(player);
            }
        }
        
        if (!toKill.isEmpty()) {
            lastTriggerTime = currentTime;
            for (ServerPlayerEntity player : toKill) {
                killPlayer(player);
            }
        }
    }
    
    @Override
    public void reset() {
        lastTriggerTime = 0;
    }
}
