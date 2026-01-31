package com.deathgame.rule.rules;

import com.deathgame.DeathGameMod;
import com.deathgame.game.GameManager;
import com.deathgame.rule.AbstractRule;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

/**
 * Rule 7: Love Triangle
 * If player A looks at player B, player B looks at player C, and player C looks at player A,
 * all three die. Works for any combination of 3 players.
 */
public class Rule07LoveTriangle extends AbstractRule {
    
    private static final double RAYCAST_DISTANCE = 64.0;
    private static final long COOLDOWN_TICKS = 20;
    private long lastTriggerTime = 0;
    
    public Rule07LoveTriangle() {
        super(7, "Любовный треугольник");
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
        
        if (players.size() < 3) return;
        
        // Build a map of who is looking at whom
        Map<ServerPlayerEntity, ServerPlayerEntity> lookingAt = new HashMap<>();
        
        for (ServerPlayerEntity viewer : players) {
            ServerPlayerEntity target = getPlayerBeingLookedAt(viewer, players);
            if (target != null) {
                lookingAt.put(viewer, target);
            }
        }
        
        // Find triangles: A -> B -> C -> A
        Set<ServerPlayerEntity> toKill = new HashSet<>();
        
        for (ServerPlayerEntity a : players) {
            ServerPlayerEntity b = lookingAt.get(a);
            if (b == null) continue;
            
            ServerPlayerEntity c = lookingAt.get(b);
            if (c == null) continue;
            
            ServerPlayerEntity backToA = lookingAt.get(c);
            if (backToA != null && backToA.equals(a)) {
                // Found a triangle!
                DeathGameMod.LOGGER.info("[Rule7] Love triangle detected: {} -> {} -> {} -> {}", 
                    a.getName().getString(),
                    b.getName().getString(),
                    c.getName().getString(),
                    a.getName().getString());
                toKill.add(a);
                toKill.add(b);
                toKill.add(c);
            }
        }
        
        if (!toKill.isEmpty()) {
            lastTriggerTime = currentTime;
            for (ServerPlayerEntity player : toKill) {
                killPlayer(player);
            }
        }
    }
    
    private ServerPlayerEntity getPlayerBeingLookedAt(ServerPlayerEntity viewer, List<ServerPlayerEntity> potentialTargets) {
        Vec3d eyePos = viewer.getEyePos();
        Vec3d lookVec = viewer.getRotationVec(1.0F);
        Vec3d endPos = eyePos.add(lookVec.multiply(RAYCAST_DISTANCE));
        
        Box searchBox = viewer.getBoundingBox().stretch(lookVec.multiply(RAYCAST_DISTANCE)).expand(1.0);
        
        EntityHitResult entityHit = ProjectileUtil.raycast(
            viewer,
            eyePos,
            endPos,
            searchBox,
            entity -> entity instanceof ServerPlayerEntity && 
                      potentialTargets.contains(entity) && 
                      !entity.equals(viewer),
            RAYCAST_DISTANCE * RAYCAST_DISTANCE
        );
        
        if (entityHit != null && entityHit.getType() == HitResult.Type.ENTITY) {
            return (ServerPlayerEntity) entityHit.getEntity();
        }
        
        return null;
    }
    
    @Override
    public void reset() {
        lastTriggerTime = 0;
    }
}
