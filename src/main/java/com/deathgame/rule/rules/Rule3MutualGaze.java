package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.projectile.ProjectileUtil;

import java.util.*;

/**
 * Rule 3: Can't look at each other at the same time
 * If player A looks at player B, and player B looks back at player A, both die.
 * Uses precise raycast for detection.
 * Players must be at least 1 block apart for the rule to trigger.
 */
public class Rule3MutualGaze extends AbstractRule {
    
    private static final double RAYCAST_DISTANCE = 64.0; // blocks
    private static final double MIN_DISTANCE = 1.0; // minimum distance for rule to trigger
    
    // Cooldown to prevent multiple triggers (20 ticks = 1 second)
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    public Rule3MutualGaze() {
        super(3, "Нельзя смотреть друг на друга одновременно");
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
        
        // Get only alive, valid participants
        List<ServerPlayerEntity> players = gameManager.getOnlineParticipants()
            .stream()
            .filter(this::isValidTarget) // This checks isDead(), isSpectator(), isCreative(), isParticipant(), isRuleEnabled()
            .toList();
        
        // Need at least 2 players
        if (players.size() < 2) return;
        
        // Check all pairs
        Set<ServerPlayerEntity> toKill = new HashSet<>();
        
        for (int i = 0; i < players.size(); i++) {
            for (int j = i + 1; j < players.size(); j++) {
                ServerPlayerEntity playerA = players.get(i);
                ServerPlayerEntity playerB = players.get(j);
                
                // Check if they're in the same world
                if (!playerA.getWorld().equals(playerB.getWorld())) continue;
                
                // Skip if either is already marked for death
                if (toKill.contains(playerA) || toKill.contains(playerB)) continue;
                
                // Check minimum distance - players must be at least 1 block apart
                double distance = playerA.getPos().distanceTo(playerB.getPos());
                if (distance < MIN_DISTANCE) continue;
                
                boolean aLooksAtB = isLookingAtWithRaycast(playerA, playerB);
                boolean bLooksAtA = isLookingAtWithRaycast(playerB, playerA);
                
                if (aLooksAtB && bLooksAtA) {
                    toKill.add(playerA);
                    toKill.add(playerB);
                }
            }
        }
        
        if (!toKill.isEmpty()) {
            lastTriggerTime = currentTime;
            for (ServerPlayerEntity player : toKill) {
                killPlayer(player);
            }
        }
    }
    
    /**
     * Uses raycast to check if viewer is looking directly at target.
     * More precise than bounding box intersection.
     */
    private boolean isLookingAtWithRaycast(ServerPlayerEntity viewer, ServerPlayerEntity target) {
        Vec3d eyePos = viewer.getEyePos();
        Vec3d lookVec = viewer.getRotationVec(1.0F);
        Vec3d endPos = eyePos.add(lookVec.multiply(RAYCAST_DISTANCE));
        
        // Use ProjectileUtil for precise entity raycast
        Box searchBox = viewer.getBoundingBox().stretch(lookVec.multiply(RAYCAST_DISTANCE)).expand(1.0);
        
        EntityHitResult entityHit = ProjectileUtil.raycast(
            viewer,
            eyePos,
            endPos,
            searchBox,
            entity -> entity instanceof ServerPlayerEntity && entity.equals(target),
            RAYCAST_DISTANCE * RAYCAST_DISTANCE
        );
        
        return entityHit != null && entityHit.getType() == HitResult.Type.ENTITY;
    }
    
    @Override
    public void reset() {
        lastTriggerTime = 0;
    }
}
