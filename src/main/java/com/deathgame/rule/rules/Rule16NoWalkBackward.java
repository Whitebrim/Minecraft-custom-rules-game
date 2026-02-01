package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Rule 16: Can't walk backward at all
 * If a player moves backward relative to their facing direction, they die.
 * Uses position delta instead of velocity for accurate server-side detection.
 */
public class Rule16NoWalkBackward extends AbstractRule {
    
    private static final double MOVEMENT_THRESHOLD = 0.03; // Minimum movement to detect
    private static final double BACKWARD_THRESHOLD = -0.5; // Dot product threshold for backward detection
    
    // Track previous positions for movement calculation
    private final Map<UUID, Vec3d> previousPositions = new HashMap<>();
    
    // Track consecutive backward ticks to avoid false positives from lag/knockback
    private final Map<UUID, Integer> backwardTicks = new HashMap<>();
    private static final int REQUIRED_BACKWARD_TICKS = 3;
    
    public Rule16NoWalkBackward() {
        super(16, "Нельзя идти назад");
    }
    
    @Override
    public void tick(MinecraftServer server) {
        GameManager gameManager = DeathGameMod.getInstance().getGameManager();
        if (!gameManager.isGameRunning()) return;
        
        for (ServerPlayerEntity player : gameManager.getOnlineParticipants()) {
            if (!isValidTarget(player)) continue;
            
            UUID uuid = player.getUuid();
            Vec3d currentPos = player.getPos();
            Vec3d prevPos = previousPositions.get(uuid);
            
            // Update position for next tick
            previousPositions.put(uuid, currentPos);
            
            // Skip first tick (no previous position)
            if (prevPos == null) {
                continue;
            }
            
            // Calculate actual movement delta
            double deltaX = currentPos.x - prevPos.x;
            double deltaZ = currentPos.z - prevPos.z;
            double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            
            // Skip if not moving enough horizontally
            if (horizontalDistance < MOVEMENT_THRESHOLD) {
                backwardTicks.remove(uuid);
                continue;
            }
            
            // Skip if player is taking damage (knockback)
            if (player.hurtTime > 0) {
                backwardTicks.remove(uuid);
                continue;
            }
            
            // Get player's facing direction (horizontal only, normalized)
            float yaw = player.getYaw();
            double lookX = -Math.sin(Math.toRadians(yaw));
            double lookZ = Math.cos(Math.toRadians(yaw));
            
            // Normalize movement direction
            double moveX = deltaX / horizontalDistance;
            double moveZ = deltaZ / horizontalDistance;
            
            // Dot product: positive = forward, negative = backward
            double dot = lookX * moveX + lookZ * moveZ;
            
            if (dot < BACKWARD_THRESHOLD) {
                int ticks = backwardTicks.getOrDefault(uuid, 0) + 1;
                backwardTicks.put(uuid, ticks);
                
                if (ticks >= REQUIRED_BACKWARD_TICKS) {
                    DeathGameMod.LOGGER.info("[Rule16] Player {} walked backward (dot={}, move=[{},{}], look=[{},{}])", 
                        player.getName().getString(), 
                        String.format("%.2f", dot),
                        String.format("%.3f", deltaX),
                        String.format("%.3f", deltaZ),
                        String.format("%.2f", lookX),
                        String.format("%.2f", lookZ));
                    killPlayer(player);
                    backwardTicks.remove(uuid);
                    previousPositions.remove(uuid);
                }
            } else {
                backwardTicks.remove(uuid);
            }
        }
    }
    
    @Override
    public void onPlayerRespawn(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        backwardTicks.remove(uuid);
        previousPositions.remove(uuid);
    }
    
    @Override
    public void reset() {
        backwardTicks.clear();
        previousPositions.clear();
    }
}
