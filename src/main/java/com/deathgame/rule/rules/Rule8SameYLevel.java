package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

/**
 * Rule 8: If all 3 are on the same Y level for 5 seconds, all die
 * If all participants are in the same dimension and at exactly the same Y coordinate
 * (with precision to 0.001) for 5 consecutive seconds, everyone dies.
 * This rule only triggers when there are exactly 3 participants.
 */
public class Rule8SameYLevel extends AbstractRule {
    
    private static final int THRESHOLD_TICKS = 5 * 20; // 5 seconds in ticks
    private static final double Y_PRECISION = 0.001;
    
    // Cooldown to prevent multiple triggers (20 ticks = 1 second)
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    private int ticksAtSameY = 0;
    
    public Rule8SameYLevel() {
        super(8, "Нельзя находиться на одном Y уровне втроём более 5 секунд");
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
        
        // Need exactly 3 online participants for this rule
        if (players.size() != 3) {
            ticksAtSameY = 0;
            return;
        }
        
        // Filter to only valid targets
        players = players.stream().filter(this::isValidTarget).toList();
        if (players.size() != 3) {
            ticksAtSameY = 0;
            return;
        }
        
        // Check same dimension
        ServerWorld world = players.get(0).getServerWorld();
        for (ServerPlayerEntity player : players) {
            if (!player.getServerWorld().equals(world)) {
                ticksAtSameY = 0;
                return;
            }
        }
        
        // Check if all at same Y
        double y1 = players.get(0).getY();
        double y2 = players.get(1).getY();
        double y3 = players.get(2).getY();
        
        boolean sameY = Math.abs(y1 - y2) < Y_PRECISION && 
                        Math.abs(y2 - y3) < Y_PRECISION && 
                        Math.abs(y1 - y3) < Y_PRECISION;
        
        if (sameY) {
            ticksAtSameY++;
            
            if (ticksAtSameY >= THRESHOLD_TICKS) {
                lastTriggerTime = currentTime;
                // Kill all players
                for (ServerPlayerEntity player : players) {
                    killPlayer(player);
                }
                ticksAtSameY = 0;
            }
        } else {
            ticksAtSameY = 0;
        }
    }
    
    @Override
    public void reset() {
        ticksAtSameY = 0;
        lastTriggerTime = 0;
    }
}
