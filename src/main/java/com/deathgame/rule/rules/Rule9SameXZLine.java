package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.*;

/**
 * Rule 9: Can't be at the same X/Z coordinates (standing in a line)
 * If all 3 players are in the same world and on the same X or Z coordinate
 * (block precision), all 3 die.
 * This rule only triggers when there are exactly 3 participants.
 */
public class Rule9SameXZLine extends AbstractRule {
    
    // Cooldown to prevent multiple triggers (20 ticks = 1 second)
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    public Rule9SameXZLine() {
        super(9, "Нельзя стоять втроём на одной линии X или Z");
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
            return;
        }
        
        // Filter to only valid targets
        players = players.stream().filter(this::isValidTarget).toList();
        if (players.size() != 3) {
            return;
        }
        
        // Check same dimension
        ServerWorld world = players.get(0).getServerWorld();
        for (ServerPlayerEntity player : players) {
            if (!player.getServerWorld().equals(world)) {
                return;
            }
        }
        
        // Get block positions
        int x1 = players.get(0).getBlockPos().getX();
        int x2 = players.get(1).getBlockPos().getX();
        int x3 = players.get(2).getBlockPos().getX();
        
        int z1 = players.get(0).getBlockPos().getZ();
        int z2 = players.get(1).getBlockPos().getZ();
        int z3 = players.get(2).getBlockPos().getZ();
        
        // Check if all on same X or all on same Z
        boolean sameX = (x1 == x2) && (x2 == x3);
        boolean sameZ = (z1 == z2) && (z2 == z3);
        
        if (sameX || sameZ) {
            lastTriggerTime = currentTime;
            // Kill all players
            for (ServerPlayerEntity player : players) {
                killPlayer(player);
            }
        }
    }
    
    @Override
    public void reset() {
        lastTriggerTime = 0;
    }
}
